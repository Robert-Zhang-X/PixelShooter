package com.pixelshooter.game.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.pixelshooter.R
import com.pixelshooter.data.SaveManager

class LevelSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_level_select)

        val levelNames = listOf("蓝天边境", "火山峡谷", "深海上空", "雷暴之城", "宇宙终点")
        val levelIds = listOf(R.id.btn_level1, R.id.btn_level2, R.id.btn_level3, R.id.btn_level4, R.id.btn_level5)
        val bestIds = listOf(R.id.tv_best1, R.id.tv_best2, R.id.tv_best3, R.id.tv_best4, R.id.tv_best5)

        val saveData = SaveManager.load(this)

        for (i in 0 until 5) {
            val levelId = i + 1
            val btn = findViewById<Button>(levelIds[i])
            val tvBest = findViewById<TextView>(bestIds[i])

            val unlocked = saveData.unlockedLevels.contains(levelId)
            btn.isEnabled = unlocked
            btn.text = if (unlocked) "第${levelId}关\n${levelNames[i]}" else "🔒 第${levelId}关"
            btn.alpha = if (unlocked) 1.0f else 0.5f

            val best = saveData.bestScores[levelId]
            tvBest.text = if (best != null) "最高: $best" else ""

            btn.setOnClickListener {
                val intent = Intent(this, PlaneSelectActivity::class.java)
                intent.putExtra("level_id", levelId)
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }
    }
}
