package com.choiceparalysis.turntable.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.choiceparalysis.turntable.MainActivity
import com.choiceparalysis.turntable.R

class QuickDecisionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Handle Xiaomi clear-data refresh
        if (intent.action == "miui.intent.action.MIUI_WIDGET_REFRESH_APP_DATA") {
            resetAllWidgets(context)
            return
        }

        // Handle tap to decide
        if (intent.action == ACTION_DECIDE) {
            decide(context)
        }
    }

    companion object {
        const val ACTION_DECIDE = "com.choiceparalysis.turntable.ACTION_DECIDE"
        const val PREFS_NAME = "quick_decision_widget"
        const val KEY_RESULT = "result"
        const val KEY_METHOD = "method"
        const val KEY_HAS_RESULT = "has_result"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hasResult = prefs.getBoolean(KEY_HAS_RESULT, false)
            val result = prefs.getString(KEY_RESULT, "")
            val method = prefs.getString(KEY_METHOD, "")

            val views = RemoteViews(context.packageName, R.layout.widget_quick_decision)

            if (hasResult && !result.isNullOrEmpty()) {
                views.setViewVisibility(R.id.state_initial, View.GONE)
                views.setViewVisibility(R.id.state_result, View.VISIBLE)
                views.setTextViewText(R.id.result_text, result)
                views.setTextViewText(R.id.result_method, method)
            } else {
                views.setViewVisibility(R.id.state_initial, View.VISIBLE)
                views.setViewVisibility(R.id.state_result, View.GONE)
            }

            // Click: decide action
            val decideIntent = Intent(context, QuickDecisionWidgetProvider::class.java).apply {
                action = ACTION_DECIDE
            }
            val decidePending = PendingIntent.getBroadcast(
                context, 0, decideIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_content, decidePending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun decide(context: Context) {
            val answerText = context.getString(WidgetDataSync.generalAnswers.random())
            val methodText = context.getString(R.string.method_answer_book)

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_RESULT, answerText)
                .putString(KEY_METHOD, methodText)
                .putBoolean(KEY_HAS_RESULT, true)
                .apply()

            refreshAll(context)
        }

        fun resetAllWidgets(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            refreshAll(context)
        }

        private fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, QuickDecisionWidgetProvider::class.java)
            )
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }
    }
}
