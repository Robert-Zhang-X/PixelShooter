package com.pixelshooter.game.entities.bosses

import com.pixelshooter.game.engine.*
import com.pixelshooter.game.entities.enemies.BasicEnemy
import kotlin.math.*

// ==================== Boss 基类 ====================

abstract class Boss(
    x: Float, y: Float, width: Float, height: Float,
    val bossName: String
) : GameEntity(x, y, width, height) {

    var phase = 1
    var attackTimer = 0L
    abstract val attackInterval: Long
    abstract val enterY: Float  // Boss 入场停止Y坐标

    private var isEntering = true
    private val enterSpeed = 2f

    override fun update(deltaMs: Long) {
        if (isEntering) {
            y += enterSpeed
            if (y >= enterY) { y = enterY; isEntering = false }
        }
        attackTimer += deltaMs
        updatePhase()
        onUpdate(deltaMs)
    }

    private fun updatePhase() {
        val pct = hp.toFloat() / maxHp
        phase = when {
            pct > 0.6f -> 1
            pct > 0.3f -> 2
            else -> 3
        }
    }

    abstract fun onUpdate(deltaMs: Long)
    abstract fun fire(playerX: Float, playerY: Float): List<Bullet>
    abstract fun spawnMinions(): List<com.pixelshooter.game.entities.enemies.EnemyPlane>
}

// ==================== Boss 1 — 铁翼长老 ====================

class Boss1IronWing(x: Float) : Boss(x, -60f, 70f, 60f, "铁翼长老") {
    override val attackInterval = 2000L
    override val enterY = 100f

    init { hp = 800; maxHp = 800 }

    override fun onUpdate(deltaMs: Long) {}

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (attackTimer < attackInterval) return emptyList()
        attackTimer = 0
        val bullets = mutableListOf<Bullet>()
        // 横向扫射排列弹幕
        val cols = if (phase == 3) 9 else 5
        for (i in 0 until cols) {
            val bx = 30f + i * (GameConfig.LOGICAL_WIDTH - 60f) / (cols - 1)
            bullets.add(Bullet(bx, y + height / 2, 0f, GameConfig.BULLET_ENEMY_SPEED,
                15, BulletType.ENEMY_STRAIGHT, false))
        }
        return bullets
    }

    override fun spawnMinions(): List<com.pixelshooter.game.entities.enemies.EnemyPlane> {
        if (hp.toFloat() / maxHp < 0.5f && attackTimer == 0L) {
            return listOf(
                BasicEnemy(x - 50f, y),
                BasicEnemy(x + 50f, y)
            )
        }
        return emptyList()
    }
}

// ==================== Boss 2 — 烈焰魔王 ====================

class Boss2FlameKing(x: Float) : Boss(x, -70f, 80f, 68f, "烈焰魔王") {
    override val attackInterval = 2200L
    override val enterY = 120f
    private var armorHp = 200
    val hasArmor get() = armorHp > 0

    init { hp = 1500; maxHp = 1500 }

    override fun onUpdate(deltaMs: Long) {}

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (attackTimer < attackInterval) return emptyList()
        attackTimer = 0
        val bullets = mutableListOf<Bullet>()
        return when (phase) {
            1 -> {
                // 3道弧形火焰弹幕
                for (row in 0 until 3) {
                    for (i in -2..2) {
                        val vx = i * 1.5f
                        val vy = GameConfig.BULLET_ENEMY_SPEED + row
                        bullets.add(Bullet(x, y + height / 2, vx, vy,
                            20, BulletType.ENEMY_ARC, false))
                    }
                }
                bullets
            }
            2 -> {
                // 火焰追踪弹 x4
                for (i in 0 until 4) {
                    val dx = playerX - x + (i - 1.5f) * 20f
                    val dy = playerY - y
                    val dist = sqrt(dx * dx + dy * dy)
                    val speed = GameConfig.BULLET_ENEMY_SPEED + 1f
                    bullets.add(Bullet(x, y + height / 2,
                        dx / dist * speed, dy / dist * speed,
                        25, BulletType.ENEMY_TRACKING, false))
                }
                bullets
            }
            else -> {
                // 全屏火柱（大范围直线弹）
                for (i in 0 until 7) {
                    val bx = 25f + i * 52f
                    bullets.add(Bullet(bx, y + height, 0f, GameConfig.BULLET_ENEMY_SPEED + 2f,
                        30, BulletType.ENEMY_STRAIGHT, false))
                }
                bullets
            }
        }
    }

    fun hitArmor(damage: Int) {
        armorHp -= damage
    }

    override fun spawnMinions() = emptyList<com.pixelshooter.game.entities.enemies.EnemyPlane>()
}

