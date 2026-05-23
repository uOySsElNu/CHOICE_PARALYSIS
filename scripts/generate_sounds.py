#!/usr/bin/env python3
"""Generate WAV sound effects for Choice Paralysis app."""

import wave
import struct
import math
import random
import os

SAMPLE_RATE = 44100
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'raw')


def write_wav(filename, samples):
    """Write 16-bit mono WAV file."""
    path = os.path.join(OUTPUT_DIR, filename)
    with wave.open(path, 'w') as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SAMPLE_RATE)
        for s in samples:
            clamped = max(-1.0, min(1.0, s))
            f.writeframes(struct.pack('<h', int(clamped * 32767)))
    size = os.path.getsize(path)
    print(f"  {filename}: {size} bytes, {len(samples)/SAMPLE_RATE:.3f}s")


def sine(freq, t):
    return math.sin(2 * math.pi * freq * t)


def noise():
    return random.uniform(-1, 1)


def envelope(t, attack, decay, sustain_level, sustain_len, release):
    """ADSR envelope."""
    if t < attack:
        return t / attack
    t -= attack
    if t < decay:
        return 1.0 - (1.0 - sustain_level) * (t / decay)
    t -= decay
    if t < sustain_len:
        return sustain_level
    t -= sustain_len
    if t < release:
        return sustain_level * (1.0 - t / release)
    return 0.0


def generate_spin_ding():
    """Metallic ding sound - two harmonics with fast attack, long decay."""
    duration = 0.8
    samples = []
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        # Two harmonics (fundamental + 3rd)
        env = math.exp(-t * 6)  # exponential decay
        s = (sine(880, t) * 0.6 + sine(2640, t) * 0.3) * env
        # Add slight detuning for metallic feel
        s += sine(885, t) * 0.1 * env
        samples.append(s * 0.8)
    return samples


def generate_coin_clink():
    """Short metallic clink - high freq burst with quick decay."""
    duration = 0.15
    samples = []
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 30)
        # High frequency metallic sound
        s = (sine(4000, t) * 0.5 + sine(6000, t) * 0.3 + sine(2500, t) * 0.2) * env
        # Add noise for "clink" texture
        s += noise() * 0.15 * math.exp(-t * 50)
        samples.append(s * 0.9)
    return samples


def generate_dice_tap():
    """Wooden tap sound - low freq with noise burst."""
    duration = 0.12
    samples = []
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 40)
        # Low frequency thud
        s = sine(200, t) * 0.6 * env
        # Noise burst for impact texture
        s += noise() * 0.4 * math.exp(-t * 60)
        # Slight mid freq
        s += sine(500, t) * 0.2 * math.exp(-t * 35)
        samples.append(s * 0.85)
    return samples


def generate_yesno_chime():
    """Mysterious chime - ascending notes with reverb feel."""
    duration = 1.0
    samples = []
    # Three ascending notes
    freqs = [523, 659, 784]  # C5, E5, G5
    note_dur = 0.25
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        s = 0
        for j, freq in enumerate(freqs):
            note_start = j * 0.15
            if t >= note_start:
                nt = t - note_start
                env = math.exp(-nt * 3) * (1 if nt < note_dur else 0)
                s += sine(freq, nt) * 0.3 * env
                # Add shimmer
                s += sine(freq * 2, nt) * 0.1 * env
        # Global decay
        s *= math.exp(-t * 1.5)
        samples.append(s * 0.7)
    return samples


def generate_elimination_drum():
    """Tense drum hit - low boom with sharp attack."""
    duration = 0.4
    samples = []
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 8)
        # Low drum fundamental
        s = sine(80, t) * 0.7 * env
        # Attack noise burst
        if t < 0.02:
            s += noise() * 0.6 * (1 - t / 0.02)
        # Mid tone
        s += sine(200, t) * 0.3 * math.exp(-t * 12)
        samples.append(s * 0.9)
    return samples


