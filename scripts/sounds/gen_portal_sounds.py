import numpy as np
import wave
import os

SR = 44100

def fade(n, fade_in, fade_out):
    env = np.ones(n)
    fi = int(fade_in * SR)
    fo = int(fade_out * SR)
    if fi > 0:
        env[:fi] *= np.linspace(0, 1, fi)
    if fo > 0:
        env[-fo:] *= np.linspace(1, 0, fo)
    return env

def write_wav(path, samples, sr=SR):
    samples = np.clip(samples, -1.0, 1.0)
    pcm = (samples * 32767).astype(np.int16)
    with wave.open(path, "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(pcm.tobytes())

def slow_jitter(n, depth, rate_hz, seed, sr=SR):
    rng = np.random.default_rng(seed)
    t = np.linspace(0, n / sr, n, endpoint=False)
    out = np.zeros(n)
    for i in range(3):
        f = rate_hz * (0.6 + 0.8 * rng.random())
        phase = rng.random() * 2 * np.pi
        out += np.sin(2 * np.pi * f * t + phase)
    out /= 3.0
    return out * depth

def detuned_sweep(dur, f_start, f_end, seed, sr=SR, voices=3, detune_cents=9):
    n = int(dur * sr)
    t = np.linspace(0, dur, n, endpoint=False)
    out = np.zeros(n)
    for v in range(voices):
        cents = (v - (voices - 1) / 2) * detune_cents
        ratio = 2 ** (cents / 1200)
        jitter = slow_jitter(n, depth=0.006, rate_hz=3.0 + v, seed=seed * 10 + v, sr=sr)
        k = (f_end / f_start) ** (t / dur)
        freq = f_start * k * ratio * (1.0 + jitter)
        phase = 2 * np.pi * np.cumsum(freq) / sr
        out += np.tanh(1.4 * np.sin(phase))
    return out / voices

def filtered_noise(dur, cutoff_lo, cutoff_hi, sr=SR, seed=0):
    rng = np.random.default_rng(seed)
    n = int(dur * sr)
    noise = rng.standard_normal(n)
    spec = np.fft.rfft(noise)
    freqs = np.fft.rfftfreq(n, 1 / sr)
    mask = (freqs >= cutoff_lo) & (freqs <= cutoff_hi)
    spec = spec * mask
    filtered = np.fft.irfft(spec, n)
    return filtered / (np.max(np.abs(filtered)) + 1e-9)

def crackle_bursts(dur, n_bursts, sr=SR, seed=1):
    rng = np.random.default_rng(seed)
    n = int(dur * sr)
    out = np.zeros(n)
    for _ in range(n_bursts):
        pos = rng.integers(0, max(1, n - 200))
        burst_len = rng.integers(40, 180)
        burst_len = min(burst_len, n - pos)
        burst = rng.standard_normal(burst_len) * np.exp(-np.linspace(0, 8, burst_len))
        out[pos:pos + burst_len] += burst * rng.uniform(0.3, 0.8)
    return out / (np.max(np.abs(out)) + 1e-9)

def make_ir(dur, seed, early_taps=4, sr=SR):
    rng = np.random.default_rng(seed)
    n = int(dur * sr)
    ir = np.zeros(n)
    for i in range(early_taps):
        pos = int((0.005 + 0.02 * i) * sr)
        if pos < n:
            ir[pos] += (0.5 ** i) * rng.uniform(0.5, 1.0)
    tail = rng.standard_normal(n) * np.exp(-np.linspace(0, 7, n))
    spec = np.fft.rfft(tail)
    freqs = np.fft.rfftfreq(n, 1 / sr)
    spec = spec * (freqs < 6000)
    tail = np.fft.irfft(spec, n)
    ir += tail * 0.25
    ir[0] += 1.0
    return ir / (np.max(np.abs(ir)) + 1e-9)

def apply_reverb(signal, ir, wet=0.22):
    n = len(signal) + len(ir) - 1
    nfft = 1
    while nfft < n:
        nfft *= 2
    wet_sig = np.fft.irfft(np.fft.rfft(signal, nfft) * np.fft.rfft(ir, nfft), nfft)[:len(signal)]
    wet_sig = wet_sig / (np.max(np.abs(wet_sig)) + 1e-9) * (np.max(np.abs(signal)) + 1e-9)
    return signal * (1 - wet) + wet_sig * wet


def make_ignite():
    """Session follow-up (2026-08-26): dropped the synthesized chime
    layer entirely - the repo owner pointed out that a synthesized bell
    can't easily match a real, pitched musical texture, and suggested
    mixing in vanilla sound sources whenever actual musical/tonal content
    is needed. SoundEvents.AMETHYST_BLOCK_CHIME (played separately,
    layered by two playSound calls rather than baked into this file - see
    PrismiumPortalIgniteHandler) now carries that role. This file is just
    the atmospheric whoosh+shimmer bed underneath it."""
    dur = 1.15
    n = int(dur * SR)
    sweep = detuned_sweep(dur, 140, 780, seed=7) * fade(n, 0.05, 0.5)
    sweep *= np.linspace(0.2, 0.55, n)

    shimmer = filtered_noise(dur, 3500, 9000, seed=11)
    shimmer *= np.linspace(0.0, 0.24, n) * fade(n, 0.3, 0.4)

    mix = np.zeros(n)
    mix[:len(sweep)] += sweep
    mix[:len(shimmer)] += shimmer
    mix *= fade(n, 0.02, 0.15)

    ir = make_ir(0.35, seed=101)
    mix = apply_reverb(mix, ir, wet=0.20)

    mix = mix / (np.max(np.abs(mix)) + 1e-9) * 0.9
    return mix


def make_fizzle():
    """Same rationale as make_ignite() - SoundEvents.AMETHYST_BLOCK_RESONATE
    (played at a lower pitch, see PrismiumPortalFrameBreakHandler) now
    supplies the musical/tonal 'closing' texture; this file is the
    descending whoosh + glassy crackle bed underneath it."""
    dur = 0.9
    n = int(dur * SR)
    sweep = detuned_sweep(dur, 700, 120, seed=17) * fade(n, 0.02, 0.55)
    sweep *= np.linspace(0.5, 0.05, n)

    crackle = crackle_bursts(dur, 14, seed=22)
    crackle *= np.linspace(0.42, 0.15, n)

    mix = sweep + crackle
    mix *= fade(n, 0.005, 0.2)

    ir = make_ir(0.3, seed=202)
    mix = apply_reverb(mix, ir, wet=0.18)

    mix = mix / (np.max(np.abs(mix)) + 1e-9) * 0.85
    return mix


def analyze(name, samples):
    peak = float(np.max(np.abs(samples)))
    rms = float(np.sqrt(np.mean(samples ** 2)))
    dur = len(samples) / SR
    print(f"{name}: duration={dur:.3f}s peak={peak:.3f} rms={rms:.4f} nan={np.isnan(samples).any()}")


if __name__ == "__main__":
    os.makedirs("/tmp/portal_sounds_v3", exist_ok=True)
    ignite = make_ignite()
    fizzle = make_fizzle()
    analyze("ignite", ignite)
    analyze("fizzle", fizzle)
    write_wav("/tmp/portal_sounds_v3/ignite.wav", ignite)
    write_wav("/tmp/portal_sounds_v3/fizzle.wav", fizzle)
    print("WAV files written")
