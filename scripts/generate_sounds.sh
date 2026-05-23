#!/bin/bash
# Generate sound effects using SoX
# Requires: sox in PATH

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$SCRIPT_DIR/../app/src/main/res/raw"
mkdir -p "$OUT_DIR"

SOX="sox"
RATE=44100

echo "Generating sound effects with SoX..."

# ============================================================
# DICE ROLL: realistic dice on hard table
#
# Real dice characteristics:
# - 3-5 bounces, irregular timing (80-180ms gaps)
# - Each "clack" = noise burst, 2-5kHz bandpass (the click)
# + low-mid body (200-500Hz rumble from table)
# - Sharp 1ms attack, 30-60ms decay
# - Total ~1.2 seconds
# - Slight room reverb
# ============================================================
TMP=$(mktemp -d)

# --- Click layers (high-mid freq, the "clack" of plastic on hard surface) ---
# Click 1: loudest, ~50ms
$SOX -n -r $RATE -b 16 "$TMP/c1.wav" \
    synth 0.05 noise \
    highpass 800 \
    bandpass 3000 2500 \
    fade t 0.001 0 0.04 \
    gain -6

# Click 2: slightly softer
$SOX -n -r $RATE -b 16 "$TMP/c2.wav" \
    synth 0.045 noise \
    highpass 800 \
    bandpass 2800 2200 \
    fade t 0.001 0 0.035 \
    gain -10

# Click 3: medium
$SOX -n -r $RATE -b 16 "$TMP/c3.wav" \
    synth 0.04 noise \
    highpass 800 \
    bandpass 3200 2600 \
    fade t 0.001 0 0.03 \
    gain -14

# Click 4: soft
$SOX -n -r $RATE -b 16 "$TMP/c4.wav" \
    synth 0.035 noise \
    highpass 800 \
    bandpass 2500 2000 \
    fade t 0.001 0 0.025 \
    gain -18

# Click 5: very soft (settle)
$SOX -n -r $RATE -b 16 "$TMP/c5.wav" \
    synth 0.03 noise \
    highpass 800 \
    bandpass 2200 1800 \
    fade t 0.001 0 0.02 \
    gain -24

# --- Body layer (low-mid freq, table resonance) ---
# Generate a low rumble that underlies the whole event
$SOX -n -r $RATE -b 16 "$TMP/body.wav" \
    synth 1.2 noise \
    lowpass 600 \
    bandpass 300 250 \
    fade t 0.01 0 0.8 \
    gain -20

# --- Silence gaps (irregular timing) ---
$SOX -n -r $RATE -b 16 "$TMP/g1.wav" trim 0 0.12
$SOX -n -r $RATE -b 16 "$TMP/g2.wav" trim 0 0.09
$SOX -n -r $RATE -b 16 "$TMP/g3.wav" trim 0 0.07
$SOX -n -r $RATE -b 16 "$TMP/g4.wav" trim 0 0.05

# --- Concatenate clicks with gaps ---
$SOX "$TMP/c1.wav" "$TMP/g1.wav" \
     "$TMP/c2.wav" "$TMP/g2.wav" \
     "$TMP/c3.wav" "$TMP/g3.wav" \
     "$TMP/c4.wav" "$TMP/g4.wav" \
     "$TMP/c5.wav" \
     "$TMP/clicks.wav"

# Pad clicks to match body length
$SOX "$TMP/clicks.wav" "$TMP/clicks_padded.wav" pad 0 0.3 trim 0 1.2

# --- Mix clicks + body ---
$SOX -m "$TMP/clicks_padded.wav" "$TMP/body.wav" "$TMP/mixed.wav"

# --- Final processing: compression + room reverb ---
$SOX "$TMP/mixed.wav" "$OUT_DIR/dice_roll.wav" \
    compand 0.005,0.03 -60,-60,-30,-15,-20,-8 0 -90 0.02 \
    reverb 25 50 100 \
    gain -n \
    rate $RATE

rm -rf "$TMP"
echo "  dice_roll.wav: done"

# ============================================================
# WHEEL TICK
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/wheel_tick.wav" \
    synth 0.035 noise \
    bandpass 1800 1200 \
    fade t 0.001 0 0.03 \
    gain -n
echo "  wheel_tick.wav: done"

# ============================================================
# COIN SPIN
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/coin_spin.wav" \
    synth 0.07 noise \
    bandpass 3000 2000 \
    fade t 0.001 0 0.06 \
    gain -n
echo "  coin_spin.wav: done"

# ============================================================
# DICE BOUNCE (single hit)
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/dice_bounce.wav" \
    synth 0.05 noise \
    bandpass 2500 2000 \
    fade t 0.001 0 0.04 \
    gain -n
echo "  dice_bounce.wav: done"

# ============================================================
# SPIN DING
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/spin_ding.wav" \
    synth 0.8 sine 880 sine 2640 \
    fade 0 0 0.8 0.6 \
    gain -n
echo "  spin_ding.wav: done"

# ============================================================
# COIN CLINK
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/coin_clink.wav" \
    synth 0.12 noise \
    bandpass 3500 2500 \
    fade t 0.001 0 0.1 \
    gain -n
echo "  coin_clink.wav: done"

# ============================================================
# DICE TAP
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/dice_tap.wav" \
    synth 0.1 noise \
    bandpass 2500 2000 \
    fade t 0.001 0 0.08 \
    gain -n
echo "  dice_tap.wav: done"

# ============================================================
# YESNO CHIME
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/yesno_chime.wav" \
    synth 1.0 sine 523 sine 659 sine 784 \
    fade 0 0 1.0 0.7 \
    reverb 30 60 100 \
    gain -n
echo "  yesno_chime.wav: done"

# ============================================================
# ELIMINATION DRUM
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/elimination_drum.wav" \
    synth 0.4 sine 80 noise \
    bandpass 120 80 \
    fade 0 0 0.4 0.25 \
    gain -n
echo "  elimination_drum.wav: done"

# ============================================================
# WINNER CHEER
# ============================================================
$SOX -n -r $RATE -b 16 "$OUT_DIR/winner_cheer.wav" \
    synth 1.2 sine 262 sine 330 sine 392 sine 523 noise \
    bandpass 1000 800 \
    fade 0 0.1 1.2 0.8 \
    reverb 40 70 100 \
    gain -n
echo "  winner_cheer.wav: done"

echo ""
echo "Done! All WAV files generated."
ls -lh "$OUT_DIR"/*.wav
