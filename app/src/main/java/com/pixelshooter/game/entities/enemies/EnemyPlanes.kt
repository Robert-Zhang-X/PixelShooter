package com.pixelshooter.game.entities.enemies

import com.pixelshooter.game.engine.*
import kotlin.math.*

// ==================== 敌机基类 ====================

abstract class EnemyPlane(
    x: Float, y: Float, width: Float, height: Float,
    val scoreValue: Int,
    val dropChance: Float = GameConfig.ITEM_DROP_CHANCE
) : GameEntity(x, y, width, height) {

    protected var fireTimer: Long = 0
    abstract val fireInterval: Long

    abstract fun fire(playerX: Float, playerY: Float): List<Bullet>

    protected fun angleToPlayer(playerX: Float, playerY: Float): Double {
        return atan2((playerX - x).toDouble(), (y - playerY).toDouble())
    }
}

// ==================== 关卡1 敌机 ====================

/** 杂兵战机：直线单发 */
class BasicEnemy(x: Float, y: Float) : EnemyPlane(x, y, 30f, 28f, 100) {
    override val fireInterval = 2000L
    init { hp = 30; maxHp = 30 }

    private var vy = 2.5f

    override fun update(deltaMs: Long) {
        y += vy
        fireTimer += deltaMs
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (fireTimer < fireInterval) return emptyList()
        fireTimer = 0
        return listOf(Bullet(x, y + height / 2, 0f, GameConfig.BULLET_ENEMY_SPEED,
            10, BulletType.ENEMY_STRAIGHT, false))
    }
}

/** 侦察机：高速飞过，不攻击 */
class ScoutEnemy(x: Float, y: Float) : EnemyPlane(x, y, 25f, 22f, 50, 0.05f) {
    override val fireInterval = Long.MAX_VALUE
    init { hp = 20; maxHp = 20 }

    private val vy = 5.5f

    override fun update(deltaMs: Long) {
        y += vy
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }

    override fun fire(playerX: Float, playerY: Float) = emptyList<Bullet>()
}

// ==================== 关卡2 敌机 ====================

/** 火焰战机：扇形火弹 */
class FireEnemy(x: Float, y: Float) : EnemyPlane(x, y, 34f, 32f, 150) {
    override val fireInterval = 2500L
    init { hp = 50; maxHp = 50 }

    private var vy = 2f

    override fun update(deltaMs: Long) {
        y += vy
        fireTimer += deltaMs
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (fireTimer < fireInterval) return emptyList()
        fireTimer = 0
        val bullets = mutableListOf<Bullet>()
        for (angle in listOf(-25f, 0f, 25f)) {
            val rad = Math.toRadians(angle.toDouble())
            val vx = (sin(rad) * GameConfig.BULLET_ENEMY_SPEED).toFloat()
            val vy = (cos(rad) * GameConfig.BULLET_ENEMY_SPEED).toFloat()
            bullets.add(Bullet(x, y + height / 2, vx, vy, 15, BulletType.ENEMY_FAN, false))
        }
        return bullets
    }
}

/** 自爆飞机：靠近玩家爆炸 */
class SuicideEnemy(x: Float, y: Float) : EnemyPlane(x, y, 30f, 30f, 200, 0.25f) {
    override val fireInterval = Long.MAX_VALUE
    init { hp = 80; maxHp = 80 }

    private var speed = 3f
    var explodeDamage = 60

    override fun update(deltaMs: Long) {
        y += speed
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }

    override fun fire(playerX: Float, playerY: Float) = emptyList<Bullet>()
}

/** 轰炸机：投落炸弹 */
class BomberEnemy(x: Float, y: Float) : EnemyPlane(x, y, 50f, 40f, 300, 0.3f) {
    override val fireInterval = 3000L
    init { hp = 120; maxHp = 120 }

    private var vy = 1.5f

    override fun update(deltaMs: Long) {
        y += vy
        fireTimer += deltaMs
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (fireTimer < fireInterval) return emptyList()
        fireTimer = 0
        return listOf(Bullet(x, y + height / 2, 0f, 6f, 25, BulletType.ENEMY_STRAIGHT, false))
    }
}

// ==================== 关卡3 敌机 ====================

/** 鱼雷战机：Z字形飞行 */
class TorpedoEnemy(x: Float, y: Float) : EnemyPlane(x, y, 32f, 30f, 200) {
    override val fireInterval = 2200L
    init { hp = 60; maxHp = 60 }

    private var vy = 2f
    private var vx = 2f
    private var timer = 0L

