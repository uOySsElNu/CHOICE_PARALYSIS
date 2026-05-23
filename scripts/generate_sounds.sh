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
# DICE ROLL: 10 rapid impacts of plastic dice on wood table
# Strategy: generate each hit at different amplitude, concatenate with silence gaps
# ============================================================
TMP=$(mktemp -d)

# Generate individual hits at different volumes using synth + bandpass
# Hit 1 (loudest)
$SOX -n -r $RATE -b 16 "$TMP/h1.wav" synth 0.04 noise bandpass -c 350 300 fade 0 0 0.04 0.015 gain -3
# Hit 2
$SOX -n -r $RATE -b 16 "$TMP/h2.wav" synth 0.04 noise bandpass -c 320 280 fade 0 0 0.04 0.015 gain -5
# Hit 3
$SOX -n -r $RATE -b 16 "$TMP/h3.wav" synth 0.04 noise bandpass -c 380 300 fade 0 0 0.04 0.015 gain -7
# Hit 4
$SOX -n -r $RATE -b 16 "$TMP/h4.wav" synth 0.04 noise bandpass -c 300 250 fade 0 0 0.04 0.015 gain -9
# Hit 5
$SOX -n -r $RATE -b 16 "$TMP/h5.wav" synth 0.04 noise bandpass -c 350 300 fade 0 0 0.04 0.015 gain -11
# Hit 6
$SOX -n -r $RATE -b 16 "$TMP/h6.wav" synth 0.04 noise bandpass -c 330 280 fade 0 0 0.04 0.015 gain -13
# Hit 7
$SOX -n -r $RATE -b 16 "$TMP/h7.wav" synth 0.04 noise bandpass -c 310 260 fade 0 0 0.04 0.015 gain -16
# Hit 8
$SOX -n -r $RATE -b 16 "$TMP/h8.wav" synth 0.04 noise bandpass -c 340 290 fade 0 0 0.04 0.015 gain -19
# Hit 9
$SOX -n -r $RATE -b 16 "$TMP/h9.wav" synth 0.04 noise bandpass -c 360 310 fade 0 0 0.04 0.015 gain -23
# Hit 10 (softest)
$SOX -n -r $RATE -b 16 "$TMP/h10.wav" synth 0.04 noise bandpass -c 300 250 fade 0 0 0.04 0.015 gain -27

# Silence gaps
$SOX -n -r $RATE -b 16 "$TMP/s006.wav" trim 0 0.006
$SOX -n -r $RATE -b 16 "$TMP/s004.wav" trim 0 0.004
$SOX -n -r $RATE -b 16 "$TMP/s002.wav" trim 0 0.002

# Concatenate: hit-silence-hit-silence-... with decreasing gaps
$SOX "$TMP/h1.wav" "$TMP/s006.wav" \
     "$TMP/h2.wav" "$TMP/s006.wav" \
     "$TMP/h3.wav" "$TMP/s006.wav" \
     "$TMP/h4.wav" "$TMP/s006.wav" \
     "$TMP/h5.wav" "$TMP/s006.wav" \
     "$TMP/h6.wav" "$TMP/s004.wav" \
     "$TMP/h7.wav" "$TMP/s004.wav" \
     "$TMP/h8.wav" "$TMP/s002.wav" \
     "$TMP/h9.wav" "$TMP/s002.wav" \
     "$TMP/h10.wav" \
     "$TMP/concat.wav"

# Pad to 1 second, add compression and slight reverb for room feel
$SOX "$TMP/concat.wav" "$OUT_DIR/dice_roll.wav" \
    pad 0 0.3 \
    compand 0.005,0.03 -60,-60,-25,-12,-15,-8 0 -90 0.02 \
    reverb 10 30 80 \
    gain -n \
    rate $RATE

rm -rf "$TMP"
echo "  dice_roll.wav: done"

# ============================================================
# WHEEL TICK: short mechanical click
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/wheel_tick.wav" \
    synth 0.035 noise \
    bandpass -c 1800 1200 \
    compand 0.001,0.01 -60,-60,-20,-10 0 -90 \
    fade 0 0 0.035 0.015 \
    gain -n
echo "  wheel_tick.wav: done"

# ============================================================
# COIN SPIN: metallic whoosh per half rotation
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/coin_spin.wav" \
    synth 0.07 noise \
    bandpass -c 3000 2000 \
    compand 0.001,0.02 -60,-60,-25,-12 0 -90 \
    fade 0 0 0.07 0.025 \
    gain -n
echo "  coin_spin.wav: done"

# ============================================================
# DICE BOUNCE: single quick bounce impact
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/dice_bounce.wav" \
    synth 0.05 noise \
    bandpass -c 400 350 \
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
    bandpass -c 3500 2500 \
    compand 0.001,0.01 -60,-60,-20,-8 0 -90 \
    fade 0 0 0.12 0.05 \
    gain -n
echo "  coin_clink.wav: done"

# ============================================================
# DICE TAP: single wooden tap
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/dice_tap.wav" \
    synth 0.1 noise \
    bandpass -c 300 250 \
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
    bandpass -c 120 80 \
    compand 0.001,0.05 -60,-60,-30,-10 0 -90 \
    fade 0 0 0.4 0.25 \
    gain -n
echo "  elimination_drum.wav: done"

# ============================================================
# WINNER CHEER: celebration chord
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/winner_cheer.wav" \
    synth 1.2 sine 262 sine 330 sine 392 sine 523 noise \
    bandpass -c 1000 800 \
    fade 0 0.1 1.2 0.8 \
    reverb 40 70 100 \
    gain -n
echo "  winner_cheer.wav: done"

echo ""
echo "Done! All WAV files generated in $OUT_DIR"
ls -lh "$OUT_DIR"/*.wav
