package com.example.shortbraker.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class OverlayManager(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null

    fun showBlockOverlay() {
        if (!Settings.canDrawOverlays(context)) return
        if (overlayView != null) return
        try {
            val view = buildOverlayView()
            windowManager.addView(view, buildLayoutParams())
            overlayView = view
        } catch (e: Exception) {
            overlayView = null
        }
    }

    fun hideBlockOverlay() {
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            // view already detached
        }
    }

    fun isShowing(): Boolean = overlayView != null

    private fun buildLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

    private fun buildOverlayView(): View {
        fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(160, 0, 0, 0))
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
            }
        }

        val title = TextView(context).apply {
            text = "오늘 Shorts 제한 시간을 초과했어요"
            textSize = 17f
            setTextColor(Color.parseColor("#1C1B1F"))
            setPadding(0, 0, 0, dp(10))
        }

        val subtitle = TextView(context).apply {
            text = "잠깐 쉬었다가 다시 돌아와요"
            textSize = 14f
            setTextColor(Color.parseColor("#49454F"))
            setPadding(0, 0, 0, dp(24))
        }

        val closeButton = TextView(context).apply {
            text = "닫기"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(13), dp(24), dp(13))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6750A4"))
                cornerRadius = dp(8).toFloat()
            }
            contentDescription = "닫기"
            setOnClickListener { hideBlockOverlay() }
        }

        card.addView(title)
        card.addView(subtitle)
        card.addView(closeButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val margin = dp(32)
        val cardParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
            setMargins(margin, 0, margin, 0)
        }
        root.addView(card, cardParams)

        return root
    }
}
