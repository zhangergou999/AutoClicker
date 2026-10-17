package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoClickService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var busy = false            // 连点进行中时不再触发新规则
    private var lastDone = 0L
    private val cooldownMs = 1500L      // 一条规则执行后的冷却，避免反复触发

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (busy || !Rules.enabled(this)) return
        if (System.currentTimeMillis() - lastDone < cooldownMs) return
        val root = rootInActiveWindow ?: return
        if (root.packageName == packageName) return   // 不处理本 App 自己的界面

        for (rule in Rules.load(this)) {
            val nodes = root.findAccessibilityNodeInfosByText(rule.keyword)
            if (nodes.isNullOrEmpty()) continue
            val r = Rect().also { nodes[0].getBoundsInScreen(it) }
            execute(rule, nodes[0], r.exactCenterX(), r.exactCenterY())
            return
        }
    }

    private fun execute(rule: Rule, node: AccessibilityNodeInfo, cx: Float, cy: Float) {
        val a = rule.args
        when (rule.act) {
            Act.CLICK -> {
                if (a.size >= 2) tap(a[0], a[1]) else clickNode(node, cx, cy)
                done()
            }
            Act.DOUBLE -> {
                val x = a.getOrNull(0) ?: cx
                val y = a.getOrNull(1) ?: cy
                tap(x, y)
                handler.postDelayed({ tap(x, y); done() }, 80)
            }
            Act.SWIPE -> {
                if (a.size >= 4) swipe(a[0], a[1], a[2], a[3], (a.getOrNull(4) ?: 300f).toLong())
                done()
            }
            Act.REPEAT -> {
                val x = a.getOrNull(0) ?: cx
                val y = a.getOrNull(1) ?: cy
                val count = (a.getOrNull(2) ?: 0f).toInt()                    // 0 = 一直点
                val interval = (a.getOrNull(3) ?: 100f).toLong().coerceAtLeast(50)
                startRepeat(rule.keyword, x, y, count, interval)
            }
        }
    }

    private fun startRepeat(keyword: String, x: Float, y: Float, count: Int, interval: Long) {
        busy = true
        var n = 0
        val tick = object : Runnable {
            override fun run() {
                val stop = !Rules.enabled(this@AutoClickService) ||
                        (count > 0 && n >= count) ||
                        (count == 0 && !keywordVisible(keyword))   // 无限模式：关键词消失就停
                if (stop) { busy = false; done(); return }
                tap(x, y); n++
                handler.postDelayed(this, interval)
            }
        }
        handler.post(tick)
    }

    private fun keywordVisible(keyword: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return !root.findAccessibilityNodeInfosByText(keyword).isNullOrEmpty()
    }

    private fun done() { lastDone = System.currentTimeMillis() }

    private fun clickNode(node: AccessibilityNodeInfo, cx: Float, cy: Float) {
        var n: AccessibilityNodeInfo? = node
        while (n != null) {
            if (n.isClickable && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
            n = n.parent
        }
        tap(cx, cy)
    }

    private fun tap(x: Float, y: Float) {
        val p = Path().apply { moveTo(x, y) }
        gesture(GestureDescription.StrokeDescription(p, 0, 30))
    }

    private fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long) {
        val p = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        gesture(GestureDescription.StrokeDescription(p, 0, duration.coerceAtLeast(50)))
    }

    private fun gesture(s: GestureDescription.StrokeDescription) {
        dispatchGesture(GestureDescription.Builder().addStroke(s).build(), null, null)
    }

    override fun onInterrupt() { busy = false }
}
