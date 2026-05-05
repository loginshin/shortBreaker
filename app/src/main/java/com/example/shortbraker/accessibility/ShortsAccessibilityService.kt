package com.example.shortbraker.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.shortbraker.data.ShortsDebugRepository
import com.example.shortbraker.data.ShortsDebugState
import com.example.shortbraker.data.ShortsUsageRepository
import com.example.shortbraker.data.UserSettingsRepository
import com.example.shortbraker.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ShortsAccessibilityService : AccessibilityService() {

    private val detector = YoutubeShortsDetector()
    private lateinit var usageRepository: ShortsUsageRepository
    private lateinit var debugRepository: ShortsDebugRepository
    private lateinit var settingsRepository: UserSettingsRepository
    private lateinit var overlayManager: OverlayManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isShortsActive = false
    private var shortsSessionStartMs = 0L
    private var consecutiveScrollCount = 0
    private var lastAnalysisMs = 0L
    private var lastDebugWriteMs = 0L

    // 두 Flow에서 수집한 값을 메인 스레드 비교용으로 캐싱
    @Volatile private var dailyLimitMinutes = 0
    @Volatile private var todayShortsSeconds = 0L
    @Volatile private var blockMode = "NORMAL"
    @Volatile private var redirectAppPackage = ""

    override fun onCreate() {
        super.onCreate()
        usageRepository = ShortsUsageRepository(applicationContext)
        debugRepository = ShortsDebugRepository(applicationContext)
        settingsRepository = UserSettingsRepository(applicationContext)
        overlayManager = OverlayManager(applicationContext)

        serviceScope.launch {
            settingsRepository.userSettings.collect { settings ->
                dailyLimitMinutes = settings.dailyLimitMinutes
                blockMode = settings.blockMode
                redirectAppPackage = settings.redirectAppPackage
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
            overlayManager.hideBlockOverlay()
            consecutiveScrollCount = 0
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

        analyzeCurrentScreen(event.eventType)
        consecutiveScrollCount = 0
    }

    private fun analyzeCurrentScreen(eventType: Int) {
        val root = rootInActiveWindow ?: return
        val result = detector.detect(
            packageName = YoutubeShortsDetector.YOUTUBE_PACKAGE,
            rootNode = root,
            eventType = eventType,
            consecutiveScrollCount = consecutiveScrollCount
        )

        when {
            result.isShorts && !isShortsActive -> startShortsSession()
            !result.isShorts && isShortsActive -> endShortsSession()
        }

        if (result.isShorts) {
            checkAndShowBlockOverlay()
        } else {
            overlayManager.hideBlockOverlay()
        }

        val now = System.currentTimeMillis()
        if (now - lastDebugWriteMs >= DEBUG_WRITE_THROTTLE_MS) {
            lastDebugWriteMs = now
            val sessionStart = if (isShortsActive) shortsSessionStartMs else 0L
            val debugState = ShortsDebugState(
                lastDetectedPackageName = YoutubeShortsDetector.YOUTUBE_PACKAGE,
                lastEventType = eventTypeToString(eventType),
                isCurrentlyShorts = result.isShorts,
                lastConfidence = result.confidence,
                lastDetectionReasons = result.reasons.joinToString(", "),
                lastDetectionTimeMillis = now,
                currentSessionStartedAt = sessionStart
            )
            serviceScope.launch {
                debugRepository.saveDebugState(debugState)
            }
        }
    }

    private fun checkAndShowBlockOverlay() {
        if (dailyLimitMinutes <= 0) return
        val activeSessionSeconds = if (isShortsActive && shortsSessionStartMs > 0L) {
            (System.currentTimeMillis() - shortsSessionStartMs) / 1000L
        } else {
            0L
        }
        if (todayShortsSeconds + activeSessionSeconds < dailyLimitMinutes * 60L) return

        // 리다이렉트 앱 설정 시 → 해당 앱 실행 후 차단 모드 생략
        if (redirectAppPackage.isNotEmpty()) {
            launchRedirectApp()
            return
        }

        when (blockMode) {
            "STRICT" -> {
                overlayManager.showBlockOverlay()
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            "SOFT" -> { /* 알림 기능 준비 중 */ }
            else -> overlayManager.showBlockOverlay() // NORMAL
        }
    }

    private fun launchRedirectApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(redirectAppPackage) ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // 앱이 제거됐거나 실행 불가 — 무시
        }
    }

    private fun startShortsSession() {
        isShortsActive = true
        shortsSessionStartMs = System.currentTimeMillis()
        serviceScope.launch {
            usageRepository.incrementOpenCount()
        }
    }

    private fun endShortsSession() {
        if (!isShortsActive) return
        isShortsActive = false
        val durationMs = System.currentTimeMillis() - shortsSessionStartMs
        shortsSessionStartMs = 0L

        if (durationMs < MIN_SESSION_MS) return

        val durationSec = durationMs / 1000L
        serviceScope.launch {
            usageRepository.addUsageSeconds(durationSec)
        }
    }

    private fun eventTypeToString(eventType: Int): String = when (eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
        AccessibilityEvent.TYPE_VIEW_SCROLLED -> "VIEW_SCROLLED"
        else -> "TYPE_$eventType"
    }

    override fun onInterrupt() {
        endShortsSession()
        overlayManager.hideBlockOverlay()
    }

    override fun onDestroy() {
        endShortsSession()
        overlayManager.hideBlockOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val DEBOUNCE_MS = 200L
        private const val MIN_SESSION_MS = 1_000L
        private const val DEBUG_WRITE_THROTTLE_MS = 400L
    }
}
