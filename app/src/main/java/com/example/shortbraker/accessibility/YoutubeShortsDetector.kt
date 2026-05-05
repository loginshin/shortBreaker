package com.example.shortbraker.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class YoutubeShortsDetector {

    fun detect(
        packageName: String?,
        rootNode: AccessibilityNodeInfo?,
        eventType: Int,
        consecutiveScrollCount: Int
    ): ShortsDetectionResult {
        if (packageName != YOUTUBE_PACKAGE) {
            return ShortsDetectionResult(false, 0f, listOf("YouTube 앱 아님"))
        }
        if (rootNode == null) {
            return ShortsDetectionResult(false, 0f, listOf("접근성 노드 없음"))
        }

        val reasons = mutableListOf<String>()
        var confidence = 0f

        val allText = collectAllText(rootNode)

        // 조건 1: "Shorts" 또는 "쇼츠" 텍스트 감지 (가장 강한 지표)
        val hasShortsKeyword = allText.any {
            it.contains("shorts") || it.contains("쇼츠")
        }
        if (hasShortsKeyword) {
            reasons.add("'Shorts/쇼츠' 텍스트 감지")
            confidence += 0.45f
        }

        // 조건 2: Shorts 화면 특유의 소셜 액션 버튼 조합 감지
        // Shorts는 like/comment/share 버튼이 세로로 나란히 나타남
        val socialKeywords = listOf("좋아요", "싫어요", "댓글", "공유", "저장")
        val englishSocialKeywords = listOf("like", "dislike", "comment", "share", "save")
        val foundKorean = socialKeywords.count { kw -> allText.any { it.contains(kw) } }
        val foundEnglish = englishSocialKeywords.count { kw -> allText.any { it.contains(kw) } }
        val totalSocial = maxOf(foundKorean, foundEnglish)
        if (totalSocial >= 2) {
            reasons.add("소셜 액션 버튼 ${totalSocial}개 감지")
            confidence += (0.1f * totalSocial).coerceAtMost(0.3f)
        }

        // 조건 3: 구독 버튼이 보이는 경우 (Shorts에서 채널 구독 버튼은 재생 화면에 겹쳐 표시)
        val hasSubscribe = allText.any { it.contains("구독") || it.contains("subscribe") }
        if (hasSubscribe && hasShortsKeyword) {
            reasons.add("구독 버튼 감지 (Shorts 재생 화면)")
            confidence += 0.1f
        }

        // 조건 4: 연속 스크롤 이벤트 (Shorts 탐색 패턴)
        if (consecutiveScrollCount >= 3) {
            reasons.add("연속 스크롤 ${consecutiveScrollCount}회 감지")

            confidence += 0.1f
        }

        // 조건 5: 일반 YouTube 피드 지표가 있으면 confidence 감소
        val feedKeywords = listOf("홈", "탐색", "구독", "보관함", "home", "explore", "library")
        val isFeedScreen = feedKeywords.count { kw -> allText.any { it.contains(kw) } } >= 3
        if (isFeedScreen && !hasShortsKeyword) {
            reasons.add("일반 피드 화면 지표 감지")
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

            node.text?.toString()?.takeIf { it.isNotBlank() }?.let {
                results.add(it.lowercase())
            }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let {
                results.add(it.lowercase())
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return results
    }

    companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val SHORTS_CONFIDENCE_THRESHOLD = 0.4f
        private const val MAX_NODES = 200
    }
}
