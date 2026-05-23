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
# DICE ROLL: based on real waveform analysis
# - Duration: 1.1s
# - 4-5 sharp clicks at 2.5-3kHz
# - 1ms attack, 30-50ms decay per click
# - Gaps: 150-200ms between clicks
# - Last 0.5s is near-silence (dice settled)
# - Crest factor ~19 (sharp transients + quiet gaps)
# ============================================================
TMP=$(mktemp -d)

# Generate 5 sharp clicks with decreasing amplitude
# Each click: 50ms noise burst, bandpass 2500-3000Hz, 1ms attack

# Click 1: loudest (0.0s)
$SOX -n -r $RATE -b 16 "$TMP/c1.wav" \
    synth 0.05 noise bandpass 2800 1500 \
    fade t 0.001 0 0.04 \
    gain +0

# Click 2 (0.18s)
$SOX -n -r $RATE -b 16 "$TMP/c2.wav" \
    synth 0.045 noise bandpass 2700 1400 \
    fade t 0.001 0 0.035 \
    gain -6

# Click 3 (0.35s)
$SOX -n -r $RATE -b 16 "$TMP/c3.wav" \
    synth 0.04 noise bandpass 2900 1500 \
    fade t 0.001 0 0.03 \
    gain -12

# Click 4 (0.50s)
$SOX -n -r $RATE -b 16 "$TMP/c4.wav" \
    synth 0.035 noise bandpass 2600 1300 \
    fade t 0.001 0 0.025 \
    gain -18

# Click 5 (0.63s, very soft)
$SOX -n -r $RATE -b 16 "$TMP/c5.wav" \
    synth 0.03 noise bandpass 2500 1200 \
    fade t 0.001 0 0.02 \
    gain -26

# Silence gaps (realistic bounce timing)
$SOX -n -r $RATE -b 16 "$TMP/g1.wav" trim 0 0.13
$SOX -n -r $RATE -b 16 "$TMP/g2.wav" trim 0 0.12
$SOX -n -r $RATE -b 16 "$TMP/g3.wav" trim 0 0.10
$SOX -n -r $RATE -b 16 "$TMP/g4.wav" trim 0 0.08

# Concatenate: click-gap-click-gap-...
$SOX "$TMP/c1.wav" "$TMP/g1.wav" \
     "$TMP/c2.wav" "$TMP/g2.wav" \
     "$TMP/c3.wav" "$TMP/g3.wav" \
     "$TMP/c4.wav" "$TMP/g4.wav" \
     "$TMP/c5.wav" \
     "$TMP/concat.wav"

# Pad to 1.1s (long tail of silence = dice settled)
$SOX "$TMP/concat.wav" "$TMP/padded.wav" pad 0 0.4 trim 0 1.1

# Final: normalize, slight room reverb
$SOX "$TMP/padded.wav" "$OUT_DIR/dice_roll.wav" \
    reverb 15 40 80 \
    gain -n \
    rate $RATE

rm -rf "$TMP"
echo "  dice_roll.wav: done"

# ============================================================
# Other sounds (unchanged)
# ============================================================

$SOX -n -r $RATE -b 16 "$OUT_DIR/wheel_tick.wav" \
    synth 0.035 noise bandpass 1800 1200 \
    fade t 0.001 0 0.03 gain -n
echo "  wheel_tick.wav: done"

$SOX -n -r $RATE -b 16 "$OUT_DIR/coin_spin.wav" \
    synth 0.07 noise bandpass 3000 2000 \
    fade t 0.001 0 0.06 gain -n
echo "  coin_spin.wav: done"

$SOX -n -r $RATE -b 16 "$OUT_DIR/dice_bounce.wav" \
    synth 0.05 noise bandpass 2500 2000 \
    fade t 0.001 0 0.04 gain -n
echo "  dice_bounce.wav: done"

$SOX -n -r $RATE -b 16 "$OUT_DIR/spin_ding.wav" \
    synth 0.8 sine 880 sine 2640 \
    fade 0 0 0.8 0.6 gain -n
echo "  spin_ding.wav: done"

$SOX -n -r $RATE -b 16 "$OUT_DIR/coin_clink.wav" \
    synth 0.12 noise bandpass 3500 2500 \
    fade t 0.001 0 0.1 gain -n
echo "  coin_clink.wav: done"

$SOX -n -r $RATE -b 16 "$OUT_DIR/dice_tap.wav" \
    synth 0.1 noise bandpass 2500 2000 \
    fade t 0.001 0 0.08 gain -n
echo "  dice_tap.wav: done"

$SOX -n -r $RATE -b 16 "$OUT_DIR/yesno_chime.wav" \
    synth 1.0 sine 523 sine 659 sine 784 \
    fade 0 0 1.0 0.7 reverb 30 60 100 gain -n
echo "  yesno_chime.wav: done"

$SOX -n -r $RATE -b 16 "$OUT_DIR/elimination_drum.wav" \
    synth 0.4 sine 80 noise bandpass 120 80 \
    fade 0 0 0.4 0.25 gain -n
echo "  elimination_drum.wav: done"

$SOX -n -r $RATE -b 16 "$OUT_DIR/winner_cheer.wav" \
    synth 1.2 sine 262 sine 330 sine 392 sine 523 noise bandpass 1000 800 \
    fade 0 0.1 1.2 0.8 reverb 40 70 100 gain -n
echo "  winner_cheer.wav: done"

echo ""
echo "Done!"
ls -lh "$OUT_DIR"/*.wav