// ==================== Boss 3 — 深海主宰 ====================

class Boss3DeepLord(x: Float) : Boss(x, -80f, 90f, 76f, "深海主宰") {
    override val attackInterval = 2500L
    override val enterY = 130f

    init { hp = 2200; maxHp = 2200 }

    var isDiving = false
    private var diveTimer = 0L
    private val diveCycle = 15000L

    override fun onUpdate(deltaMs: Long) {
        diveTimer += deltaMs
        isDiving = diveTimer % diveCycle < 2000L
        if (isDiving) isInvincible = true
        else if (isInvincible && diveTimer % diveCycle >= 2000L) {
            isInvincible = false
            // 随机出现位置
            x = (40f + Math.random() * (GameConfig.LOGICAL_WIDTH - 80f)).toFloat()
        }
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (attackTimer < attackInterval || isDiving) return emptyList()
        attackTimer = 0
        val bullets = mutableListOf<Bullet>()
        return when (phase) {
            1 -> {
                // 360° 环形弹幕
                for (i in 0 until 12) {
                    val a = i * 30.0
                    val rad = Math.toRadians(a)
                    val s = GameConfig.BULLET_ENEMY_SPEED
                    bullets.add(Bullet(x, y, (sin(rad) * s).toFloat(), (cos(rad) * s).toFloat(),
                        18, BulletType.ENEMY_RING, false))
                }
                bullets
            }
            2 -> {
                // 从屏幕下方升起水柱
                for (i in 0 until 4) {
                    val bx = 45f + i * 90f
                    bullets.add(Bullet(bx, GameConfig.LOGICAL_HEIGHT + 10f, 0f, -8f,
                        20, BulletType.ENEMY_STRAIGHT, false))
                }
                bullets
            }
            else -> {
                // 旋转弹幕 + 追踪鱼雷
                for (i in 0 until 16) {
                    val a = i * 22.5
                    val rad = Math.toRadians(a)
                    val s = GameConfig.BULLET_ENEMY_SPEED + 1f
                    bullets.add(Bullet(x, y, (sin(rad) * s).toFloat(), (cos(rad) * s).toFloat(),
                        15, BulletType.ENEMY_RING, false))
                }
                for (i in 0 until 3) {
                    val dx = playerX - x + (i - 1) * 30f
                    val dy = playerY - y
                    val dist = sqrt(dx * dx + dy * dy)
                    val s = GameConfig.BULLET_ENEMY_SPEED + 2f
                    bullets.add(Bullet(x, y, dx / dist * s, dy / dist * s,
                        25, BulletType.ENEMY_TRACKING, false))
                }
                bullets
            }
        }
    }

    override fun spawnMinions() = emptyList<com.pixelshooter.game.entities.enemies.EnemyPlane>()
}

// ==================== Boss 4 — 雷霆支配者 ====================

class Boss4ThunderLord(x: Float) : Boss(x, -80f, 85f, 72f, "雷霆支配者") {
    override val attackInterval = 1800L
    override val enterY = 110f

    init { hp = 3000; maxHp = 3000 }

    private var moveTimer = 0L
    private var moveDir = 1f

    override fun onUpdate(deltaMs: Long) {
        // Boss 快速横移
        moveTimer += deltaMs
        x += moveDir * 3f
        if (x > GameConfig.LOGICAL_WIDTH - 60f || x < 60f) moveDir = -moveDir
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (attackTimer < attackInterval) return emptyList()
        attackTimer = 0
        val bullets = mutableListOf<Bullet>()
        return when (phase) {
            1 -> {
                // 3道随机位置闪电柱（大伤直线弹）
                for (i in 0 until 3) {
                    val bx = 30f + Math.random().toFloat() * (GameConfig.LOGICAL_WIDTH - 60f)
                    bullets.add(Bullet(bx, y + height / 2, 0f, GameConfig.BULLET_ENEMY_SPEED + 3f,
                        30, BulletType.ENEMY_STRAIGHT, false))
                }
                bullets
            }
            2 -> {
                // 蜘蛛网弹幕（反弹弹）
                for (i in -3..3) {
                    bullets.add(Bullet(x, y + height / 2, i * 1.5f, GameConfig.BULLET_ENEMY_SPEED,
                        20, BulletType.ENEMY_BOUNCE, false))
                }
                bullets
            }
            else -> {
                // 全方位高密度弹幕
                for (i in 0 until 20) {
                    val a = i * 18.0
                    val rad = Math.toRadians(a)
                    val s = GameConfig.BULLET_ENEMY_SPEED + 2f
                    bullets.add(Bullet(x, y, (sin(rad) * s).toFloat(), (cos(rad) * s).toFloat(),
                        25, BulletType.ENEMY_RING, false))
                }
                bullets
            }
        }
    }

