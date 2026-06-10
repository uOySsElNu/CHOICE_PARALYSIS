package com.choiceparalysis.turntable.widget

import android.content.Context
import com.choiceparalysis.turntable.R

/**
 * Shared data sync for widgets.
 * - Provides the general answers pool (used by QuickDecisionWidgetProvider)
 * - Updates LastResultWidgetProvider when a new decision is made
 */
object WidgetDataSync {

    /** General answer pool (@StringRes), shared with AnswerBook in YesNoViewModel. */
    val generalAnswers = listOf(
        R.string.answer_general_1,
        R.string.answer_general_2,
        R.string.answer_general_3,
        R.string.answer_general_4,
        R.string.answer_general_5,
        R.string.answer_general_6,
        R.string.answer_general_7,
        R.string.answer_general_8,
        R.string.answer_general_9,
        R.string.answer_general_10,
    )

    /**
     * Update LastResultWidget with a new decision result.
     * Call this from ViewModels after recording history.
     */
    fun updateLastResultWidget(context: Context, result: String, method: String) {
        try {
            LastResultWidgetProvider.notifyNewResult(context, result, method)
        } catch (_: Exception) {
            // Widget update failure should not crash the app
        }
    }
}