def generate_winner_cheer():
    """Celebration sound - rising chord with shimmer."""
    duration = 1.2
    samples = []
    # Major chord: C4, E4, G4, C5
    freqs = [262, 330, 392, 523]
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        s = 0
        for freq in freqs:
            # Rising envelope
            rise_env = min(1.0, t / 0.3) * math.exp(-t * 1.2)
            s += sine(freq, t) * 0.2 * rise_env
        # Add sparkle (high freq)
        s += sine(2093, t) * 0.1 * math.exp(-t * 2) * min(1.0, t / 0.1)
        # Add noise for "applause" texture
        s += noise() * 0.08 * math.exp(-t * 1.5)
        samples.append(s * 0.75)
    return samples


def generate_wheel_tick():
    """Short mechanical tick - for wheel passing segment boundaries."""
    duration = 0.04
    samples = []
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 120)
        # Sharp click at ~2kHz
        s = sine(2000, t) * 0.5 * env
        # Add noise for mechanical texture
        s += noise() * 0.3 * math.exp(-t * 150)
        # Slight low freq body
        s += sine(400, t) * 0.2 * math.exp(-t * 100)
        samples.append(s * 0.7)
    return samples


def generate_coin_spin():
    """Metallic whoosh - for coin spinning in air."""
    duration = 0.08
    samples = []
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 35)
        # High freq whirring
        s = sine(3500, t) * 0.4 * env
        # Slightly detuned for shimmer
        s += sine(3550, t) * 0.3 * env
        # Noise for air texture
        s += noise() * 0.25 * math.exp(-t * 40)
        # Mid freq body
        s += sine(1500, t) * 0.15 * env
        samples.append(s * 0.7)
    return samples


def generate_dice_bounce():
    """Short bouncy tap - for dice hitting surface."""
    duration = 0.06
    samples = []
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 80)
        # Low thud
        s = sine(250, t) * 0.6 * env
        # Noise burst for impact
        s += noise() * 0.35 * math.exp(-t * 100)
        # Slight high click
        s += sine(1200, t) * 0.15 * math.exp(-t * 90)
        samples.append(s * 0.75)
    return samples


def generate_dice_roll():
    """Realistic dice roll on hard table - pure impact noise, no tonal elements."""
    duration = 1.0
    samples = []
    random.seed(42)  # reproducible
    # Bounce events: (time, amplitude)
    bounces = [
        (0.00, 1.0),
        (0.07, 0.9),
        (0.13, 0.75),
        (0.19, 0.6),
        (0.26, 0.5),
        (0.34, 0.4),
        (0.43, 0.3),
        (0.53, 0.22),
        (0.64, 0.15),
        (0.76, 0.1),
    ]
    for i in range(int(SAMPLE_RATE * duration)):
        t = i / SAMPLE_RATE
        s = 0.0
        for bt, amp in bounces:
            if t >= bt:
                dt = t - bt
                # Sharp noise burst = hard plastic/hitting wood table
                burst_env = math.exp(-dt * 60) * amp
                s += noise() * burst_env * 0.8
                # Filtered noise band for "clack" body (2k-5k)
                s += (sine(2500, dt) + sine(3800, dt) + sine(4500, dt)) * 0.05 * burst_env
                # Subtle low thud from table resonance
                s += noise() * math.exp(-dt * 20) * amp * 0.15
        # Sliding/rattling between bounces
        if t < 0.5:
            for bt2, amp2 in bounces:
                if t >= bt2 and t < bt2 + 0.04:
                    dt2 = t - bt2
                    s += noise() * math.exp(-dt2 * 30) * amp2 * 0.2
        samples.append(s * 0.85)
    return samples


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print("Generating sound effects...")

    write_wav('spin_ding.wav', generate_spin_ding())
    write_wav('coin_clink.wav', generate_coin_clink())
    write_wav('dice_tap.wav', generate_dice_tap())
    write_wav('yesno_chime.wav', generate_yesno_chime())
    write_wav('elimination_drum.wav', generate_elimination_drum())
    write_wav('winner_cheer.wav', generate_winner_cheer())
    write_wav('wheel_tick.wav', generate_wheel_tick())
    write_wav('coin_spin.wav', generate_coin_spin())
    write_wav('dice_bounce.wav', generate_dice_bounce())
    write_wav('dice_roll.wav', generate_dice_roll())

    print("\nDone! Replace .mp3 files with these .wav files.")
    print("Remember to update SoundEffect enum resId references if extension changes.")


if __name__ == '__main__':
    main()
