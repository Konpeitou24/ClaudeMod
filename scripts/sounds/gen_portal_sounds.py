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

def detuned_sweep(dur, f_start, f_end, seed, sr=SR, voices=3, detune_cents=6):
    """Session follow-up (2026-08-26, second listen): the repo owner
    called the first pass 'goofy'/too loud. Root cause of the goofiness
    was almost certainly the tanh(1.4 * sin(...)) hard waveshaping - that
    much soft-clipping on a sine turns it almost square, which reads as
    buzzy/honky/kazoo-like, not epic. Waveshaping intensity cut drastically
    (1.4 -> 0.35, barely-there warmth instead of a nasal buzz) and detune
    spread narrowed (9 -> 6 cents) so the unison reads as 'thick' rather
    than 'wobbly/cartoonish'."""
    n = int(dur * sr)
    t = np.linspace(0, dur, n, endpoint=False)
    out = np.zeros(n)
    for v in range(voices):
        cents = (v - (voices - 1) / 2) * detune_cents
        ratio = 2 ** (cents / 1200)
        jitter = slow_jitter(n, depth=0.004, rate_hz=3.0 + v, seed=seed * 10 + v, sr=sr)
        k = (f_end / f_start) ** (t / dur)
        freq = f_start * k * ratio * (1.0 + jitter)
        phase = 2 * np.pi * np.cumsum(freq) / sr
        out += np.tanh(0.35 * np.sin(phase)) / np.tanh(0.35)
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

def apply_reverb(signal, ir, wet=0.15):
    n = len(signal) + len(ir) - 1
    nfft = 1
    while nfft < n:
        nfft *= 2
    wet_sig = np.fft.irfft(np.fft.rfft(signal, nfft) * np.fft.rfft(ir, nfft), nfft)[:len(signal)]
    wet_sig = wet_sig / (np.max(np.abs(wet_sig)) + 1e-9) * (np.max(np.abs(signal)) + 1e-9)
    return signal * (1 - wet) + wet_sig * wet


# Session follow-up (2026-08-26, second listen): "way too big/loud" and
# "goofy, doesn't fit the mod's mood" was the verdict on the previous
# pass. Besides the waveshaping fix above, the overall level target was
# cut roughly in half (0.9/0.85 peak -> 0.45/0.4) - a subtle background
# cue for opening/closing a gate should not be the loudest thing in the
# mix. Java-side playSound volume parameters were cut similarly (see
# PrismiumPortalIgniteHandler / PrismiumPortalFrameBreakHandler), and the
# vanilla layer was swapped from AMETHYST_BLOCK_CHIME (a bright, almost
# music-box-like twinkle - fine for a growing crystal, too playful/cute
# for a dimensional gate) to the deeper, more ambient AMETHYST_BLOCK_RESONATE
# for both ignite and fizzle (differentiated by pitch instead of by using
# two different vanilla events), matching this mod's exploration-focused,
# more serious tone.
IGNITE_PEAK = 0.42
FIZZLE_PEAK = 0.38


def make_ignite():
    dur = 1.1
    n = int(dur * SR)
    sweep = detuned_sweep(dur, 150, 620, seed=7) * fade(n, 0.05, 0.5)
    sweep *= np.linspace(0.18, 0.4, n)

    shimmer = filtered_noise(dur, 3500, 8500, seed=11)
    shimmer *= np.linspace(0.0, 0.14, n) * fade(n, 0.3, 0.4)

    mix = np.zeros(n)
    mix[:len(sweep)] += sweep
    mix[:len(shimmer)] += shimmer
    mix *= fade(n, 0.02, 0.2)

    ir = make_ir(0.3, seed=101)
    mix = apply_reverb(mix, ir, wet=0.15)

    mix = mix / (np.max(np.abs(mix)) + 1e-9) * IGNITE_PEAK
    return mix


def make_fizzle():
    dur = 0.85
    n = int(dur * SR)
    sweep = detuned_sweep(dur, 550, 130, seed=17) * fade(n, 0.02, 0.55)
    sweep *= np.linspace(0.35, 0.05, n)

    crackle = crackle_bursts(dur, 10, seed=22)
    crackle *= np.linspace(0.28, 0.1, n)

    mix = sweep + crackle
    mix *= fade(n, 0.005, 0.25)

    ir = make_ir(0.25, seed=202)
    mix = apply_reverb(mix, ir, wet=0.13)

    mix = mix / (np.max(np.abs(mix)) + 1e-9) * FIZZLE_PEAK
    return mix


def analyze(name, samples):
    peak = float(np.max(np.abs(samples)))
    rms = float(np.sqrt(np.mean(samples ** 2)))
    dur = len(samples) / SR
    print(f"{name}: duration={dur:.3f}s peak={peak:.3f} rms={rms:.4f} nan={np.isnan(samples).any()}")


if __name__ == "__main__":
    os.makedirs("/tmp/portal_sounds_v4", exist_ok=True)
    ignite = make_ignite()
    fizzle = make_fizzle()
    analyze("ignite", ignite)
    analyze("fizzle", fizzle)
    write_wav("/tmp/portal_sounds_v4/ignite.wav", ignite)
    write_wav("/tmp/portal_sounds_v4/fizzle.wav", fizzle)
    print("WAV files written")
