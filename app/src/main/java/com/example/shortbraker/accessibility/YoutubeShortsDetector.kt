package com.example.shortbraker.accessibility

import android.view.accessibility.AccessibilityNodeInfo

class YoutubeShortsDetector {

    fun detect(
        packageName: String?,
        rootNode: AccessibilityNodeInfo?,
        eventType: Int,
        consecutiveScrollCount: Int,
        windowClassName: String? = null
    ): ShortsDetectionResult {
        if (packageName != YOUTUBE_PACKAGE) {
            return ShortsDetectionResult(false, 0f, listOf("YouTube 앱 아님"))
        }
        if (rootNode == null) {
            return ShortsDetectionResult(false, 0f, listOf("접근성 노드 없음"))
        }

        // 1순위: View ID 기반 감지 (가장 신뢰도 높음)
        for (viewId in SHORTS_VIEW_IDS) {
            val nodes = try {
                rootNode.findAccessibilityNodeInfosByViewId(viewId)
            } catch (e: Exception) {
                emptyList()
            }
            if (nodes.isNotEmpty()) {
                nodes.forEach { it.recycle() }
                return ShortsDetectionResult(
                    isShorts = true,
                    confidence = 1.0f,
                    reasons = listOf("Shorts 뷰ID 감지: ${viewId.substringAfterLast('/')}")
                )
            }
        }

        // 2순위: 창 클래스명 기반 감지
        if (windowClassName != null) {
            val cls = windowClassName.lowercase()
            if (SHORTS_CLASS_KEYWORDS.any { cls.contains(it) }) {
                return ShortsDetectionResult(
                    isShorts = true,
                    confidence = 0.95f,
                    reasons = listOf("Shorts 창 클래스 감지: $windowClassName")
                )
            }
        }

        // 3순위: 텍스트 기반 신뢰도 점수
        val reasons = mutableListOf<String>()
        var confidence = 0f
        val allText = collectAllText(rootNode)

        val hasShortsKeyword = allText.any { it.contains("shorts") || it.contains("쇼츠") }
        if (hasShortsKeyword) {
            reasons.add("'Shorts/쇼츠' 텍스트 감지")
            confidence += 0.5f
        }

        val socialKo = listOf("좋아요", "싫어요", "댓글", "공유", "저장", "팔로우")
        val socialEn = listOf("like", "dislike", "comment", "share", "save", "follow")
        val foundSocial = maxOf(
            socialKo.count { kw -> allText.any { it.contains(kw) } },
            socialEn.count { kw -> allText.any { it.contains(kw) } }
        )
        if (foundSocial >= 2) {
            reasons.add("소셜 버튼 ${foundSocial}개 감지")
            confidence += (0.1f * foundSocial).coerceAtMost(0.35f)
        }

        val hasSubscribe = allText.any { it.contains("구독") || it.contains("subscribe") }
        if (hasSubscribe && hasShortsKeyword) {
            reasons.add("구독 버튼 + Shorts 키워드")
            confidence += 0.15f
        }

        if (consecutiveScrollCount >= 3) {
            reasons.add("연속 스크롤 ${consecutiveScrollCount}회")
            confidence += 0.1f
        }

        val feedKeywords = listOf("홈", "탐색", "보관함", "home", "explore", "library")
        val isFeedScreen = feedKeywords.count { kw -> allText.any { it.contains(kw) } } >= 3
        if (isFeedScreen && !hasShortsKeyword) {
            reasons.add("일반 피드 화면")
            confidence -= 0.2f
        }

        val finalConfidence = confidence.coerceIn(0f, 1f)
        return ShortsDetectionResult(
            isShorts = finalConfidence >= SHORTS_CONFIDENCE_THRESHOLD,
            confidence = finalConfidence,
            reasons = reasons.ifEmpty { listOf("YouTube 앱이지만 Shorts 지표 없음") }
        )
    }

    private fun collectAllText(root: AccessibilityNodeInfo): Set<String> {
        val results = mutableSetOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var nodeCount = 0

        while (queue.isNotEmpty() && nodeCount < MAX_NODES) {
            val node = queue.removeFirst()
            nodeCount++
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { results.add(it.lowercase()) }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { results.add(it.lowercase()) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return results
    }

    companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val SHORTS_CONFIDENCE_THRESHOLD = 0.3f
        private const val MAX_NODES = 300

        private val SHORTS_VIEW_IDS = listOf(
            "com.google.android.youtube:id/reel_player_page",
            "com.google.android.youtube:id/shorts_container",
            "com.google.android.youtube:id/reels_player_fragment_container",
            "com.google.android.youtube:id/reel_channel_bar_container",
            "com.google.android.youtube:id/reel_like_button",
            "com.google.android.youtube:id/reel_comment_button",
            "com.google.android.youtube:id/reel_share_button",
        )

        private val SHORTS_CLASS_KEYWORDS = listOf(
            "shorts", "reel", "reelwatchfragment", "shortsactivity"
        )
    }
}
