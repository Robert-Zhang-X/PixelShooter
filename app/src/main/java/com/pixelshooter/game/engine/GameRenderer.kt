package com.pixelshooter.game.engine

import android.graphics.*
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
        canvas.save()
        canvas.translate(offsetX, 0f)
        canvas.scale(scaleX, scaleY)

        drawBackground(canvas, engine, bgOffset)
        drawItems(canvas, engine.items)
        drawEnemies(canvas, engine.enemies)
        engine.boss?.let { drawBoss(canvas, it) }
        drawBullets(canvas, engine.bullets)
        drawPlayer(canvas, engine.player)

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
        // 闪烁效果（无敌期）
        if (player.isInvincible && (System.currentTimeMillis() / 100) % 2 == 0L) return

        val x = player.x; val y = player.y
        val w = player.width; val h = player.height

        // 护盾效果
        if (player.shieldTimeMs > 0) {
            paint.color = 0x4400BFFF
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, w * 0.8f, paint)
            paint.color = 0xFF00BFFF.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(x, y, w * 0.8f, paint)
            paint.style = Paint.Style.FILL
        }

        // 机身颜色
        val bodyColor = when (player.planeType) {
            PlaneType.FALCON -> 0xFF00FF88.toInt()
            PlaneType.STORM  -> 0xFFFFAA00.toInt()
            PlaneType.HUNTERII -> 0xFF00AAFF.toInt()
        }
        drawPixelPlane(canvas, x, y, w, h, bodyColor, isPlayer = true)

        // 强化buff光效
        if (player.powerUpTimeMs > 0) {
            paint.color = 0x88FFFF00.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawRect(x - w / 2, y - h / 2, x + w / 2, y + h / 2, paint)
            paint.style = Paint.Style.FILL
        }
    }

    // ==================== 通用像素飞机绘制 ====================
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
        // 驾驶舱（深色小矩形）
        paint.color = darken(color)
        canvas.drawRect(cx - w * 0.12f, cy - h * 0.3f, cx + w * 0.12f, cy, paint)
        // 引擎火焰（玩家飞机下方）
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

    // ==================== 敌机 ====================
    private fun drawEnemies(canvas: Canvas, enemies: List<EnemyPlane>) {
        enemies.filter { it.isAlive }.forEach { enemy ->
            val color = when (enemy) {
                is BasicEnemy -> 0xFFFF4444.toInt()
                is ScoutEnemy -> 0xFFFFAA44.toInt()
                is FireEnemy  -> 0xFFFF2200.toInt()
                is SuicideEnemy -> 0xFFFF0000.toInt()
                is BomberEnemy -> 0xFF884400.toInt()
                is TorpedoEnemy -> 0xFF0088FF.toInt()
                is FormationEnemy -> 0xFF44AAFF.toInt()
                is JammerEnemy -> 0xFF44FF44.toInt()
                is ShieldEnemy -> 0xFF8844FF.toInt()
                is SpaceEnemy -> 0xFFAA00FF.toInt()
                is StealthEnemy -> if ((enemy as StealthEnemy).isStealthed) 0x44FFFFFF else 0xFF00FFFF.toInt()
                else -> 0xFFFF4444.toInt()
            }
            drawPixelPlane(canvas, enemy.x, enemy.y, enemy.width, enemy.height, color, isPlayer = false)

            // 护盾战机的护盾圆圈
            if (enemy is ShieldEnemy && enemy.shieldLayers > 0) {
                paint.color = if (enemy.shieldLayers == 2) 0x888844FF.toInt() else 0x448844FF.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawCircle(enemy.x, enemy.y, enemy.width * 0.7f, paint)
                paint.style = Paint.Style.FILL
            }

            // 血量条（非满血时显示）
            if (enemy.hp < enemy.maxHp) drawHpBar(canvas, enemy.x, enemy.y - enemy.height / 2 - 6f,
                enemy.width, enemy.hp, enemy.maxHp)
        }
    }

    // ==================== Boss ====================
    private fun drawBoss(canvas: Canvas, boss: Boss) {
        if (!boss.isAlive) return
        val color = when (boss) {
            is Boss1IronWing -> 0xFFCC4400.toInt()
            is Boss2FlameKing -> 0xFFFF2200.toInt()
            is Boss3DeepLord -> 0xFF0044FF.toInt()
            is Boss4ThunderLord -> 0xFF44FF44.toInt()
            is Boss5CosmicDestroyer -> 0xFFCC00FF.toInt()
            else -> 0xFFFF0000.toInt()
        }

        // 无敌时半透明
        if (boss.isInvincible) {
            paint.alpha = 120
        }

        drawPixelPlane(canvas, boss.x, boss.y, boss.width, boss.height, color, isPlayer = false)
        paint.alpha = 255

        // Boss血量条
        drawBossHpBar(canvas, boss)

        // Boss分身（第5关）
        if (boss is Boss5CosmicDestroyer) {
            boss.activeClones.forEach { clone ->
                drawPixelPlane(canvas, clone.x, clone.y, clone.width, clone.height,
                    0x88CC00FF.toInt(), isPlayer = false)
            }
        }
    }

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
            paint.color = if (bullet.isPlayerBullet) {
                when (bullet.type) {
                    BulletType.PLAYER_SINGLE -> 0xFFFFFF00.toInt()
                    BulletType.PLAYER_SPREAD -> 0xFFFFAA00.toInt()
                    BulletType.PLAYER_MISSILE,
                    BulletType.PLAYER_MISSILE_STORM -> 0xFF00FFFF.toInt()
                    BulletType.PLAYER_CHARGED -> 0xFFFFFFFF.toInt()
                    BulletType.PLAYER_TORNADO -> 0xFFFF88FF.toInt()
                    else -> 0xFFFFFF00.toInt()
                }
            } else {
                when (bullet.type) {
                    BulletType.ENEMY_ARC -> 0xFFFF4400.toInt()
                    BulletType.ENEMY_RING -> 0xFFFF00FF.toInt()
                    BulletType.ENEMY_TRACKING -> 0xFFFF8800.toInt()
                    BulletType.ENEMY_BOUNCE -> 0xFF00FF88.toInt()
                    else -> 0xFFFF2222.toInt()
                }
            }
            // 追踪导弹稍大
            val r = if (bullet.type == BulletType.PLAYER_MISSILE ||
                bullet.type == BulletType.PLAYER_MISSILE_STORM ||
                bullet.type == BulletType.PLAYER_CHARGED) 6f else 4f
            canvas.drawRect(bullet.x - r / 2, bullet.y - r, bullet.x + r / 2, bullet.y + r, paint)
        }
    }

    // ==================== 道具 ====================
    private fun drawItems(canvas: Canvas, items: List<Item>) {
        items.filter { it.isAlive }.forEach { item ->
            val (color, label) = when (item.itemType) {
                ItemType.HEALTH_SMALL -> Pair(0xFFFF4444.toInt(), "+")
                ItemType.HEALTH_LARGE -> Pair(0xFFFF0000.toInt(), "++")
                ItemType.BOMB         -> Pair(0xFFFF8800.toInt(), "B")
                ItemType.SHIELD       -> Pair(0xFF0088FF.toInt(), "S")
                ItemType.POWER_UP     -> Pair(0xFFFFFF00.toInt(), "P")
                ItemType.DOUBLE_SHOT  -> Pair(0xFFAA00FF.toInt(), "x2")
            }
            paint.color = color
            canvas.drawRect(item.x - 12f, item.y - 12f, item.x + 12f, item.y + 12f, paint)
            textPaint.color = 0xFFFFFFFF.toInt()
            textPaint.textSize = 10f
            canvas.drawText(label, item.x, item.y + 4f, textPaint)
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
}
