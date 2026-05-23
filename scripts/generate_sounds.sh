#!/bin/bash
# Generate sound effects using SoX (Sound eXchange)
# Requires: sox in PATH
# Usage: bash scripts/generate_sounds.sh

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$SCRIPT_DIR/../app/src/main/res/raw"
mkdir -p "$OUT_DIR"

SOX="sox"
RATE=44100

echo "Generating sound effects with SoX..."

# ============================================================
# DICE ROLL: realistic dice thrown on wood table
# Key: few bounces, long gaps, heavy thuds, ~1s total
# ============================================================
TMP=$(mktemp -d)

# Each hit: noise bandpassed to 150-400Hz (heavy thud body)
# + a sharp transient at 1kHz (hard surface click)
# Duration per hit: 0.08s (longer for more weight)

# Hit 1: initial impact (loudest, heaviest)
$SOX -n -r $RATE -b 16 "$TMP/h1.wav" \
    synth 0.08 noise bandpass 250 200 \
    compand 0.001,0.02 -60,-60,-20,-5 0 -90 \
    fade 0 0 0.08 0.03 \
    gain +2

# Hit 2: second bounce (slightly softer)
$SOX -n -r $RATE -b 16 "$TMP/h2.wav" \
    synth 0.07 noise bandpass 230 180 \
    compand 0.001,0.02 -60,-60,-20,-5 0 -90 \
    fade 0 0 0.07 0.025 \
    gain -2

# Hit 3: third bounce
$SOX -n -r $RATE -b 16 "$TMP/h3.wav" \
    synth 0.06 noise bandpass 260 200 \
    compand 0.001,0.02 -60,-60,-20,-5 0 -90 \
    fade 0 0 0.06 0.02 \
    gain -6

# Hit 4: fourth bounce (settling)
$SOX -n -r $RATE -b 16 "$TMP/h4.wav" \
    synth 0.05 noise bandpass 220 170 \
    compand 0.001,0.02 -60,-60,-20,-5 0 -90 \
    fade 0 0 0.05 0.015 \
    gain -12

# Hit 5: final tiny tap (dice settling)
$SOX -n -r $RATE -b 16 "$TMP/h5.wav" \
    synth 0.04 noise bandpass 200 150 \
    compand 0.001,0.01 -60,-60,-20,-5 0 -90 \
    fade 0 0 0.04 0.01 \
    gain -18

# Silence gaps between bounces (realistic timing)
# Gap 1: ~120ms (hand release to first table hit)
$SOX -n -r $RATE -b 16 "$TMP/g1.wav" trim 0 0.08
# Gap 2: ~100ms (first bounce)
$SOX -n -r $RATE -b 16 "$TMP/g2.wav" trim 0 0.06
# Gap 3: ~80ms (bounces getting faster)
$SOX -n -r $RATE -b 16 "$TMP/g3.wav" trim 0 0.05
# Gap 4: ~60ms (settling)
$SOX -n -r $RATE -b 16 "$TMP/g4.wav" trim 0 0.04

# Concatenate: hit-gap-hit-gap-...
$SOX "$TMP/h1.wav" "$TMP/g1.wav" \
     "$TMP/h2.wav" "$TMP/g2.wav" \
     "$TMP/h3.wav" "$TMP/g3.wav" \
     "$TMP/h4.wav" "$TMP/g4.wav" \
     "$TMP/h5.wav" \
     "$TMP/concat.wav"

# Pad to 1 second, add room reverb
$SOX "$TMP/concat.wav" "$OUT_DIR/dice_roll.wav" \
    pad 0 0.3 \
    compand 0.01,0.04 -60,-60,-25,-10,-15,-5 0 -90 0.03 \
    reverb 20 50 100 \
    gain -n \
    rate $RATE

rm -rf "$TMP"
echo "  dice_roll.wav: done"

# ============================================================
# WHEEL TICK: short mechanical click
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/wheel_tick.wav" \
    synth 0.035 noise \
    bandpass 1800 1200 \
    compand 0.001,0.01 -60,-60,-20,-10 0 -90 \
    fade 0 0 0.035 0.015 \
    gain -n
echo "  wheel_tick.wav: done"

# ============================================================
# COIN SPIN: metallic whoosh per half rotation
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/coin_spin.wav" \
    synth 0.07 noise \
    bandpass 3000 2000 \
    compand 0.001,0.02 -60,-60,-25,-12 0 -90 \
    fade 0 0 0.07 0.025 \
    gain -n
echo "  coin_spin.wav: done"

# ============================================================
# DICE BOUNCE: single quick bounce impact
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/dice_bounce.wav" \
    synth 0.05 noise \
    bandpass 400 350 \
    compand 0.001,0.01 -60,-60,-20,-10 0 -90 \
    fade 0 0 0.05 0.02 \
    gain -n
echo "  dice_bounce.wav: done"

# ============================================================
# SPIN DING: metallic bell ding
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/spin_ding.wav" \
    synth 0.8 sine 880 sine 2640 \
    fade 0 0 0.8 0.6 \
    gain -n
echo "  spin_ding.wav: done"

# ============================================================
# COIN CLINK: short metallic impact
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/coin_clink.wav" \
    synth 0.12 noise \
    bandpass 3500 2500 \
    compand 0.001,0.01 -60,-60,-20,-8 0 -90 \
    fade 0 0 0.12 0.05 \
    gain -n
echo "  coin_clink.wav: done"

# ============================================================
# DICE TAP: single wooden tap
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/dice_tap.wav" \
    synth 0.1 noise \
    bandpass 300 250 \
    compand 0.001,0.01 -60,-60,-20,-10 0 -90 \
    fade 0 0 0.1 0.04 \
    gain -n
echo "  dice_tap.wav: done"

# ============================================================
# YESNO CHIME: ascending mystery chord
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/yesno_chime.wav" \
    synth 1.0 sine 523 sine 659 sine 784 \
    fade 0 0 1.0 0.7 \
    reverb 30 60 100 \
    gain -n
echo "  yesno_chime.wav: done"

# ============================================================
# ELIMINATION DRUM: tense low boom
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/elimination_drum.wav" \
    synth 0.4 sine 80 noise \
    bandpass 120 80 \
    compand 0.001,0.05 -60,-60,-30,-10 0 -90 \
    fade 0 0 0.4 0.25 \
    gain -n
echo "  elimination_drum.wav: done"

# ============================================================
# WINNER CHEER: celebration chord
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/winner_cheer.wav" \
    synth 1.2 sine 262 sine 330 sine 392 sine 523 noise \
    bandpass 1000 800 \
    fade 0 0.1 1.2 0.8 \
    reverb 40 70 100 \
    gain -n
echo "  winner_cheer.wav: done"

echo ""
echo "Done! All WAV files generated in $OUT_DIR"
ls -lh "$OUT_DIR"/*.wav
