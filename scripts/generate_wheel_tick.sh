#!/bin/bash
# Generate wheel_tick.wav based on real waveform analysis
# Characteristics: 0.68s, 3-4kHz click, fast decay over 150ms, then near-silence

SOX="/tmp/sox_extract/sox-14.4.2/sox"
RATE=44100
OUT="D:/Android/Projects/CHOICEPARALYSIS/app/src/main/res/raw/wheel_tick.wav"

# Layer 1: Sharp noise click at 3-4kHz, fast decay
$SOX -n -r $RATE -b 16 /tmp/tick_l1.wav \
    synth 0.15 noise bandpass 3500 2000 \
    fade t 0.001 0 0.12 \
    gain +0

# Layer 2: Slightly lower freq body (2.5kHz), slightly longer
$SOX -n -r $RATE -b 16 /tmp/tick_l2.wav \
    synth 0.12 noise bandpass 2800 1500 \
    fade t 0.001 0 0.08 \
    gain -4

# Mix the two layers
$SOX -m /tmp/tick_l1.wav /tmp/tick_l2.wav /tmp/tick_mixed.wav

# Pad to 0.68s, add slight reverb, normalize
$SOX /tmp/tick_mixed.wav "$OUT" \
    pad 0 0.45 \
    reverb 5 20 50 \
    gain -n \
    rate $RATE

rm -f /tmp/tick_l1.wav /tmp/tick_l2.wav /tmp/tick_mixed.wav
echo "wheel_tick.wav generated"
