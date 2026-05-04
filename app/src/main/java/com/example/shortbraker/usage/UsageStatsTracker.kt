package com.example.shortbraker.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

class UsageStatsTracker(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getTodayYoutubeUsageMinutes(): Int {
        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return 0

        val totalMs = stats
            .filter { it.packageName == YOUTUBE_PACKAGE }
            .sumOf { it.totalTimeInForeground }

        return (totalMs / 60_000L).toInt()
    }

    companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}