    override fun spawnMinions() = emptyList<com.pixelshooter.game.entities.enemies.EnemyPlane>()
}

// ==================== Boss 5 — 宇宙毁灭者（三阶段）====================

class Boss5CosmicDestroyer(x: Float) : Boss(x, -100f, 100f, 88f, "宇宙毁灭者") {
    override val attackInterval = 1500L
    override val enterY = 140f

    init { hp = 5000; maxHp = 5000 }

    private var moveTimer = 0L
    private var moveDir = 1f
    private var clones = mutableListOf<BossClone>()
    val activeClones: List<BossClone> get() = clones.filter { it.isAlive }

    override fun onUpdate(deltaMs: Long) {
        moveTimer += deltaMs
        when (phase) {
            1 -> { /* 静止 */ }
            2 -> {
                // 阶段2 加速横移
                x += moveDir * 4f
                if (x > GameConfig.LOGICAL_WIDTH - 70f || x < 70f) moveDir = -moveDir
            }
            3 -> {
                // 阶段3 狂暴横移
                x += moveDir * 6f
                if (x > GameConfig.LOGICAL_WIDTH - 70f || x < 70f) moveDir = -moveDir
                // 生成分身
                if (clones.isEmpty()) {
                    clones.add(BossClone(x - 80f, y))
                    clones.add(BossClone(x + 80f, y))
                }
                clones.forEach { it.update(deltaMs) }
            }
        }
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (attackTimer < attackInterval) return emptyList()
        attackTimer = 0
        val bullets = mutableListOf<Bullet>()
        when (phase) {
            1 -> {
                // 追踪导弹 x3 + 扇形弹幕
                for (i in 0 until 3) {
                    val dx = playerX - x + (i - 1) * 25f
                    val dy = playerY - y
                    val dist = sqrt(dx * dx + dy * dy)
                    val s = GameConfig.BULLET_ENEMY_SPEED + 1f
                    bullets.add(Bullet(x, y + height / 2,
                        dx / dist * s, dy / dist * s, 30, BulletType.ENEMY_TRACKING, false))
                }
                for (i in -3..3) {
                    bullets.add(Bullet(x, y + height / 2, i * 1.5f, GameConfig.BULLET_ENEMY_SPEED,
                        20, BulletType.ENEMY_FAN, false))
                }
            }
            2 -> {
                // 密集弹幕迷宫（留空隙）
                for (i in 0 until 24) {
                    val a = i * 15.0
                    val rad = Math.toRadians(a)
                    val s = GameConfig.BULLET_ENEMY_SPEED + 2f
                    if (i % 6 == 2 || i % 6 == 5) continue  // 留空隙
                    bullets.add(Bullet(x, y, (sin(rad) * s).toFloat(), (cos(rad) * s).toFloat(),
                        25, BulletType.ENEMY_RING, false))
                }
            }
            3 -> {
                // 收缩弹幕 + 分身协同攻击
                for (i in 0 until 36) {
                    val a = i * 10.0
                    val rad = Math.toRadians(a)
                    val s = GameConfig.BULLET_ENEMY_SPEED + 3f
                    if (i % 9 == 4) continue  // 极小安全区
                    bullets.add(Bullet(x, y, (sin(rad) * s).toFloat(), (cos(rad) * s).toFloat(),
                        30, BulletType.ENEMY_RING, false))
                }
            }
        }
        return bullets
    }

    override fun spawnMinions(): List<com.pixelshooter.game.entities.enemies.EnemyPlane> = emptyList()
}

/** Boss5 分身 */
class BossClone(x: Float, y: Float) : GameEntity(x, y, 60f, 50f) {
    init { hp = 500; maxHp = 500 }
    private var moveDir = if (x < GameConfig.LOGICAL_WIDTH / 2) -1f else 1f

    override fun update(deltaMs: Long) {
        x += moveDir * 3f
        if (x < 50f || x > GameConfig.LOGICAL_WIDTH - 50f) moveDir = -moveDir
    }
}
