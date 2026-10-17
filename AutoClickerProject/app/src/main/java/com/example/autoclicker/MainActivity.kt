package com.example.autoclicker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tip = TextView(this).apply {
            text = """
规则（每行一条，# 开头为注释）：关键词|动作|参数

跳过|click
领取|click|540,1800
签到|double|540,1800
抢购|repeat|540,1800,50,100
加载更多|swipe|540,1800,540,600,300

repeat 参数：x,y,次数,间隔毫秒（次数0=一直点，直到文字消失）
swipe 参数：x1,y1,x2,y2,时长毫秒
坐标可在 开发者选项→指针位置 里查看
""".trimIndent()
        }
        val edit = EditText(this).apply {
            setText(Rules.loadText(this@MainActivity))
            gravity = Gravity.TOP
            minLines = 8
        }
        val sw = Switch(this).apply {
            text = "总开关"
            isChecked = Rules.enabled(this@MainActivity)
            setOnCheckedChangeListener { _, on -> Rules.setEnabled(this@MainActivity, on) }
        }
        val save = Button(this).apply {
            text = "保存规则"
            setOnClickListener {
                Rules.saveText(this@MainActivity, edit.text.toString())
                Toast.makeText(this@MainActivity, "已保存", Toast.LENGTH_SHORT).show()
            }
        }
        val open = Button(this).apply {
            text = "去开启无障碍服务"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        setContentView(ScrollView(this).apply {
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 96, 48, 48)
                addView(sw); addView(tip); addView(edit); addView(save); addView(open)
            })
        })
    }
}
