package com.example.shortbraker.accessibility

data class ShortsDetectionResult(
    val isShorts: Boolean,
    val confidence: Float,
    val reasons: List<String>
)
