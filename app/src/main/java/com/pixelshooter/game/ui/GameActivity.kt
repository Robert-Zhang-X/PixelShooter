package com.pixelshooter.game.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.pixelshooter.data.SaveManager
import com.pixelshooter.game.audio.AudioManager
import com.pixelshooter.game.engine.GameConfig
import com.pixelshooter.game.engine.GameState
import com.pixelshooter.game.entities.PlaneType
import android.widget.*
import android.view.View
import android.graphics.Color
import com.pixelshooter.R

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var tvScore: TextView
    private lateinit var tvHp: TextView
    private lateinit var tvBombs: TextView
    private lateinit var progressBossHp: ProgressBar
    private lateinit var tvBossLabel: TextView
    private lateinit var btnBomb: Button
    private lateinit var btnSkill: Button
    private lateinit var layoutGameOver: View
    private lateinit var layoutLevelComplete: View
    private lateinit var tvFinalScore: TextView

    private var levelId = 1
    private var planeType = PlaneType.FALCON

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // 初始化音频管理器
        AudioManager.initialize(this)
        AudioManager.playBGM(this)

        levelId = intent.getIntExtra("level_id", 1)
        planeType = PlaneType.valueOf(intent.getStringExtra("plane_type") ?: PlaneType.FALCON.name)

        setContentView(R.layout.activity_game)
        bindViews()

        gameView = GameView(
            context = this,
            levelId = levelId,
            planeType = planeType,
            onScoreUpdate = { score ->
                runOnUiThread { tvScore.text = "分数: $score" }
            },
            onHpUpdate = { hp, maxHp ->
                runOnUiThread { tvHp.text = "HP: $hp/$maxHp" }
            },
            onBombUpdate = { bombs ->
                runOnUiThread { tvBombs.text = "炸弹×$bombs" }
            },
            onStateChange = { state ->
                runOnUiThread { handleStateChange(state) }
            },
            onBossHpUpdate = { hp, maxHp ->
                runOnUiThread {
                    progressBossHp.max = maxHp
                    progressBossHp.progress = hp
                    tvBossLabel.visibility = View.VISIBLE
                    progressBossHp.visibility = View.VISIBLE
                }
            },
            onSkillCooldown = { progress ->
                runOnUiThread { btnSkill.alpha = 0.4f + progress * 0.6f }
            }
        )

        val container = findViewById<FrameLayout>(R.id.game_container)
        container.addView(gameView)

        btnBomb.setOnClickListener { gameView.useBomb() }
        btnSkill.setOnClickListener { gameView.useSkill() }
    }

    private fun bindViews() {
        tvScore = findViewById(R.id.tv_score)
        tvHp = findViewById(R.id.tv_hp)
        tvBombs = findViewById(R.id.tv_bombs)
        progressBossHp = findViewById(R.id.progress_boss_hp)
        tvBossLabel = findViewById(R.id.tv_boss_label)
        btnBomb = findViewById(R.id.btn_bomb)
        btnSkill = findViewById(R.id.btn_skill)
        layoutGameOver = findViewById(R.id.layout_game_over)
        layoutLevelComplete = findViewById(R.id.layout_level_complete)
        // tv_final_score 在 game_over 层；关卡完成层使用 tv_level_complete_score
        tvFinalScore = findViewById(R.id.tv_final_score)

        progressBossHp.visibility = View.GONE
        tvBossLabel.visibility = View.GONE
        layoutGameOver.visibility = View.GONE
        layoutLevelComplete.visibility = View.GONE
    }

    private fun handleStateChange(state: GameState) {
        when (state) {
            GameState.GAME_OVER -> showGameOver()
            GameState.LEVEL_COMPLETE -> showLevelComplete()
            GameState.BOSS_FIGHT -> {
                tvBossLabel.visibility = View.VISIBLE
                progressBossHp.visibility = View.VISIBLE
            }
            else -> {}
        }
    }

    private fun showGameOver() {
        gameView.pauseGame()
        layoutGameOver.visibility = View.VISIBLE
        tvFinalScore.text = "最终得分: ${gameView.getScore()}"
        SaveManager.updateBestScore(this, levelId, gameView.getScore())

        findViewById<Button>(R.id.btn_retry).setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("level_id", levelId)
            intent.putExtra("plane_type", planeType.name)
            startActivity(intent)
            finish()
        }
        findViewById<Button>(R.id.btn_main_menu).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showLevelComplete() {
        gameView.pauseGame()
        layoutLevelComplete.visibility = View.VISIBLE
        val score = gameView.getScore()
        findViewById<TextView>(R.id.tv_level_complete_score).text = "本关得分: $score"
        SaveManager.updateBestScore(this, levelId, score)
        if (levelId < 5) SaveManager.unlockLevel(this, levelId + 1)

        findViewById<Button>(R.id.btn_next_level).apply {
            isEnabled = levelId < 5
            setOnClickListener {
                val intent = Intent(this@GameActivity, PlaneSelectActivity::class.java)
                intent.putExtra("level_id", levelId + 1)
                startActivity(intent)
                finish()
            }
        }
        findViewById<Button>(R.id.btn_back_select).setOnClickListener {
            startActivity(Intent(this, LevelSelectActivity::class.java))
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        gameView.pauseGame()
        AudioManager.pauseBGM()
    }

    override fun onResume() {
        super.onResume()
        gameView.resumeGame()
        AudioManager.resumeBGM()
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioManager.release()
    }
}
