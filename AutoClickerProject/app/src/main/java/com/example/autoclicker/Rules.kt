package com.example.autoclicker

import android.content.Context

enum class Act { CLICK, DOUBLE, REPEAT, SWIPE }

/**
 * 规则格式（每行一条，# 开头为注释）：
 *   关键词|动作|参数
 *
 * 动作 / 参数：
 *   click  点击      参数可省略(点文字位置)，或 x,y
 *   double 双击      参数可省略，或 x,y
 *   repeat 连点      x,y,次数,间隔毫秒   （次数填0=一直点，直到关键词消失或关闭开关；x,y 可省略）
 *   swipe  滑动      x1,y1,x2,y2,时长毫秒
 * 动作也可写中文：点击 / 双击 / 连点 / 滑动
 */
data class Rule(val keyword: String, val act: Act, val args: List<Float>)

object Rules {
    private const val PREF = "rules"

    fun saveText(ctx: Context, text: String) =
        ctx.getSharedPreferences(PREF, 0).edit().putString("text", text).apply()

    fun loadText(ctx: Context): String =
        ctx.getSharedPreferences(PREF, 0).getString("text", "") ?: ""

    fun setEnabled(ctx: Context, on: Boolean) =
        ctx.getSharedPreferences(PREF, 0).edit().putBoolean("enabled", on).apply()

    fun enabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, 0).getBoolean("enabled", true)

    fun load(ctx: Context): List<Rule> = loadText(ctx).lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val p = line.split("|").map { it.trim() }
            val kw = p[0]
            if (kw.isEmpty()) return@mapNotNull null
            val act = when (p.getOrNull(1)?.lowercase()) {
                null, "", "click", "点击" -> Act.CLICK
                "double", "双击" -> Act.DOUBLE
                "repeat", "连点" -> Act.REPEAT
                "swipe", "滑动" -> Act.SWIPE
                else -> return@mapNotNull null
            }
            val args = p.getOrNull(2)?.replace("，", ",")?.split(",")
                ?.mapNotNull { it.trim().toFloatOrNull() } ?: emptyList()
            Rule(kw, act, args)
        }
}