    override fun update(deltaMs: Long) {
        timer += deltaMs
        if (timer % 1200 < 600) x += vx else x -= vx
        y += vy
        fireTimer += deltaMs
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (fireTimer < fireInterval) return emptyList()
        fireTimer = 0
        val angle = angleToPlayer(playerX, playerY)
        return listOf(Bullet(x, y + height / 2,
            (sin(angle) * GameConfig.BULLET_ENEMY_SPEED).toFloat(),
            (cos(angle) * GameConfig.BULLET_ENEMY_SPEED).toFloat(),
            18, BulletType.ENEMY_TRACKING, false))
    }
}

/** 编队战机：三角阵同步射击 */
class FormationEnemy(x: Float, y: Float) : EnemyPlane(x, y, 28f, 26f, 150) {
    override val fireInterval = 1800L
    init { hp = 40; maxHp = 40 }

    private var vy = 2.5f

    override fun update(deltaMs: Long) {
        y += vy
        fireTimer += deltaMs
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (fireTimer < fireInterval) return emptyList()
        fireTimer = 0
        return listOf(Bullet(x, y + height / 2, 0f, GameConfig.BULLET_ENEMY_SPEED,
            12, BulletType.ENEMY_STRAIGHT, false))
    }
}

// ==================== 关卡4 敌机 ====================

/** 电磁干扰机：发射减速子弹 */
class JammerEnemy(x: Float, y: Float) : EnemyPlane(x, y, 32f, 30f, 200) {
    override val fireInterval = 3000L
    init { hp = 80; maxHp = 80 }

    private var vy = 1.8f

    override fun update(deltaMs: Long) {
        y += vy
        fireTimer += deltaMs
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (fireTimer < fireInterval) return emptyList()
        fireTimer = 0
        // 减速弹（游戏引擎需判断此类型子弹命中时附加减速效果）
        return listOf(Bullet(x, y + height / 2, 0f, GameConfig.BULLET_ENEMY_SPEED + 1f,
            5, BulletType.ENEMY_STRAIGHT, false))
    }
}

/** 护盾战机：自带双层护盾 */
class ShieldEnemy(x: Float, y: Float) : EnemyPlane(x, y, 35f, 33f, 300) {
    override val fireInterval = 2000L
    init { hp = 150; maxHp = 150 }

    var shieldLayers = 2
    private var vy = 1.5f

    fun hitShield(): Boolean {
        if (shieldLayers > 0) { shieldLayers--; return true }
        return false
    }

    override fun update(deltaMs: Long) {
        y += vy
        fireTimer += deltaMs
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (fireTimer < fireInterval) return emptyList()
        fireTimer = 0
        return listOf(Bullet(x, y + height / 2, 0f, GameConfig.BULLET_ENEMY_SPEED,
            20, BulletType.ENEMY_STRAIGHT, false))
    }
}

// ==================== 关卡5 敌机 ====================

/** 宇宙战机：螺旋弹道 */
class SpaceEnemy(x: Float, y: Float) : EnemyPlane(x, y, 35f, 32f, 250) {
    override val fireInterval = 2000L
    init { hp = 100; maxHp = 100 }

    private var vy = 2f
    private var spiralAngle = 0f

    override fun update(deltaMs: Long) {
        y += vy
        spiralAngle += 3f
        fireTimer += deltaMs
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (fireTimer < fireInterval) return emptyList()
        fireTimer = 0
        val bullets = mutableListOf<Bullet>()
        for (i in 0 until 4) {
            val a = spiralAngle + i * 90f
            val rad = Math.toRadians(a.toDouble())
            val speed = GameConfig.BULLET_ENEMY_SPEED
            bullets.add(Bullet(x, y, (sin(rad) * speed).toFloat(), (cos(rad) * speed).toFloat(),
                20, BulletType.ENEMY_RING, false))
        }
        return bullets
    }
}

/** 时空战机：短暂隐身后突袭 */
class StealthEnemy(x: Float, y: Float) : EnemyPlane(x, y, 32f, 30f, 300) {
    override val fireInterval = 2500L
    init { hp = 120; maxHp = 120 }

    private var vy = 2f
    private var stealthTimer = 0L
    var isStealthed = false

    override fun update(deltaMs: Long) {
        y += vy
        stealthTimer += deltaMs
        // 每5秒隐身1.5秒
        isStealthed = stealthTimer % 5000 < 1500
        fireTimer += deltaMs
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }

    override fun fire(playerX: Float, playerY: Float): List<Bullet> {
        if (fireTimer < fireInterval || isStealthed) return emptyList()
        fireTimer = 0
        val angle = angleToPlayer(playerX, playerY)
        val speed = GameConfig.BULLET_ENEMY_SPEED + 2f
        return listOf(Bullet(x, y + height / 2,
            (sin(angle) * speed).toFloat(), (cos(angle) * speed).toFloat(),
            25, BulletType.ENEMY_TRACKING, false))
    }
}
