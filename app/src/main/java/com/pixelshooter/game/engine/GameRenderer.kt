package com.pixelshooter.game.engine

import android.graphics.*
import android.graphics.Color
import com.pixelshooter.game.entities.*
import com.pixelshooter.game.entities.enemies.*
import com.pixelshooter.game.entities.bosses.*
import kotlin.math.sin

/**
 * 游戏渲染器：负责将所有游戏实体绘制到 Canvas 上
 * 使用纯 Paint + Canvas 绘制像素风格图形（无需外部图片资源）
 */
class GameRenderer(private val config: GameConfig = GameConfig) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
    }

    // 坐标缩放比（逻辑坐标 -> 实际屏幕坐标）
    var scaleX = 1f
    var scaleY = 1f
    var offsetX = 0f  // 左右留黑边（letterbox）

    fun setScreenSize(screenW: Int, screenH: Int) {
        val logW = GameConfig.LOGICAL_WIDTH
        val logH = GameConfig.LOGICAL_HEIGHT
        scaleX = screenW / logW
        scaleY = screenH / logH
        val scale = minOf(scaleX, scaleY)
        scaleX = scale; scaleY = scale
        offsetX = (screenW - logW * scale) / 2f
    }

    fun toScreenX(lx: Float) = offsetX + lx * scaleX
    fun toScreenY(ly: Float) = ly * scaleY
    fun toLogicX(sx: Float) = (sx - offsetX) / scaleX
    fun toLogicY(sy: Float) = sy / scaleY

    fun render(canvas: Canvas, engine: GameEngine, bgOffset: Float) {
        // 清除整个画布（包括黑边区域）
        canvas.drawColor(Color.BLACK)
        
        canvas.save()
        canvas.translate(offsetX, 0f)
        canvas.scale(scaleX, scaleY)

        drawBackground(canvas, engine, bgOffset)
        drawItems(canvas, engine.items)
        drawEnemies(canvas, engine.enemies)
        engine.boss?.let { drawBoss(canvas, it) }
        drawBullets(canvas, engine.bullets)
        drawPlayer(canvas, engine.player)
        drawPlayerHpBar(canvas, engine.player)  // 绘制玩家血量条

        canvas.restore()
    }

    // ==================== 背景 ====================
    private fun drawBackground(canvas: Canvas, engine: GameEngine, bgOffset: Float) {
        val level = engine.levelId
        val (topColor, botColor) = when (level) {
            1 -> Pair(0xFF87CEEB.toInt(), 0xFF4169E1.toInt())
            2 -> Pair(0xFFFF4500.toInt(), 0xFF8B0000.toInt())
            3 -> Pair(0xFF006994.toInt(), 0xFF003366.toInt())
            4 -> Pair(0xFF2F4F4F.toInt(), 0xFF1C1C1C.toInt())
            else -> Pair(0xFF0D0D2B.toInt(), 0xFF000000.toInt())
        }
        val gradient = LinearGradient(0f, 0f, 0f, GameConfig.LOGICAL_HEIGHT,
            topColor, botColor, Shader.TileMode.CLAMP)
        paint.shader = gradient
        canvas.drawRect(0f, 0f, GameConfig.LOGICAL_WIDTH, GameConfig.LOGICAL_HEIGHT, paint)
        paint.shader = null

        // 滚动星点/云朵
        paint.color = 0x55FFFFFF
        paint.style = Paint.Style.FILL
        val stars = listOf(
            Pair(50f, 80f), Pair(120f, 200f), Pair(200f, 50f),
            Pair(280f, 150f), Pair(330f, 300f), Pair(80f, 400f),
            Pair(160f, 500f), Pair(260f, 600f), Pair(310f, 700f)
        )
        stars.forEach { (sx, sy) ->
            val yy = ((sy + bgOffset) % GameConfig.LOGICAL_HEIGHT)
            canvas.drawCircle(sx, yy, if (level == 5) 2f else 3f, paint)
        }
    }

    // ==================== 玩家飞机 ====================
    private fun drawPlayer(canvas: Canvas, player: PlayerPlane) {
        if (!player.isAlive) return
        if (player.isInvincible && (System.currentTimeMillis() / 100) % 2 == 0L) return

        val x = player.x; val y = player.y
        val w = player.width; val h = player.height

        // 护盾效果
        if (player.shieldTimeMs > 0) {
            paint.color = 0x4400BFFF
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, w * 0.85f, paint)
            paint.color = 0xFF00BFFF.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            canvas.drawCircle(x, y, w * 0.85f, paint)
            // 护盾六边形光纹
            paint.color = 0x2200BFFF
            canvas.drawCircle(x, y, w * 0.65f, paint)
            paint.style = Paint.Style.FILL
        }

        // 按飞机类型分别精细绘制
        when (player.planeType) {
            PlaneType.FALCON   -> drawFalcon(canvas, x, y, w, h)
            PlaneType.STORM    -> drawStorm(canvas, x, y, w, h)
            PlaneType.HUNTERII -> drawHunterII(canvas, x, y, w, h)
        }

        // 强化buff光效
        if (player.powerUpTimeMs > 0) {
            paint.color = 0x88FFFF00.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawRect(x - w / 2, y - h / 2, x + w / 2, y + h / 2, paint)
            paint.style = Paint.Style.FILL
        }
    }

    // ---- 猎鹰号：绿色流线型单发穿透机 ----
    private fun drawFalcon(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        // 机身（细长梭形）
        paint.color = 0xFF00DD77.toInt()
        val body = Path().apply {
            moveTo(x, y - h * 0.48f)          // 机头
            lineTo(x + w * 0.22f, y - h * 0.1f)
            lineTo(x + w * 0.18f, y + h * 0.45f)
            lineTo(x - w * 0.18f, y + h * 0.45f)
            lineTo(x - w * 0.22f, y - h * 0.1f)
            close()
        }
        canvas.drawPath(body, paint)
        // 主机翼（后掠翼）
        paint.color = 0xFF00AA55.toInt()
        val wingL = Path().apply {
            moveTo(x - w * 0.22f, y + h * 0.05f)
            lineTo(x - w * 0.52f, y + h * 0.35f)
            lineTo(x - w * 0.42f, y + h * 0.45f)
            lineTo(x - w * 0.18f, y + h * 0.3f)
            close()
        }
        canvas.drawPath(wingL, paint)
        val wingR = Path().apply {
            moveTo(x + w * 0.22f, y + h * 0.05f)
            lineTo(x + w * 0.52f, y + h * 0.35f)
            lineTo(x + w * 0.42f, y + h * 0.45f)
            lineTo(x + w * 0.18f, y + h * 0.3f)
            close()
        }
        canvas.drawPath(wingR, paint)
        // 尾翼
        paint.color = 0xFF00BB66.toInt()
        val tailL = Path().apply {
            moveTo(x - w * 0.18f, y + h * 0.3f)
            lineTo(x - w * 0.32f, y + h * 0.48f)
            lineTo(x - w * 0.12f, y + h * 0.48f)
            close()
        }
        canvas.drawPath(tailL, paint)
        val tailR = Path().apply {
            moveTo(x + w * 0.18f, y + h * 0.3f)
            lineTo(x + w * 0.32f, y + h * 0.48f)
            lineTo(x + w * 0.12f, y + h * 0.48f)
            close()
        }
        canvas.drawPath(tailR, paint)
        // 驾驶舱玻璃
        paint.color = 0xFF004422.toInt()
        canvas.drawOval(x - w * 0.1f, y - h * 0.32f, x + w * 0.1f, y - h * 0.02f, paint)
        paint.color = 0xBB88FFCC.toInt()
        canvas.drawOval(x - w * 0.07f, y - h * 0.30f, x + w * 0.07f, y - h * 0.06f, paint)
        // 机头高光
        paint.color = 0x8800FFAA.toInt()
        canvas.drawOval(x - w * 0.04f, y - h * 0.47f, x + w * 0.04f, y - h * 0.28f, paint)
        // 引擎火焰
        paint.color = 0xFFFF6600.toInt()
        canvas.drawOval(x - w * 0.1f, y + h * 0.43f, x + w * 0.1f, y + h * 0.58f, paint)
        paint.color = 0xFFFFCC00.toInt()
        canvas.drawOval(x - w * 0.06f, y + h * 0.43f, x + w * 0.06f, y + h * 0.53f, paint)
    }

    // ---- 风暴号：橙色宽翼散射机 ----
    private fun drawStorm(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        // 宽机身
        paint.color = 0xFFEE8800.toInt()
        val body = Path().apply {
            moveTo(x, y - h * 0.42f)
            lineTo(x + w * 0.28f, y + h * 0.0f)
            lineTo(x + w * 0.24f, y + h * 0.45f)
            lineTo(x - w * 0.24f, y + h * 0.45f)
            lineTo(x - w * 0.28f, y + h * 0.0f)
            close()
        }
        canvas.drawPath(body, paint)
        // 大展机翼
        paint.color = 0xFFCC6600.toInt()
        val wingL = Path().apply {
            moveTo(x - w * 0.28f, y - h * 0.05f)
            lineTo(x - w * 0.58f, y + h * 0.15f)
            lineTo(x - w * 0.55f, y + h * 0.38f)
            lineTo(x - w * 0.24f, y + h * 0.28f)
            close()
        }
        canvas.drawPath(wingL, paint)
        val wingR = Path().apply {
            moveTo(x + w * 0.28f, y - h * 0.05f)
            lineTo(x + w * 0.58f, y + h * 0.15f)
            lineTo(x + w * 0.55f, y + h * 0.38f)
            lineTo(x + w * 0.24f, y + h * 0.28f)
            close()
        }
        canvas.drawPath(wingR, paint)
        // 双引擎舱（左右凸出）
        paint.color = 0xFF994400.toInt()
        canvas.drawRoundRect(x - w * 0.38f, y + h * 0.1f, x - w * 0.22f, y + h * 0.45f, 4f, 4f, paint)
        canvas.drawRoundRect(x + w * 0.22f, y + h * 0.1f, x + w * 0.38f, y + h * 0.45f, 4f, 4f, paint)
        // 驾驶舱
        paint.color = 0xFF552200.toInt()
        canvas.drawOval(x - w * 0.12f, y - h * 0.28f, x + w * 0.12f, y + h * 0.05f, paint)
        paint.color = 0xBBFFCC88.toInt()
        canvas.drawOval(x - w * 0.09f, y - h * 0.26f, x + w * 0.09f, y - h * 0.0f, paint)
        // 机头高光
        paint.color = 0x88FFAA00.toInt()
        canvas.drawOval(x - w * 0.05f, y - h * 0.40f, x + w * 0.05f, y - h * 0.18f, paint)
        // 双引擎火焰
        paint.color = 0xFFFF6600.toInt()
        canvas.drawOval(x - w * 0.35f, y + h * 0.43f, x - w * 0.22f, y + h * 0.56f, paint)
        canvas.drawOval(x + w * 0.22f, y + h * 0.43f, x + w * 0.35f, y + h * 0.56f, paint)
        paint.color = 0xFFFFDD00.toInt()
        canvas.drawOval(x - w * 0.32f, y + h * 0.43f, x - w * 0.25f, y + h * 0.52f, paint)
        canvas.drawOval(x + w * 0.25f, y + h * 0.43f, x + w * 0.32f, y + h * 0.52f, paint)
    }

    // ---- 猎鹰II号：蓝色导弹追踪机 ----
    private fun drawHunterII(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        // 机身（稍宽，有厚重感）
        paint.color = 0xFF1188EE.toInt()
        val body = Path().apply {
            moveTo(x, y - h * 0.46f)
            lineTo(x + w * 0.24f, y - h * 0.08f)
            lineTo(x + w * 0.20f, y + h * 0.44f)
            lineTo(x - w * 0.20f, y + h * 0.44f)
            lineTo(x - w * 0.24f, y - h * 0.08f)
            close()
        }
        canvas.drawPath(body, paint)
        // 后掠机翼
        paint.color = 0xFF0066CC.toInt()
        val wingL = Path().apply {
            moveTo(x - w * 0.24f, y + h * 0.08f)
            lineTo(x - w * 0.55f, y + h * 0.32f)
            lineTo(x - w * 0.48f, y + h * 0.45f)
            lineTo(x - w * 0.20f, y + h * 0.30f)
            close()
        }
        canvas.drawPath(wingL, paint)
        val wingR = Path().apply {
            moveTo(x + w * 0.24f, y + h * 0.08f)
            lineTo(x + w * 0.55f, y + h * 0.32f)
            lineTo(x + w * 0.48f, y + h * 0.45f)
            lineTo(x + w * 0.20f, y + h * 0.30f)
            close()
        }
        canvas.drawPath(wingR, paint)
        // 导弹挂架（左右翼下）
        paint.color = 0xFF557799.toInt()
        canvas.drawRoundRect(x - w * 0.48f, y + h * 0.18f, x - w * 0.30f, y + h * 0.36f, 3f, 3f, paint)
        canvas.drawRoundRect(x + w * 0.30f, y + h * 0.18f, x + w * 0.48f, y + h * 0.36f, 3f, 3f, paint)
        // 导弹头（橙红色）
        paint.color = 0xFFFF5533.toInt()
        val missL = Path().apply { moveTo(x - w * 0.48f, y + h * 0.18f); lineTo(x - w * 0.39f, y + h * 0.10f); lineTo(x - w * 0.30f, y + h * 0.18f); close() }
        canvas.drawPath(missL, paint)
        val missR = Path().apply { moveTo(x + w * 0.30f, y + h * 0.18f); lineTo(x + w * 0.39f, y + h * 0.10f); lineTo(x + w * 0.48f, y + h * 0.18f); close() }
        canvas.drawPath(missR, paint)
        // 驾驶舱
        paint.color = 0xFF003366.toInt()
        canvas.drawOval(x - w * 0.11f, y - h * 0.32f, x + w * 0.11f, y + h * 0.02f, paint)
        paint.color = 0xBBAADDFF.toInt()
        canvas.drawOval(x - w * 0.08f, y - h * 0.30f, x + w * 0.08f, y - h * 0.04f, paint)
        // 机头高光
        paint.color = 0x880099FF.toInt()
        canvas.drawOval(x - w * 0.04f, y - h * 0.44f, x + w * 0.04f, y - h * 0.22f, paint)
        // 主引擎火焰
        paint.color = 0xFF4488FF.toInt()
        canvas.drawOval(x - w * 0.12f, y + h * 0.42f, x + w * 0.12f, y + h * 0.60f, paint)
        paint.color = 0xFFCCEEFF.toInt()
        canvas.drawOval(x - w * 0.07f, y + h * 0.42f, x + w * 0.07f, y + h * 0.54f, paint)
    }

    // ==================== 通用像素飞机绘制（敌机使用）====================
    private fun drawPixelPlane(
        canvas: Canvas, cx: Float, cy: Float,
        w: Float, h: Float, color: Int, isPlayer: Boolean
    ) {
        paint.color = color
        paint.style = Paint.Style.FILL
        // 机身
        val bodyRect = RectF(cx - w * 0.25f, cy - h * 0.45f, cx + w * 0.25f, cy + h * 0.45f)
        canvas.drawRect(bodyRect, paint)
        // 机翼
        val wingPath = Path().apply {
            if (isPlayer) {
                moveTo(cx - w * 0.5f, cy + h * 0.2f)
                lineTo(cx - w * 0.25f, cy - h * 0.1f)
                lineTo(cx - w * 0.25f, cy + h * 0.4f)
                close()
                moveTo(cx + w * 0.5f, cy + h * 0.2f)
                lineTo(cx + w * 0.25f, cy - h * 0.1f)
                lineTo(cx + w * 0.25f, cy + h * 0.4f)
                close()
            } else {
                moveTo(cx - w * 0.5f, cy - h * 0.1f)
                lineTo(cx - w * 0.25f, cy - h * 0.3f)
                lineTo(cx - w * 0.25f, cy + h * 0.2f)
                close()
                moveTo(cx + w * 0.5f, cy - h * 0.1f)
                lineTo(cx + w * 0.25f, cy - h * 0.3f)
                lineTo(cx + w * 0.25f, cy + h * 0.2f)
                close()
            }
        }
        canvas.drawPath(wingPath, paint)
        // 驾驶舱
        paint.color = darken(color)
        canvas.drawRect(cx - w * 0.12f, cy - h * 0.3f, cx + w * 0.12f, cy, paint)
        // 引擎火焰
        if (isPlayer) {
            paint.color = 0xFFFF6600.toInt()
            canvas.drawRect(cx - w * 0.12f, cy + h * 0.4f, cx + w * 0.12f, cy + h * 0.55f, paint)
        }
    }

    private fun darken(color: Int): Int {
        val r = (Color.red(color) * 0.5f).toInt()
        val g = (Color.green(color) * 0.5f).toInt()
        val b = (Color.blue(color) * 0.5f).toInt()
        return Color.rgb(r, g, b)
    }

    // ==================== 敌机绘制（分类型精细绘制）====================

    private fun drawEnemies(canvas: Canvas, enemies: List<EnemyPlane>) {
        enemies.filter { it.isAlive }.forEach { enemy ->
            when (enemy) {
                is BasicEnemy     -> drawBasicEnemy(canvas, enemy)
                is ScoutEnemy     -> drawScoutEnemy(canvas, enemy)
                is FireEnemy      -> drawFireEnemy(canvas, enemy)
                is SuicideEnemy   -> drawSuicideEnemy(canvas, enemy)
                is BomberEnemy    -> drawBomberEnemy(canvas, enemy)
                is TorpedoEnemy   -> drawTorpedoEnemy(canvas, enemy)
                is FormationEnemy -> drawFormationEnemy(canvas, enemy)
                is JammerEnemy    -> drawJammerEnemy(canvas, enemy)
                is ShieldEnemy    -> drawShieldEnemy(canvas, enemy)
                is SpaceEnemy     -> drawSpaceEnemy(canvas, enemy)
                is StealthEnemy   -> drawStealthEnemy(canvas, enemy)
                else              -> drawGenericEnemy(canvas, enemy)
            }
            // 血量条（非满血时显示）
            if (enemy.hp < enemy.maxHp) drawHpBar(canvas, enemy.x, enemy.y - enemy.height / 2 - 6f,
                enemy.width, enemy.hp, enemy.maxHp)
        }
    }

    // ---- 基础敌机：倒三角+下视机翼+驾驶舱 ----
    private fun drawBasicEnemy(canvas: Canvas, e: BasicEnemy) {
        paint.style = Paint.Style.FILL
        val x = e.x; val y = e.y; val w = e.width; val h = e.height
        // 机身（倒三角锥形）
        paint.color = 0xFFCC2222.toInt()
        val body = Path().apply {
            moveTo(x, y + h * 0.48f)
            lineTo(x + w * 0.25f, y - h * 0.3f)
            lineTo(x - w * 0.25f, y - h * 0.3f)
            close()
        }
        canvas.drawPath(body, paint)
        // 机翼（向下展开）
        paint.color = 0xFFAA1111.toInt()
        val wingL = Path().apply {
            moveTo(x - w * 0.25f, y + h * 0.1f)
            lineTo(x - w * 0.52f, y + h * 0.45f)
            lineTo(x - w * 0.18f, y + h * 0.38f)
            close()
        }
        canvas.drawPath(wingL, paint)
        val wingR = Path().apply {
            moveTo(x + w * 0.25f, y + h * 0.1f)
            lineTo(x + w * 0.52f, y + h * 0.45f)
            lineTo(x + w * 0.18f, y + h * 0.38f)
            close()
        }
        canvas.drawPath(wingR, paint)
        // 驾驶舱
        paint.color = 0xFF550000.toInt()
        canvas.drawOval(x - w * 0.1f, y - h * 0.18f, x + w * 0.1f, y + h * 0.1f, paint)
        paint.color = 0x88FF8888.toInt()
        canvas.drawOval(x - w * 0.07f, y - h * 0.16f, x + w * 0.07f, y + h * 0.06f, paint)
        // 引擎（背部小发光）
        paint.color = 0x88FF4400.toInt()
        canvas.drawRect(x - w * 0.1f, y - h * 0.42f, x + w * 0.1f, y - h * 0.32f, paint)
    }

    // ---- 侦察机：扁平流线，Z字纹路 ----
    private fun drawScoutEnemy(canvas: Canvas, e: ScoutEnemy) {
        paint.style = Paint.Style.FILL
        val x = e.x; val y = e.y; val w = e.width; val h = e.height
        paint.color = 0xFFDD8800.toInt()
        val body = Path().apply {
            moveTo(x, y + h * 0.46f)
            lineTo(x + w * 0.22f, y - h * 0.25f)
            lineTo(x - w * 0.22f, y - h * 0.25f)
            close()
        }
        canvas.drawPath(body, paint)
        // Z字装饰条纹
        paint.color = 0xFF993300.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawLine(x - w * 0.15f, y + h * 0.1f, x, y - h * 0.1f, paint)
        canvas.drawLine(x, y - h * 0.1f, x + w * 0.15f, y + h * 0.1f, paint)
        paint.style = Paint.Style.FILL
        // 扁平小翼
        paint.color = 0xFFCC6600.toInt()
        val wing = Path().apply {
            moveTo(x - w * 0.22f, y); lineTo(x - w * 0.45f, y + h * 0.3f)
            lineTo(x - w * 0.16f, y + h * 0.38f); close()
        }
        canvas.drawPath(wing, paint)
        val wingR = Path().apply {
            moveTo(x + w * 0.22f, y); lineTo(x + w * 0.45f, y + h * 0.3f)
            lineTo(x + w * 0.16f, y + h * 0.38f); close()
        }
        canvas.drawPath(wingR, paint)
    }

    // ---- 火焰战机：棱角深红，侧面有橙红焰纹 ----
    private fun drawFireEnemy(canvas: Canvas, e: FireEnemy) {
        paint.style = Paint.Style.FILL
        val x = e.x; val y = e.y; val w = e.width; val h = e.height
        paint.color = 0xFFDD1100.toInt()
        val body = Path().apply {
            moveTo(x, y + h * 0.46f)
            lineTo(x + w * 0.28f, y + h * 0.05f)
            lineTo(x + w * 0.22f, y - h * 0.38f)
            lineTo(x - w * 0.22f, y - h * 0.38f)
            lineTo(x - w * 0.28f, y + h * 0.05f)
            close()
        }
        canvas.drawPath(body, paint)
        // 侧翼（锯齿边缘）
        paint.color = 0xFFAA0000.toInt()
        val wingL = Path().apply {
            moveTo(x - w * 0.28f, y - h * 0.1f)
            lineTo(x - w * 0.55f, y + h * 0.2f)
            lineTo(x - w * 0.35f, y + h * 0.35f)
            lineTo(x - w * 0.22f, y + h * 0.28f)
            close()
        }
        canvas.drawPath(wingL, paint)
        val wingR = Path().apply {
            moveTo(x + w * 0.28f, y - h * 0.1f)
            lineTo(x + w * 0.55f, y + h * 0.2f)
            lineTo(x + w * 0.35f, y + h * 0.35f)
            lineTo(x + w * 0.22f, y + h * 0.28f)
            close()
        }
        canvas.drawPath(wingR, paint)
        // 驾驶舱（黑色）
        paint.color = 0xFF330000.toInt()
        canvas.drawOval(x - w * 0.12f, y - h * 0.28f, x + w * 0.12f, y + h * 0.02f, paint)
        // 机身中线橙红装饰
        paint.color = 0xFFFF6600.toInt()
        canvas.drawRect(x - w * 0.03f, y - h * 0.36f, x + w * 0.03f, y + h * 0.3f, paint)
    }

    // ---- 自爆机：紧凑菱形炸弹状 ----
    private fun drawSuicideEnemy(canvas: Canvas, e: SuicideEnemy) {
        paint.style = Paint.Style.FILL
        val x = e.x; val y = e.y; val w = e.width; val h = e.height
        paint.color = 0xFFFF0000.toInt()
        val body = Path().apply {
            moveTo(x, y - h * 0.48f)
            lineTo(x + w * 0.35f, y)
            lineTo(x, y + h * 0.48f)
            lineTo(x - w * 0.35f, y)
            close()
        }
        canvas.drawPath(body, paint)
        // 十字准星线
        paint.color = 0xFFFFAAAA.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawLine(x - w * 0.25f, y, x + w * 0.25f, y, paint)
        canvas.drawLine(x, y - h * 0.3f, x, y + h * 0.3f, paint)
        paint.style = Paint.Style.FILL
        // 中心警告标记
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawRect(x - w * 0.08f, y - h * 0.08f, x + w * 0.08f, y + h * 0.08f, paint)
        paint.color = 0xFFFF0000.toInt()
        canvas.drawRect(x - w * 0.04f, y - h * 0.04f, x + w * 0.04f, y + h * 0.04f, paint)
    }

    // ---- 轰炸机：宽扁大型，双引擎舱 ----
    private fun drawBomberEnemy(canvas: Canvas, e: BomberEnemy) {
        paint.style = Paint.Style.FILL
        val x = e.x; val y = e.y; val w = e.width; val h = e.height
        // 宽扁主机身
        paint.color = 0xFF664400.toInt()
        val body = Path().apply {
            moveTo(x, y + h * 0.46f)
            lineTo(x + w * 0.35f, y + h * 0.1f)
            lineTo(x + w * 0.28f, y - h * 0.38f)
            lineTo(x - w * 0.28f, y - h * 0.38f)
            lineTo(x - w * 0.35f, y + h * 0.1f)
            close()
        }
        canvas.drawPath(body, paint)
        // 大型外伸机翼
        paint.color = 0xFF553300.toInt()
        val wingL = Path().apply {
            moveTo(x - w * 0.35f, y - h * 0.1f)
            lineTo(x - w * 0.58f, y + h * 0.35f)
            lineTo(x - w * 0.28f, y + h * 0.38f)
            close()
        }
        canvas.drawPath(wingL, paint)
        val wingR = Path().apply {
            moveTo(x + w * 0.35f, y - h * 0.1f)
            lineTo(x + w * 0.58f, y + h * 0.35f)
            lineTo(x + w * 0.28f, y + h * 0.38f)
            close()
        }
        canvas.drawPath(wingR, paint)
        // 双引擎舱（外凸鼓包）
        paint.color = 0xFF442200.toInt()
        canvas.drawRoundRect(x - w * 0.48f, y + h * 0.05f, x - w * 0.28f, y + h * 0.44f, 5f, 5f, paint)
        canvas.drawRoundRect(x + w * 0.28f, y + h * 0.05f, x + w * 0.48f, y + h * 0.44f, 5f, 5f, paint)
        // 驾驶舱
        paint.color = 0xFF331100.toInt()
        canvas.drawOval(x - w * 0.14f, y - h * 0.3f, x + w * 0.14f, y, paint)
        paint.color = 0x88FFAA44.toInt()
        canvas.drawOval(x - w * 0.1f, y - h * 0.28f, x + w * 0.1f, y - h * 0.08f, paint)
    }

    // ---- 鱼雷战机：修长鱼雷形，头部尖锐 ----
    private fun drawTorpedoEnemy(canvas: Canvas, e: TorpedoEnemy) {
        paint.style = Paint.Style.FILL
        val x = e.x; val y = e.y; val w = e.width; val h = e.height
        paint.color = 0xFF0066CC.toInt()
        val body = Path().apply {
            moveTo(x, y - h * 0.48f)
            lineTo(x + w * 0.28f, y - h * 0.1f)
            lineTo(x + w * 0.22f, y + h * 0.45f)
            lineTo(x - w * 0.22f, y + h * 0.45f)
            lineTo(x - w * 0.28f, y - h * 0.1f)
            close()
        }
        canvas.drawPath(body, paint)
        // 尾舵（横向宽展）
        paint.color = 0xFF004499.toInt()
        val tail = Path().apply {
            moveTo(x - w * 0.22f, y + h * 0.3f)
            lineTo(x - w * 0.5f, y + h * 0.48f)
            lineTo(x - w * 0.12f, y + h * 0.48f)
            lineTo(x + w * 0.12f, y + h * 0.48f)
            lineTo(x + w * 0.5f, y + h * 0.48f)
            lineTo(x + w * 0.22f, y + h * 0.3f)
            close()
        }
        canvas.drawPath(tail, paint)
        // 鱼雷头尖（银白）
        paint.color = 0xFFAADDFF.toInt()
        val nose = Path().apply {
            moveTo(x - w * 0.12f, y - h * 0.48f)
            lineTo(x, y - h * 0.58f)
            lineTo(x + w * 0.12f, y - h * 0.48f)
            close()
        }
        canvas.drawPath(nose, paint)
        // 驾驶舱
        paint.color = 0xFF002255.toInt()
        canvas.drawOval(x - w * 0.1f, y - h * 0.3f, x + w * 0.1f, y - h * 0.05f, paint)
    }

    // ---- 编队战机：紧凑菱形，编队编号装饰 ----
    private fun drawFormationEnemy(canvas: Canvas, e: FormationEnemy) {
        paint.style = Paint.Style.FILL
        val x = e.x; val y = e.y; val w = e.width; val h = e.height
        paint.color = 0xFF3388DD.toInt()
        val body = Path().apply {
            moveTo(x, y - h * 0.46f)
            lineTo(x + w * 0.3f, y + h * 0.1f)
            lineTo(x, y + h * 0.46f)
            lineTo(x - w * 0.3f, y + h * 0.1f)
            close()
        }
        canvas.drawPath(body, paint)
        // 翼尖小翼
        paint.color = 0xFF2266BB.toInt()
        val wingL = Path().apply {
            moveTo(x - w * 0.3f, y + h * 0.05f)
            lineTo(x - w * 0.48f, y + h * 0.32f)
            lineTo(x - w * 0.22f, y + h * 0.32f)
            close()
        }
        canvas.drawPath(wingL, paint)
        val wingR = Path().apply {
            moveTo(x + w * 0.3f, y + h * 0.05f)
            lineTo(x + w * 0.48f, y + h * 0.32f)
            lineTo(x + w * 0.22f, y + h * 0.32f)
            close()
        }
        canvas.drawPath(wingR, paint)
        // 白色编号点
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(x, y - h * 0.15f, w * 0.06f, paint)
        // 机头高光
        paint.color = 0x88AADDFF.toInt()
        canvas.drawOval(x - w * 0.05f, y - h * 0.44f, x + w * 0.05f, y - h * 0.28f, paint)
    }

    // ---- 干扰机：带雷达天线和波纹装饰 ----
    private fun drawJammerEnemy(canvas: Canvas, e: JammerEnemy) {
        paint.style = Paint.Style.FILL
        val x = e.x; val y = e.y; val w = e.width; val h = e.height
        // 六边形主机身
        paint.color = 0xFF22AA22.toInt()
        val body = Path().apply {
            moveTo(x, y - h * 0.46f)
            lineTo(x + w * 0.3f, y - h * 0.1f)
            lineTo(x + w * 0.25f, y + h * 0.42f)
            lineTo(x - w * 0.25f, y + h * 0.42f)
            lineTo(x - w * 0.3f, y - h * 0.1f)
            close()
        }
        canvas.drawPath(body, paint)
        // 顶部雷达天线
        paint.color = 0xFF118811.toInt()
        canvas.drawRect(x - w * 0.04f, y - h * 0.65f, x + w * 0.04f, y - h * 0.46f, paint)
        paint.color = 0xFF44FF44.toInt()
        canvas.drawCircle(x, y - h * 0.65f, w * 0.06f, paint)
        // 横向天线杆
        canvas.drawRect(x - w * 0.45f, y - h * 0.1f, x - w * 0.38f, y + h * 0.15f, paint)
        canvas.drawRect(x + w * 0.38f, y - h * 0.1f, x + w * 0.45f, y + h * 0.15f, paint)
        // 干扰波纹（3条弧线）
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = 0x8844FF44.toInt()
        canvas.drawCircle(x, y - h * 0.65f, w * 0.18f, paint)
        canvas.drawCircle(x, y - h * 0.65f, w * 0.28f, paint)
        canvas.drawCircle(x, y - h * 0.65f, w * 0.38f, paint)
        paint.style = Paint.Style.FILL
        // 驾驶舱
        paint.color = 0xFF114411.toInt()
        canvas.drawOval(x - w * 0.1f, y - h * 0.3f, x + w * 0.1f, y, paint)
    }

    // ---- 护盾战机：六边形棱角+双层护盾光环 ----
    private fun drawShieldEnemy(canvas: Canvas, e: ShieldEnemy) {
        paint.style = Paint.Style.FILL
        val x = e.x; val y = e.y; val w = e.width; val h = e.height
        paint.color = 0xFF7733CC.toInt()
        val body = Path().apply {
            moveTo(x, y - h * 0.46f)
            lineTo(x + w * 0.32f, y - h * 0.12f)
            lineTo(x + w * 0.28f, y + h * 0.42f)
            lineTo(x - w * 0.28f, y + h * 0.42f)
            lineTo(x - w * 0.32f, y - h * 0.12f)
            close()
        }
        canvas.drawPath(body, paint)
        // 机身边缘高光（六边形线条）
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = 0xFFAA66FF.toInt()
        canvas.drawPath(body, paint)
        paint.style = Paint.Style.FILL
        // 外层护盾（双层）
        if (e.shieldLayers >= 1) {
            paint.color = if (e.shieldLayers == 2) 0x509944FF.toInt() else 0x308844FF.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawCircle(x, y, w * 0.75f, paint)
            paint.style = Paint.Style.FILL
        }
        if (e.shieldLayers >= 2) {
            paint.color = 0x208844FF.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(x, y, w * 0.88f, paint)
            paint.style = Paint.Style.FILL
        }
        // 驾驶舱
        paint.color = 0xFF330066.toInt()
        canvas.drawOval(x - w * 0.11f, y - h * 0.3f, x + w * 0.11f, y + h * 0.05f, paint)
        paint.color = 0x88CC99FF.toInt()
        canvas.drawOval(x - w * 0.08f, y - h * 0.28f, x + w * 0.08f, y - h * 0.08f, paint)
    }

    // ---- 宇宙战机：飞碟造型，中心发光球+四条腿 ----
    private fun drawSpaceEnemy(canvas: Canvas, e: SpaceEnemy) {
        paint.style = Paint.Style.FILL
        val x = e.x; val y = e.y; val w = e.width; val h = e.height
        // 飞碟主体（椭圆）
        paint.color = 0xFF8800CC.toInt()
        canvas.drawOval(x - w * 0.42f, y - h * 0.18f, x + w * 0.42f, y + h * 0.3f, paint)
        // 驾驶舱球（发光）
        paint.color = 0xFF550099.toInt()
        canvas.drawCircle(x, y - h * 0.25f, w * 0.25f, paint)
        paint.color = 0xCCDD88FF.toInt()
        canvas.drawCircle(x, y - h * 0.28f, w * 0.16f, paint)
        // 四条腿
        paint.color = 0xFF550088.toInt()
        canvas.drawRect(x - w * 0.38f, y + h * 0.28f, x - w * 0.25f, y + h * 0.48f, paint)
        canvas.drawRect(x + w * 0.25f, y + h * 0.28f, x + w * 0.38f, y + h * 0.48f, paint)
        canvas.drawRect(x - w * 0.08f, y + h * 0.28f, x + w * 0.08f, y + h * 0.48f, paint)
        // 腿尖发光
        paint.color = 0xFFAA44FF.toInt()
        canvas.drawCircle(x - w * 0.32f, y + h * 0.48f, w * 0.06f, paint)
        canvas.drawCircle(x + w * 0.32f, y + h * 0.48f, w * 0.06f, paint)
        canvas.drawCircle(x, y + h * 0.48f, w * 0.06f, paint)
    }

    // ---- 隐形战机：倾斜钻石形，隐身时半透明 ----
    private fun drawStealthEnemy(canvas: Canvas, e: StealthEnemy) {
        paint.style = Paint.Style.FILL
        val x = e.x; val y = e.y; val w = e.width; val h = e.height
        val alpha = if (e.isStealthed) 0x44 else 0xCC
        // 倾斜菱形机身
        paint.color = (alpha shl 24) or 0x00FFFF
        val body = Path().apply {
            moveTo(x, y - h * 0.48f)
            lineTo(x + w * 0.35f, y + h * 0.1f)
            lineTo(x, y + h * 0.48f)
            lineTo(x - w * 0.35f, y + h * 0.1f)
            close()
        }
        canvas.drawPath(body, paint)
        // 机身线条
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = (alpha shl 24) or 0xAAFFFF
        canvas.drawPath(body, paint)
        paint.style = Paint.Style.FILL
        // 驾驶舱
        paint.color = (alpha shl 24) or 0x003333
        canvas.drawOval(x - w * 0.1f, y - h * 0.28f, x + w * 0.1f, y - h * 0.05f, paint)
        // 隐身效果：淡出边缘
        if (e.isStealthed) {
            paint.color = 0x2200FFFF.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(x, y, w * 0.55f, paint)
            paint.style = Paint.Style.FILL
        }
    }

    // ---- 兜底：未分类敌机 ----
    private fun drawGenericEnemy(canvas: Canvas, e: EnemyPlane) {
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFF4444.toInt()
        val cx = e.x; val cy = e.y; val w = e.width; val h = e.height
        canvas.drawRect(cx - w * 0.25f, cy - h * 0.45f, cx + w * 0.25f, cy + h * 0.45f, paint)
        paint.color = 0xFFCC2222.toInt()
        val wing = Path().apply {
            moveTo(cx - w * 0.5f, cy - h * 0.1f)
            lineTo(cx - w * 0.25f, cy - h * 0.3f)
            lineTo(cx - w * 0.25f, cy + h * 0.2f)
            close()
            moveTo(cx + w * 0.5f, cy - h * 0.1f)
            lineTo(cx + w * 0.25f, cy - h * 0.3f)
            lineTo(cx + w * 0.25f, cy + h * 0.2f)
            close()
        }
        canvas.drawPath(wing, paint)
    }

    // ==================== Boss 精细绘制 ====================

    private fun drawBoss(canvas: Canvas, boss: Boss) {
        if (!boss.isAlive) return
        val prevAlpha = paint.alpha
        if (boss.isInvincible) paint.alpha = 120

        when (boss) {
            is Boss1IronWing     -> drawBoss1IronWing(canvas, boss)
            is Boss2FlameKing    -> drawBoss2FlameKing(canvas, boss)
            is Boss3DeepLord     -> drawBoss3DeepLord(canvas, boss)
            is Boss4ThunderLord  -> drawBoss4ThunderLord(canvas, boss)
            is Boss5CosmicDestroyer -> drawBoss5CosmicDestroyer(canvas, boss)
            else                 -> drawGenericBoss(canvas, boss)
        }

        paint.alpha = prevAlpha
        drawBossHpBar(canvas, boss)

        // Boss分身（第5关）
        if (boss is Boss5CosmicDestroyer) {
            boss.activeClones.forEach { clone ->
                paint.alpha = 140
                drawBossClone(canvas, clone)
                paint.alpha = prevAlpha
            }
        }
    }

    // ---- Boss1 铁翼长老：大双翼战机+炮管 ----
    private fun drawBoss1IronWing(canvas: Canvas, b: Boss1IronWing) {
        paint.style = Paint.Style.FILL
        val x = b.x; val y = b.y; val w = b.width; val h = b.height
        // 主机身（矩形块状）
        paint.color = 0xFFCC4400.toInt()
        canvas.drawRect(x - w * 0.25f, y - h * 0.42f, x + w * 0.25f, y + h * 0.42f, paint)
        // 大型双翼
        paint.color = 0xFF993300.toInt()
        val wingL = Path().apply {
            moveTo(x - w * 0.25f, y - h * 0.1f)
            lineTo(x - w * 0.55f, y + h * 0.35f)
            lineTo(x - w * 0.38f, y + h * 0.42f)
            lineTo(x - w * 0.18f, y + h * 0.28f)
            close()
        }
        canvas.drawPath(wingL, paint)
        val wingR = Path().apply {
            moveTo(x + w * 0.25f, y - h * 0.1f)
            lineTo(x + w * 0.55f, y + h * 0.35f)
            lineTo(x + w * 0.38f, y + h * 0.42f)
            lineTo(x + w * 0.18f, y + h * 0.28f)
            close()
        }
        canvas.drawPath(wingR, paint)
        // 头部炮管（向下两根）
        paint.color = 0xFF773300.toInt()
        canvas.drawRoundRect(x - w * 0.18f, y + h * 0.38f, x - w * 0.06f, y + h * 0.55f, 3f, 3f, paint)
        canvas.drawRoundRect(x + w * 0.06f, y + h * 0.38f, x + w * 0.18f, y + h * 0.55f, 3f, 3f, paint)
        // 驾驶舱
        paint.color = 0xFF551100.toInt()
        canvas.drawOval(x - w * 0.14f, y - h * 0.32f, x + w * 0.14f, y - h * 0.05f, paint)
        paint.color = 0xAAFF9966.toInt()
        canvas.drawOval(x - w * 0.1f, y - h * 0.3f, x + w * 0.1f, y - h * 0.1f, paint)
    }

    // ---- Boss2 烈焰魔王：顶部火焰王冠+双角 ----
    private fun drawBoss2FlameKing(canvas: Canvas, b: Boss2FlameKing) {
        paint.style = Paint.Style.FILL
        val x = b.x; val y = b.y; val w = b.width; val h = b.height
        // 厚重主机身
        paint.color = 0xFFDD1100.toInt()
        canvas.drawRect(x - w * 0.3f, y - h * 0.38f, x + w * 0.3f, y + h * 0.42f, paint)
        // 火焰王冠（顶部两尖刺）
        paint.color = 0xFFFF4400.toInt()
        val crownL = Path().apply {
            moveTo(x - w * 0.28f, y - h * 0.38f)
            lineTo(x - w * 0.18f, y - h * 0.58f)
            lineTo(x - w * 0.08f, y - h * 0.38f)
            close()
        }
        canvas.drawPath(crownL, paint)
        val crownR = Path().apply {
            moveTo(x + w * 0.08f, y - h * 0.38f)
            lineTo(x + w * 0.18f, y - h * 0.58f)
            lineTo(x + w * 0.28f, y - h * 0.38f)
            close()
        }
        canvas.drawPath(crownR, paint)
        // 侧面机翼
        paint.color = 0xFFAA0000.toInt()
        val wingL = Path().apply {
            moveTo(x - w * 0.3f, y - h * 0.1f)
            lineTo(x - w * 0.58f, y + h * 0.3f)
            lineTo(x - w * 0.22f, y + h * 0.42f)
            close()
        }
        canvas.drawPath(wingL, paint)
        val wingR = Path().apply {
            moveTo(x + w * 0.3f, y - h * 0.1f)
            lineTo(x + w * 0.58f, y + h * 0.3f)
            lineTo(x + w * 0.22f, y + h * 0.42f)
            close()
        }
        canvas.drawPath(wingR, paint)
        // 装甲条（生命低时显示受损）
        if (b.hasArmor) {
            paint.color = 0xFF885500.toInt()
            canvas.drawRect(x - w * 0.32f, y - h * 0.05f, x + w * 0.32f, y + h * 0.12f, paint)
            paint.color = 0xFFBB8800.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(x - w * 0.32f, y - h * 0.05f, x + w * 0.32f, y + h * 0.12f, paint)
            paint.style = Paint.Style.FILL
        }
        // 驾驶舱
        paint.color = 0xFF330000.toInt()
        canvas.drawOval(x - w * 0.14f, y - h * 0.28f, x + w * 0.14f, y, paint)
        paint.color = 0xAAFF6633.toInt()
        canvas.drawOval(x - w * 0.1f, y - h * 0.26f, x + w * 0.1f, y - h * 0.08f, paint)
    }

    // ---- Boss3 深海主宰：鲸鱼流线型+鳍状突出 ----
    private fun drawBoss3DeepLord(canvas: Canvas, b: Boss3DeepLord) {
        paint.style = Paint.Style.FILL
        val x = b.x; val y = b.y; val w = b.width; val h = b.height
        // 流线主身躯（椭圆形）
        paint.color = 0xFF0033CC.toInt()
        canvas.drawOval(x - w * 0.35f, y - h * 0.4f, x + w * 0.35f, y + h * 0.45f, paint)
        // 背鳍
        paint.color = 0xFF002299.toInt()
        val fin = Path().apply {
            moveTo(x - w * 0.12f, y - h * 0.4f)
            lineTo(x, y - h * 0.62f)
            lineTo(x + w * 0.12f, y - h * 0.4f)
            close()
        }
        canvas.drawPath(fin, paint)
        // 两侧胸鳍
        val pL = Path().apply {
            moveTo(x - w * 0.35f, y)
            lineTo(x - w * 0.52f, y + h * 0.3f)
            lineTo(x - w * 0.22f, y + h * 0.25f)
            close()
        }
        canvas.drawPath(pL, paint)
        val pR = Path().apply {
            moveTo(x + w * 0.35f, y)
            lineTo(x + w * 0.52f, y + h * 0.3f)
            lineTo(x + w * 0.22f, y + h * 0.25f)
            close()
        }
        canvas.drawPath(pR, paint)
        // 眼睛（深海中发光的）
        paint.color = 0xFF88FFFF.toInt()
        canvas.drawCircle(x - w * 0.14f, y - h * 0.15f, w * 0.06f, paint)
        canvas.drawCircle(x + w * 0.14f, y - h * 0.15f, w * 0.06f, paint)
        paint.color = 0xFF001133.toInt()
        canvas.drawCircle(x - w * 0.14f, y - h * 0.15f, w * 0.03f, paint)
        canvas.drawCircle(x + w * 0.14f, y - h * 0.15f, w * 0.03f, paint)
    }

    // ---- Boss4 雷霆支配者：三叉闪电形 ----
    private fun drawBoss4ThunderLord(canvas: Canvas, b: Boss4ThunderLord) {
        paint.style = Paint.Style.FILL
        val x = b.x; val y = b.y; val w = b.width; val h = b.height
        // 中心主体（椭圆）
        paint.color = 0xFF22BB22.toInt()
        canvas.drawOval(x - w * 0.28f, y - h * 0.38f, x + w * 0.28f, y + h * 0.42f, paint)
        // 三叉闪电：左右各一个分叉
        paint.color = 0xFF118811.toInt()
        val叉L = Path().apply {
            moveTo(x - w * 0.28f, y - h * 0.1f)
            lineTo(x - w * 0.52f, y - h * 0.45f)
            lineTo(x - w * 0.38f, y - h * 0.38f)
            close()
        }
        canvas.drawPath(叉L, paint)
        val叉R = Path().apply {
            moveTo(x + w * 0.28f, y - h * 0.1f)
            lineTo(x + w * 0.52f, y - h * 0.45f)
            lineTo(x + w * 0.38f, y - h * 0.38f)
            close()
        }
        canvas.drawPath(叉R, paint)
        // 闪电线条（黄色高光）
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = 0xFFFFFF00.toInt()
        canvas.drawLine(x - w * 0.5f, y - h * 0.1f, x, y + h * 0.1f, paint)
        canvas.drawLine(x, y + h * 0.1f, x + w * 0.5f, y - h * 0.1f, paint)
        paint.style = Paint.Style.FILL
        // 核心能量球
        paint.color = 0xFF66FF66.toInt()
        canvas.drawCircle(x, y, w * 0.15f, paint)
        paint.color = 0xFFFFFFAA.toInt()
        canvas.drawCircle(x, y, w * 0.08f, paint)
    }

    // ---- Boss5 宇宙毁灭者：飞船造型+多炮口 ----
    private fun drawBoss5CosmicDestroyer(canvas: Canvas, b: Boss5CosmicDestroyer) {
        paint.style = Paint.Style.FILL
        val x = b.x; val y = b.y; val w = b.width; val h = b.height
        // 主船体（宽扁矩形）
        paint.color = 0xFF8800CC.toInt()
        canvas.drawRect(x - w * 0.38f, y - h * 0.35f, x + w * 0.38f, y + h * 0.42f, paint)
        // 驾驶舱（发光球）
        paint.color = 0xFF550099.toInt()
        canvas.drawCircle(x, y - h * 0.32f, w * 0.22f, paint)
        paint.color = 0xCCDD88FF.toInt()
        canvas.drawCircle(x, y - h * 0.35f, w * 0.14f, paint)
        // 四个炮口（圆形）
        paint.color = 0xFFFF4400.toInt()
        canvas.drawCircle(x - w * 0.22f, y + h * 0.35f, w * 0.07f, paint)
        canvas.drawCircle(x - w * 0.06f, y + h * 0.38f, w * 0.07f, paint)
        canvas.drawCircle(x + w * 0.06f, y + h * 0.38f, w * 0.07f, paint)
        canvas.drawCircle(x + w * 0.22f, y + h * 0.35f, w * 0.07f, paint)
        // 机身边缘线条
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = 0xFFBB44FF.toInt()
        canvas.drawRect(x - w * 0.38f, y - h * 0.35f, x + w * 0.38f, y + h * 0.42f, paint)
        paint.style = Paint.Style.FILL
        // 能量条装饰（根据phase变化颜色）
        paint.color = when {
            b.phase == 3 -> 0xFFFF0066.toInt()
            b.phase == 2 -> 0xFFFF8800.toInt()
            else -> 0xFF8800FF.toInt()
        }
        canvas.drawRect(x - w * 0.3f, y - h * 0.05f, x + w * 0.3f, y + h * 0.12f, paint)
    }

    // ---- Boss兜底 ----
    private fun drawGenericBoss(canvas: Canvas, b: Boss) {
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFF0000.toInt()
        canvas.drawRect(b.x - b.width * 0.3f, b.y - b.height * 0.4f, b.x + b.width * 0.3f, b.y + b.height * 0.4f, paint)
    }

    // ---- Boss5分身 ----
    private fun drawBossClone(canvas: Canvas, c: BossClone) {
        paint.style = Paint.Style.FILL
        paint.color = 0xAA8800CC.toInt()
        canvas.drawOval(c.x - c.width * 0.38f, c.y - c.height * 0.35f, c.x + c.width * 0.38f, c.y + c.height * 0.42f, paint)
        paint.color = 0xCCDD88FF.toInt()
        canvas.drawCircle(c.x, c.y - c.height * 0.28f, c.width * 0.18f, paint)
    }

    // ==================== 辅助绘制函数 ====================

    private fun drawBossHpBar(canvas: Canvas, boss: Boss) {
        val barW = GameConfig.LOGICAL_WIDTH - 40f
        val barH = 10f
        val bx = 20f; val by = 14f
        paint.color = 0xFF333333.toInt()
        canvas.drawRect(bx, by, bx + barW, by + barH, paint)
        val pct = boss.hp.toFloat() / boss.maxHp
        paint.color = when {
            pct > 0.6f -> 0xFF00CC44.toInt()
            pct > 0.3f -> 0xFFFFAA00.toInt()
            else -> 0xFFFF2222.toInt()
        }
        canvas.drawRect(bx, by, bx + barW * pct, by + barH, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRect(bx, by, bx + barW, by + barH, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textSize = 9f
        canvas.drawText(boss.bossName, GameConfig.LOGICAL_WIDTH / 2, by - 2f, textPaint)
    }

    // ==================== 子弹 ====================
    private fun drawBullets(canvas: Canvas, bullets: List<Bullet>) {
        bullets.filter { it.isAlive }.forEach { bullet ->
            val (coreColor, glowColor) = if (bullet.isPlayerBullet) {
                when (bullet.type) {
                    BulletType.PLAYER_SINGLE -> Pair(0xFFFFFF00.toInt(), 0x88FFFF00.toInt())
                    BulletType.PLAYER_SPREAD -> Pair(0xFFFFAA00.toInt(), 0x88FFAA00.toInt())
                    BulletType.PLAYER_MISSILE,
                    BulletType.PLAYER_MISSILE_STORM -> Pair(0xFF00FFFF.toInt(), 0x8800FFFF.toInt())
                    BulletType.PLAYER_CHARGED -> Pair(0xFFFFFFFF.toInt(), 0x88FFFFFF.toInt())
                    BulletType.PLAYER_TORNADO -> Pair(0xFFFF88FF.toInt(), 0x88FF88FF.toInt())
                    else -> Pair(0xFFFFFF00.toInt(), 0x88FFFF00.toInt())
                }
            } else {
                when (bullet.type) {
                    BulletType.ENEMY_ARC -> Pair(0xFFFF6600.toInt(), 0x88FF6600.toInt())
                    BulletType.ENEMY_RING -> Pair(0xFFFF00FF.toInt(), 0x88FF00FF.toInt())
                    BulletType.ENEMY_TRACKING -> Pair(0xFFFFAA00.toInt(), 0x88FFAA00.toInt())
                    BulletType.ENEMY_BOUNCE -> Pair(0xFF00FF88.toInt(), 0x8800FF88.toInt())
                    else -> Pair(0xFFFF4444.toInt(), 0x88FF4444.toInt())
                }
            }
            
            // 追踪导弹稍大
            val r = if (bullet.type == BulletType.PLAYER_MISSILE ||
                bullet.type == BulletType.PLAYER_MISSILE_STORM ||
                bullet.type == BulletType.PLAYER_CHARGED) 6f else 4f
            
            // 发光效果（外层）
            paint.color = glowColor
            paint.style = Paint.Style.FILL
            canvas.drawCircle(bullet.x, bullet.y, r + 3f, paint)
            
            // 核心（内层）
            paint.color = coreColor
            canvas.drawRect(bullet.x - r / 2, bullet.y - r, bullet.x + r / 2, bullet.y + r, paint)
            
            // 白色中心点（增强对比度）
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(bullet.x, bullet.y, r / 3f, paint)
        }
    }

    // ==================== 道具（增大尺寸 + 图标化绘制）====================
    private fun drawItems(canvas: Canvas, items: List<Item>) {
        items.filter { it.isAlive }.forEach { item ->
            val r = 18f  // 增大道具尺寸（原来12f）
            val x = item.x; val y = item.y

            when (item.itemType) {
                ItemType.HEALTH_SMALL -> {
                    // 红色圆角背景 + 十字
                    paint.color = 0xFFCC2222.toInt()
                    paint.style = Paint.Style.FILL
                    canvas.drawRoundRect(x - r, y - r, x + r, y + r, 5f, 5f, paint)
                    paint.color = 0xFFFF6666.toInt()
                    canvas.drawRoundRect(x - r + 1f, y - r + 1f, x + r - 1f, y - r + 4f, 2f, 2f, paint)
                    paint.color = 0xFFFFFFFF.toInt()
                    canvas.drawRect(x - r * 0.55f, y - 4f, x + r * 0.55f, y + 4f, paint)
                    canvas.drawRect(x - 4f, y - r * 0.55f, x + 4f, y + r * 0.55f, paint)
                    // 外发光
                    paint.color = 0x44FF4444.toInt()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    canvas.drawRoundRect(x - r - 2f, y - r - 2f, x + r + 2f, y + r + 2f, 7f, 7f, paint)
                    paint.style = Paint.Style.FILL
                }
                ItemType.HEALTH_LARGE -> {
                    // 深红心形背景
                    paint.color = 0xFFAA0000.toInt()
                    canvas.drawRoundRect(x - r, y - r, x + r, y + r, 5f, 5f, paint)
                    paint.color = 0xFFFF2244.toInt()
                    // 心形简化：两圆 + 三角
                    canvas.drawCircle(x - 5f, y - 4f, 6f, paint)
                    canvas.drawCircle(x + 5f, y - 4f, 6f, paint)
                    val heartPath = Path().apply {
                        moveTo(x - 11f, y - 2f)
                        lineTo(x, y + 10f)
                        lineTo(x + 11f, y - 2f)
                        close()
                    }
                    canvas.drawPath(heartPath, paint)
                    paint.color = 0x44FF2244.toInt()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    canvas.drawRoundRect(x - r - 2f, y - r - 2f, x + r + 2f, y + r + 2f, 7f, 7f, paint)
                    paint.style = Paint.Style.FILL
                }
                ItemType.BOMB -> {
                    // 橙色背景 + 炸弹图标
                    paint.color = 0xFFCC4400.toInt()
                    canvas.drawRoundRect(x - r, y - r, x + r, y + r, 5f, 5f, paint)
                    paint.color = 0xFF222222.toInt()
                    canvas.drawCircle(x, y + 3f, 10f, paint)
                    // 导线
                    paint.color = 0xFF888888.toInt()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    canvas.drawLine(x, y - 7f, x + 4f, y - 13f, paint)
                    paint.style = Paint.Style.FILL
                    // 火花
                    paint.color = 0xFFFFAA00.toInt()
                    canvas.drawCircle(x + 5f, y - 14f, 3f, paint)
                    // 光效
                    paint.color = 0x44FF8800.toInt()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    canvas.drawRoundRect(x - r - 2f, y - r - 2f, x + r + 2f, y + r + 2f, 7f, 7f, paint)
                    paint.style = Paint.Style.FILL
                }
                ItemType.SHIELD -> {
                    // 蓝色背景 + 盾形
                    paint.color = 0xFF004488.toInt()
                    canvas.drawRoundRect(x - r, y - r, x + r, y + r, 5f, 5f, paint)
                    paint.color = 0xFF2288FF.toInt()
                    val shieldPath = Path().apply {
                        moveTo(x, y - r * 0.8f)
                        lineTo(x + r * 0.7f, y - r * 0.4f)
                        lineTo(x + r * 0.7f, y + r * 0.1f)
                        quadTo(x + r * 0.7f, y + r * 0.8f, x, y + r * 0.9f)
                        quadTo(x - r * 0.7f, y + r * 0.8f, x - r * 0.7f, y + r * 0.1f)
                        lineTo(x - r * 0.7f, y - r * 0.4f)
                        close()
                    }
                    canvas.drawPath(shieldPath, paint)
                    paint.color = 0xFFAADDFF.toInt()
                    textPaint.color = 0xFFFFFFFF.toInt()
                    textPaint.textSize = 10f
                    textPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText("S", x, y + 4f, textPaint)
                    paint.color = 0x440088FF.toInt()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    canvas.drawRoundRect(x - r - 2f, y - r - 2f, x + r + 2f, y + r + 2f, 7f, 7f, paint)
                    paint.style = Paint.Style.FILL
                }
                ItemType.POWER_UP -> {
                    // 黄色背景 + 闪电
                    paint.color = 0xFF886600.toInt()
                    canvas.drawRoundRect(x - r, y - r, x + r, y + r, 5f, 5f, paint)
                    paint.color = 0xFFFFDD00.toInt()
                    val boltPath = Path().apply {
                        moveTo(x + 4f, y - r * 0.8f)
                        lineTo(x - 3f, y + 1f)
                        lineTo(x + 2f, y + 1f)
                        lineTo(x - 4f, y + r * 0.8f)
                        lineTo(x + 5f, y - 1f)
                        lineTo(x, y - 1f)
                        close()
                    }
                    canvas.drawPath(boltPath, paint)
                    paint.color = 0x44FFDD00.toInt()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    canvas.drawRoundRect(x - r - 2f, y - r - 2f, x + r + 2f, y + r + 2f, 7f, 7f, paint)
                    paint.style = Paint.Style.FILL
                }
                ItemType.DOUBLE_SHOT -> {
                    // 紫色背景 + 双箭头
                    paint.color = 0xFF550088.toInt()
                    canvas.drawRoundRect(x - r, y - r, x + r, y + r, 5f, 5f, paint)
                    paint.color = 0xFFDD44FF.toInt()
                    // 左子弹
                    canvas.drawRect(x - 8f, y - r * 0.7f, x - 4f, y + r * 0.5f, paint)
                    val tip1 = Path().apply { moveTo(x - 8f, y - r * 0.7f); lineTo(x - 6f, y - r * 0.95f); lineTo(x - 4f, y - r * 0.7f); close() }
                    canvas.drawPath(tip1, paint)
                    // 右子弹
                    canvas.drawRect(x + 4f, y - r * 0.7f, x + 8f, y + r * 0.5f, paint)
                    val tip2 = Path().apply { moveTo(x + 4f, y - r * 0.7f); lineTo(x + 6f, y - r * 0.95f); lineTo(x + 8f, y - r * 0.7f); close() }
                    canvas.drawPath(tip2, paint)
                    // x2标签
                    textPaint.textSize = 8f
                    textPaint.color = 0xFFFFFFFF.toInt()
                    textPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText("x2", x, y + r * 0.75f, textPaint)
                    paint.color = 0x44AA00FF.toInt()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    canvas.drawRoundRect(x - r - 2f, y - r - 2f, x + r + 2f, y + r + 2f, 7f, 7f, paint)
                    paint.style = Paint.Style.FILL
                }
            }
        }
    }

    // ==================== 血量条 ====================
    private fun drawHpBar(canvas: Canvas, cx: Float, top: Float, width: Float, hp: Int, maxHp: Int) {
        val barW = width * 0.8f
        val barH = 3f
        val left = cx - barW / 2
        paint.color = 0xFF333333.toInt()
        canvas.drawRect(left, top, left + barW, top + barH, paint)
        paint.color = 0xFF00FF44.toInt()
        canvas.drawRect(left, top, left + barW * (hp.toFloat() / maxHp), top + barH, paint)
    }

    // ==================== 玩家血量条（屏幕顶部）====================
    private fun drawPlayerHpBar(canvas: Canvas, player: PlayerPlane) {
        val barW = 120f
        val barH = 10f
        val bx = 10f
        val by = 25f
        
        // 背景
        paint.color = 0xFF333333.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(bx, by, bx + barW, by + barH, paint)
        
        // 血量
        val pct = player.hp.toFloat() / player.maxHp
        paint.color = when {
            pct > 0.6f -> 0xFF00FF44.toInt()
            pct > 0.3f -> 0xFFFFAA00.toInt()
            else -> 0xFFFF2222.toInt()
        }
        canvas.drawRect(bx, by, bx + barW * pct, by + barH, paint)
        
        // 边框
        paint.color = 0xFFFFFFFF.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(bx, by, bx + barW, by + barH, paint)
        paint.style = Paint.Style.FILL
        
        // 文字
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textSize = 10f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("HP: ${player.hp}/${player.maxHp}", bx, by - 4f, textPaint)
        
        // 飞机图标
        paint.color = when (player.planeType) {
            PlaneType.FALCON -> 0xFF00FF88.toInt()
            PlaneType.STORM  -> 0xFFFFAA00.toInt()
            PlaneType.HUNTERII -> 0xFF00AAFF.toInt()
        }
        canvas.drawRect(bx + barW + 8f, by, bx + barW + 18f, by + barH, paint)
    }
}
