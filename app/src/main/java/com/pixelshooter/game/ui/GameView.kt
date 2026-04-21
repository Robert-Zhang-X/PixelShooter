package com.pixelshooter.game.ui

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.pixelshooter.game.engine.*
import com.pixelshooter.game.entities.PlaneType

class GameView(
    context: Context,
    private val levelId: Int,
    private val planeType: PlaneType,
    private val onScoreUpdate: (Int) -> Unit,
    private val onHpUpdate: (Int, Int) -> Unit,
    private val onBombUpdate: (Int) -> Unit,
    private val onStateChange: (GameState) -> Unit,
    private val onBossHpUpdate: (Int, Int) -> Unit,
    private val onSkillCooldown: (Float) -> Unit
) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private val engine: GameEngine
    private val renderer = GameRenderer()
    private var gameThread: Thread? = null
    private var isRunning = false

    // 触控
    private var touchX = GameConfig.LOGICAL_WIDTH / 2
    private var touchY = GameConfig.LOGICAL_HEIGHT - 80f
    private var isTouching = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    init {
        holder.addCallback(this)
        engine = GameEngine(
            levelId, planeType,
            onScoreUpdate, onHpUpdate, onBombUpdate,
            onStateChange, onBossHpUpdate, onSkillCooldown
        )
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderer.setScreenSize(width, height)
        isRunning = true
        gameThread = Thread(this).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderer.setScreenSize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        gameThread?.join(2000)
    }

    // ==================== 游戏主循环 ====================
    override fun run() {
        var lastTime = System.currentTimeMillis()
        while (isRunning) {
            val now = System.currentTimeMillis()
            val delta = now - lastTime
            lastTime = now

            // 更新游戏逻辑
            engine.update(delta)

            // 跟随手指移动玩家
            if (isTouching) {
                engine.player.x = renderer.toLogicX(touchX)
                engine.player.y = renderer.toLogicY(touchY)
            }

            // 渲染
            val canvas = holder.lockCanvas() ?: continue
            try {
                renderer.render(canvas, engine, engine.bgOffset)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }

            // 帧率控制
            val elapsed = System.currentTimeMillis() - now
            val sleep = GameConfig.FRAME_TIME_MS - elapsed
            if (sleep > 0) Thread.sleep(sleep)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                isTouching = true
                touchX = event.x
                touchY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
            }
        }
        return true
    }

    fun useBomb() = engine.useBomb()
    fun useSkill() = engine.useSkill()
    fun pauseGame() { isRunning = false }
    fun resumeGame() {
        if (!isRunning) {
            isRunning = true
            gameThread = Thread(this).also { it.start() }
        }
    }

    fun getScore() = engine.player.score
}
