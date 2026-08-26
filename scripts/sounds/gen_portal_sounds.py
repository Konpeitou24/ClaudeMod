import numpy as np
import wave
import struct
import subprocess
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

def sine_sweep(dur, f_start, f_end, sr=SR):
    t = np.linspace(0, dur, int(dur * sr), endpoint=False)
    # exponential frequency glide (more natural sounding than linear for pitch sweeps)
    k = (f_end / f_start) ** (t / dur)
    freq = f_start * k
    phase = 2 * np.pi * np.cumsum(freq) / sr
    return np.sin(phase)

def chime_stack(dur, base_freq, n_partials, decay, sr=SR):
    t = np.linspace(0, dur, int(dur * sr), endpoint=False)
    out = np.zeros_like(t)
    for i in range(1, n_partials + 1):
        partial_freq = base_freq * i * (1.0 + 0.003 * i)  # slight inharmonicity, bell-like
        amp = (1.0 / i) * np.exp(-decay * i * t)
        out += amp * np.sin(2 * np.pi * partial_freq * t)
    return out / np.max(np.abs(out) + 1e-9)

def filtered_noise(dur, cutoff_lo, cutoff_hi, sr=SR, seed=0):
    rng = np.random.default_rng(seed)
    n = int(dur * sr)
    noise = rng.standard_normal(n)
    # crude bandpass via FFT masking - fine for short one-shot SFX
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


def make_ignite():
    dur = 1.1
    n = int(dur * SR)
    # 1) rising pitch sweep - the "portal opening" whoosh
    sweep = sine_sweep(dur, 140, 780) * fade(n, 0.05, 0.5)
    sweep *= np.linspace(0.15, 0.55, n)

    # 2) shimmering high-frequency noise swelling in alongside the sweep
    shimmer = filtered_noise(dur, 3500, 9000, seed=11)
    shimmer *= np.linspace(0.0, 0.22, n) * fade(n, 0.3, 0.4)

    # 3) crystalline chime stack near the end - the "gate now active" accent
    chime_dur = 0.75
    chime = chime_stack(chime_dur, 660, 6, 3.2)
    chime_env = fade(len(chime), 0.01, 0.6)
    chime *= chime_env * 0.35
    chime_start = int(0.35 * SR)
    chime_full = np.zeros(n)
    end = min(n, chime_start + len(chime))
    chime_full[chime_start:end] += chime[:end - chime_start]

    mix = np.zeros(n)
    mix[:len(sweep)] += sweep
    mix[:len(shimmer)] += shimmer
    mix += chime_full
    mix *= fade(n, 0.02, 0.15)
    mix = mix / (np.max(np.abs(mix)) + 1e-9) * 0.9
    return mix


def make_fizzle():
    dur = 0.85
    n = int(dur * SR)
    # 1) falling pitch sweep - the "portal collapsing" reverse whoosh
    sweep = sine_sweep(dur, 700, 120) * fade(n, 0.02, 0.55)
    sweep *= np.linspace(0.5, 0.05, n)

    # 2) glassy/crystal crackle bursts - matches this mod's crystal theme
    #    (SoundType.GLASS/AMETHYST is used across its Prismium blocks)
    crackle = crackle_bursts(dur, 14, seed=22)
    crackle *= np.linspace(0.4, 0.15, n)

    # 3) short descending chime "un-ringing" as the frame goes dark
    chime_dur = 0.5
    chime = chime_stack(chime_dur, 520, 5, 5.5)
    chime *= fade(len(chime), 0.005, 0.4) * 0.3
    chime_full = np.zeros(n)
    end = min(n, len(chime))
    chime_full[:end] += chime[:end]

    mix = sweep + crackle + chime_full
    mix *= fade(n, 0.005, 0.2)
    mix = mix / (np.max(np.abs(mix)) + 1e-9) * 0.85
    return mix


def analyze(path, samples):
    peak = float(np.max(np.abs(samples)))
    rms = float(np.sqrt(np.mean(samples ** 2)))
    dur = len(samples) / SR
    print(f"{path}: duration={dur:.3f}s peak={peak:.3f} rms={rms:.4f} nan={np.isnan(samples).any()}")


if __name__ == "__main__":
    os.makedirs("/tmp/portal_sounds", exist_ok=True)
    ignite = make_ignite()
    fizzle = make_fizzle()
    analyze("ignite", ignite)
    analyze("fizzle", fizzle)
    write_wav("/tmp/portal_sounds/ignite.wav", ignite)
    write_wav("/tmp/portal_sounds/fizzle.wav", fizzle)
    print("WAV files written")
