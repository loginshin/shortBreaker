package com.example.shortbraker.data

data class UserSettings(
    val dailyLimitMinutes: Int = 30,
    val blockMode: String = "NORMAL",
    val bedtimeModeEnabled: Boolean = false,
    val bedtimeStart: String = "23:30",
    val bedtimeEnd: String = "07:00"
)
