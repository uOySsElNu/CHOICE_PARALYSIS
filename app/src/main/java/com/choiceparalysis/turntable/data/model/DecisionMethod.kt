package com.choiceparalysis.turntable.data.model

import androidx.annotation.StringRes
import com.choiceparalysis.turntable.R

enum class DecisionMethod(@StringRes val displayNameRes: Int) {
    SPIN_WHEEL(R.string.method_spin_wheel),
    COIN_FLIP(R.string.method_coin_flip),
    DICE_ROLL(R.string.method_dice_roll),
    YES_NO(R.string.method_answer_book),
    RANDOM_PICK(R.string.method_random_pick),
    FINGER_ROULETTE(R.string.method_finger_roulette);

}
