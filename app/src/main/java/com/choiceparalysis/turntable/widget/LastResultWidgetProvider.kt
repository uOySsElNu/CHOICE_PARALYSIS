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

class LastResultWidgetProvider : AppWidgetProvider() {

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
        }
    }

    companion object {
        const val PREFS_NAME = "last_result_widget"
        const val KEY_RESULT = "result"
        const val KEY_METHOD = "method"
        const val KEY_HAS_RESULT = "has_result"

        /** Call this from ViewModel after each decision to update the widget. */
        fun notifyNewResult(context: Context, result: String, method: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_RESULT, result)
                .putString(KEY_METHOD, method)
                .putBoolean(KEY_HAS_RESULT, true)
                .apply()
            refreshAll(context)
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hasResult = prefs.getBoolean(KEY_HAS_RESULT, false)
            val result = prefs.getString(KEY_RESULT, "")
            val method = prefs.getString(KEY_METHOD, "")

            val views = RemoteViews(context.packageName, R.layout.widget_last_result)

            if (hasResult && !result.isNullOrEmpty()) {
                views.setViewVisibility(R.id.state_empty, View.GONE)
                views.setViewVisibility(R.id.state_has_result, View.VISIBLE)
                views.setTextViewText(R.id.result_text, result)
                views.setTextViewText(R.id.result_method, method)
            } else {
                views.setViewVisibility(R.id.state_empty, View.VISIBLE)
                views.setViewVisibility(R.id.state_has_result, View.GONE)
            }

            // Click: open app
            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_content, openPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
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
                ComponentName(context, LastResultWidgetProvider::class.java)
            )
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }
    }
}
