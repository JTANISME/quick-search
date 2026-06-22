package com.tk.quicksearch.widgetsPanel

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF

internal object WidgetPanelDebugLogger {
    private const val TAG = "WidgetPanel"

    fun logHostEvent(
        event: String,
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo?,
        detail: String? = null,
    ) {
        Log.d(TAG, buildString {
            append(event)
            append(" appWidgetId=")
            append(appWidgetId)
            append(' ')
            append(describeProvider(providerInfo))
            if (!detail.isNullOrBlank()) {
                append(" detail=")
                append(detail)
            }
        })
    }

    fun logOptions(
        event: String,
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo?,
        options: Bundle?,
    ) {
        logHostEvent(
            event = event,
            appWidgetId = appWidgetId,
            providerInfo = providerInfo,
            detail = describeOptions(options),
        )
    }

    fun logError(
        event: String,
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo?,
        throwable: Throwable,
    ) {
        Log.e(
            TAG,
            buildString {
                append(event)
                append(" appWidgetId=")
                append(appWidgetId)
                append(' ')
                append(describeProvider(providerInfo))
            },
            throwable,
        )
    }

    private fun describeProvider(providerInfo: AppWidgetProviderInfo?): String {
        if (providerInfo == null) return "provider=<null>"
        val label = providerInfo.label.orEmpty()
        val provider = providerInfo.provider.flattenToShortString()
        val configure = providerInfo.configure?.flattenToShortString() ?: "<none>"
        val previewLayout =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) providerInfo.previewLayout else 0
        val targetCellWidth =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) providerInfo.targetCellWidth else 0
        val targetCellHeight =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) providerInfo.targetCellHeight else 0
        return buildString {
            append("provider=")
            append(provider)
            append(" label=")
            append(label)
            append(" configure=")
            append(configure)
            append(" min=")
            append(providerInfo.minWidth)
            append('x')
            append(providerInfo.minHeight)
            append(" resizeMin=")
            append(providerInfo.minResizeWidth)
            append('x')
            append(providerInfo.minResizeHeight)
            append(" targetCells=")
            append(targetCellWidth)
            append('x')
            append(targetCellHeight)
            append(" previewLayout=")
            append(previewLayout)
        }
    }

    private fun describeOptions(options: Bundle?): String {
        if (options == null) return "options=<null>"
        val sizes =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                options.getParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    SizeF::class.java,
                )?.joinToString(prefix = "[", postfix = "]") { "${it.width}x${it.height}" }
            } else {
                null
            } ?: "[]"
        return buildString {
            append("options{min=")
            append(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -1))
            append('x')
            append(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1))
            append(", max=")
            append(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, -1))
            append('x')
            append(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, -1))
            append(", category=")
            append(options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1))
            append(", sizes=")
            append(sizes)
            append('}')
        }
    }
}
