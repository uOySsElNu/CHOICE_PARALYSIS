package com.choiceparalysis.turntable.data.model

enum class DecisionMethod(val displayName: String) {
    SPIN_WHEEL("转盘"),
    COIN_FLIP("抛硬币"),
    DICE_ROLL("掷骰子"),
    YES_NO("Yes/No"),
    RANDOM_PICK("随机选择"),
    FINGER_ROULETTE("指尖轮盘")
}
