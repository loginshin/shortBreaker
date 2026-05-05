package com.example.shortbraker.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.shortbraker.data.ShortsDebugRepository
import com.example.shortbraker.data.ShortsDebugState
import com.example.shortbraker.data.ShortsUsageRepository
import com.example.shortbraker.data.UserSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShortsAccessibilityService : AccessibilityService() {

    private val detector = YoutubeShortsDetector()
    private lateinit var usageRepository: ShortsUsageRepository
    private lateinit var debugRepository: ShortsDebugRepository
    private lateinit var settingsRepository: UserSettingsRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isShortsActive = false
    private var shortsSessionStartMs = 0L
    private var consecutiveScrollCount = 0
    private var lastAnalysisMs = 0L
    private var lastDebugWriteMs = 0L
    private var isKilling = false

    @Volatile private var dailyLimitMinutes = 30
    @Volatile private var todayShortsSeconds = 0L
    @Volatile private var blockMode = "NORMAL"
    @Volatile private var redirectAppPackage = ""
    @Volatile private var blockYoutubeApp = false

    override fun onCreate() {
        super.onCreate()
        usageRepository = ShortsUsageRepository(applicationContext)
        debugRepository = ShortsDebugRepository(applicationContext)
        settingsRepository = UserSettingsRepository(applicationContext)

        serviceScope.launch {
            settingsRepository.userSettings.collect { settings ->
                dailyLimitMinutes = settings.dailyLimitMinutes
                blockMode = settings.blockMode
                redirectAppPackage = settings.redirectAppPackage
                blockYoutubeApp = settings.blockYoutubeApp
            }
        }
        serviceScope.launch {
            usageRepository.todayUsage.collect { usage ->
                todayShortsSeconds = usage.usageSeconds
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return

        if (packageName != YoutubeShortsDetector.YOUTUBE_PACKAGE) {
            if (isShortsActive) endShortsSession()
            isKilling = false
            consecutiveScrollCount = 0
            return
        }

        if (isKilling) return

        // 유튜브 앱 전체 차단 모드
        if (blockYoutubeApp) {
            applyBlock()
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            consecutiveScrollCount++
            return
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        val now = System.currentTimeMillis()
        if (now - lastAnalysisMs < DEBOUNCE_MS) return
        lastAnalysisMs = now

        val className = event.className?.toString()
        analyzeCurrentScreen(event.eventType, className)
        consecutiveScrollCount = 0
    }

    private fun analyzeCurrentScreen(eventType: Int, windowClassName: String? = null) {
        val root = rootInActiveWindow ?: return
        val result = detector.detect(
            packageName = YoutubeShortsDetector.YOUTUBE_PACKAGE,
            rootNode = root,
            eventType = eventType,
            consecutiveScrollCount = consecutiveScrollCount,
            windowClassName = windowClassName
        )

        when {
            result.isShorts && !isShortsActive -> startShortsSession()
            !result.isShorts && isShortsActive -> endShortsSession()
        }

        if (result.isShorts) checkLimitAndBlock()

        val now = System.currentTimeMillis()
        if (now - lastDebugWriteMs >= DEBUG_WRITE_THROTTLE_MS) {
            lastDebugWriteMs = now
            val sessionStart = if (isShortsActive) shortsSessionStartMs else 0L
            serviceScope.launch {
                debugRepository.saveDebugState(
                    ShortsDebugState(
                        lastDetectedPackageName = YoutubeShortsDetector.YOUTUBE_PACKAGE,
                        lastEventType = eventTypeToString(eventType),
                        isCurrentlyShorts = result.isShorts,
                        lastConfidence = result.confidence,
                        lastDetectionReasons = result.reasons.joinToString(", "),
                        lastDetectionTimeMillis = now,
                        currentSessionStartedAt = sessionStart
                    )
                )
            }
        }
    }

    private fun checkLimitAndBlock() {
        val isZeroLimit = dailyLimitMinutes == 0
        if (!isZeroLimit) {
            val activeSessionSeconds = if (isShortsActive && shortsSessionStartMs > 0L)
                (System.currentTimeMillis() - shortsSessionStartMs) / 1000L else 0L
            if (todayShortsSeconds + activeSessionSeconds < dailyLimitMinutes * 60L) return
        }
        applyBlock()
    }

    private fun applyBlock() {
        if (isKilling) return
        isKilling = true
        endShortsSession()

        if (redirectAppPackage.isNotEmpty()) {
            launchRedirectApp()
        } else {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        // 홈/앱 전환 후 YouTube가 백그라운드로 가면 프로세스 종료
        serviceScope.launch {
            delay(KILL_DELAY_MS)
            killYoutube()
            delay(1_000)
            isKilling = false
        }
    }

    private fun killYoutube() {
        try {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(YoutubeShortsDetector.YOUTUBE_PACKAGE)
        } catch (_: Exception) {}
    }

    private fun launchRedirectApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(redirectAppPackage) ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun startShortsSession() {
        isShortsActive = true
        shortsSessionStartMs = System.currentTimeMillis()
        serviceScope.launch { usageRepository.incrementOpenCount() }
    }

    private fun endShortsSession() {
        if (!isShortsActive) return
        isShortsActive = false
        val durationMs = System.currentTimeMillis() - shortsSessionStartMs
        shortsSessionStartMs = 0L
        if (durationMs < MIN_SESSION_MS) return
        serviceScope.launch { usageRepository.addUsageSeconds(durationMs / 1000L) }
    }

    private fun eventTypeToString(eventType: Int): String = when (eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
        AccessibilityEvent.TYPE_VIEW_SCROLLED -> "VIEW_SCROLLED"
        else -> "TYPE_$eventType"
    }

    override fun onInterrupt() { endShortsSession() }

    override fun onDestroy() {
        endShortsSession()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val DEBOUNCE_MS = 200L
        private const val MIN_SESSION_MS = 1_000L
        private const val DEBUG_WRITE_THROTTLE_MS = 400L
        private const val KILL_DELAY_MS = 400L
    }
}
