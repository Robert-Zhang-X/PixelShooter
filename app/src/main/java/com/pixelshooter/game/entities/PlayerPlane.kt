package com.pixelshooter.game.entities

import com.pixelshooter.game.engine.*
import kotlin.math.*

// ==================== 玩家飞机基类 ====================

enum class PlaneType { FALCON, STORM, HUNTERII }

abstract class PlayerPlane(
    x: Float, y: Float,
    val planeType: PlaneType
) : GameEntity(x, y, GameConfig.PLAYER_WIDTH, GameConfig.PLAYER_HEIGHT) {

    abstract val maxSpeed: Float
    abstract val fireInterval: Long  // 射击间隔(ms)
    abstract val name: String
    abstract val description: String

    var score: Int = 0
    var bombs: Int = 3          // 炸弹数量
    var shieldTimeMs: Long = 0  // 护盾剩余时间
    var powerUpTimeMs: Long = 0 // 强化子弹剩余时间
    var doubleShotTimeMs: Long = 0

    var isInvincible: Boolean = false
    var invincibleTimeMs: Long = 0

    private var lastFireTime: Long = 0
    private var chargeStartTime: Long = -1L
    var isCharging: Boolean = false

    // 特技冷却
    var skillCooldown: Long = 0
    val skillMaxCooldown: Long = 8000L

    override fun update(deltaMs: Long) {
        // 更新无敌时间
        if (isInvincible && invincibleTimeMs > 0) {
            invincibleTimeMs -= deltaMs
            if (invincibleTimeMs <= 0) isInvincible = false
        }
        // 更新道具buff
        if (shieldTimeMs > 0) shieldTimeMs -= deltaMs
        if (powerUpTimeMs > 0) powerUpTimeMs -= deltaMs
        if (doubleShotTimeMs > 0) doubleShotTimeMs -= deltaMs
        // 技能冷却
        if (skillCooldown > 0) skillCooldown -= deltaMs

        // 限制在屏幕内
        x = x.coerceIn(width / 2, GameConfig.LOGICAL_WIDTH - width / 2)
        y = y.coerceIn(height / 2, GameConfig.LOGICAL_HEIGHT - height / 2)
    }

    fun tryFire(currentTimeMs: Long): List<Bullet> {
        if (currentTimeMs - lastFireTime < fireInterval) return emptyList()
        lastFireTime = currentTimeMs
        return createBullets()
    }

    fun takeDamage(damage: Int) {
        if (isInvincible || shieldTimeMs > 0) return
        hp -= damage
        if (hp <= 0) { hp = 0; isAlive = false }
        else {
            isInvincible = true
            invincibleTimeMs = GameConfig.PLAYER_INVINCIBLE_MS
        }
    }

    abstract fun createBullets(): List<Bullet>
    abstract fun useSkill(): List<Bullet>

    fun applyItem(item: Item) {
        when (item.itemType) {
            ItemType.HEALTH_SMALL -> hp = min(hp + 30, maxHp)
            ItemType.HEALTH_LARGE -> hp = maxHp
            ItemType.BOMB -> bombs++
            ItemType.SHIELD -> shieldTimeMs = 5000L
            ItemType.POWER_UP -> powerUpTimeMs = 10000L
            ItemType.DOUBLE_SHOT -> doubleShotTimeMs = 8000L
        }
        score += GameConfig.SCORE_ITEM_PICK
    }
}

// ==================== 猎鹰号（单发高伤）====================

class FalconPlane(x: Float, y: Float) : PlayerPlane(x, y, PlaneType.FALCON) {
    override val maxSpeed = 7f
    override val fireInterval = 300L
    override val name = "猎鹰号"
    override val description = "单发穿透弹，伤害极高"

    init { hp = 150; maxHp = 150 }

    override fun createBullets(): List<Bullet> {
        val dmg = if (powerUpTimeMs > 0) 45 else 30
        val bullets = mutableListOf<Bullet>()
        bullets.add(Bullet(x, y - height / 2 - 10, 0f, -GameConfig.BULLET_PLAYER_SPEED,
            dmg, BulletType.PLAYER_SINGLE, true, isPenetrating = true))
        if (doubleShotTimeMs > 0) {
            bullets.add(Bullet(x - 12f, y - height / 2 - 10, 0f, -GameConfig.BULLET_PLAYER_SPEED,
                dmg, BulletType.PLAYER_SINGLE, true, isPenetrating = true))
            bullets.add(Bullet(x + 12f, y - height / 2 - 10, 0f, -GameConfig.BULLET_PLAYER_SPEED,
                dmg, BulletType.PLAYER_SINGLE, true, isPenetrating = true))
        }
        return bullets
    }

    // 技能：蓄力超级炮
    override fun useSkill(): List<Bullet> {
        if (skillCooldown > 0) return emptyList()
        skillCooldown = skillMaxCooldown
        return listOf(Bullet(x, y - height / 2 - 10, 0f, -GameConfig.BULLET_PLAYER_SPEED * 1.5f,
            150, BulletType.PLAYER_CHARGED, true, isPenetrating = true))
    }
}

// ==================== 风暴号（多发散射）====================

class StormPlane(x: Float, y: Float) : PlayerPlane(x, y, PlaneType.STORM) {
    override val maxSpeed = 9f
    override val fireInterval = 200L
    override val name = "风暴号"
    override val description = "扇形5发散射，覆盖范围广"

    init { hp = 120; maxHp = 120 }

    override fun createBullets(): List<Bullet> {
        val dmg = if (powerUpTimeMs > 0) 18 else 12
        val bullets = mutableListOf<Bullet>()
        val angles = listOf(-40f, -20f, 0f, 20f, 40f)
        val count = if (doubleShotTimeMs > 0) angles + listOf(-60f, 60f) else angles
        for (angle in count) {
            val rad = Math.toRadians(angle.toDouble())
            val vx = (sin(rad) * GameConfig.BULLET_PLAYER_SPEED).toFloat()
            val vy = -(cos(rad) * GameConfig.BULLET_PLAYER_SPEED).toFloat()
            bullets.add(Bullet(x, y - height / 2, vx, vy, dmg, BulletType.PLAYER_SPREAD, true))
        }
        return bullets
    }

    // 技能：旋风模式（360° 散射）
    override fun useSkill(): List<Bullet> {
        if (skillCooldown > 0) return emptyList()
        skillCooldown = skillMaxCooldown
        val bullets = mutableListOf<Bullet>()
        for (i in 0 until 24) {
            val angle = i * 15.0
            val rad = Math.toRadians(angle)
            val vx = (sin(rad) * GameConfig.BULLET_PLAYER_SPEED).toFloat()
            val vy = (cos(rad) * GameConfig.BULLET_PLAYER_SPEED).toFloat()
            bullets.add(Bullet(x, y, vx, vy, 15, BulletType.PLAYER_TORNADO, true))
        }
        return bullets
    }
}

// ==================== 猎鹰II号（追踪导弹）====================

class HunterIIPlane(x: Float, y: Float) : PlayerPlane(x, y, PlaneType.HUNTERII) {
    override val maxSpeed = 10f
    override val fireInterval = 400L
    override val name = "猎鹰II号"
    override val description = "自动追踪导弹，锁定最近敌人"

    init { hp = 100; maxHp = 100 }

    var nearestEnemy: GameEntity? = null

    override fun createBullets(): List<Bullet> {
        val dmg = if (powerUpTimeMs > 0) 30 else 20
        val count = if (doubleShotTimeMs > 0) 4 else 2
        val offsets = listOf(-10f, 10f, -20f, 20f)
        return (0 until count).map { i ->
            Bullet(x + offsets[i], y - height / 2, 0f, -GameConfig.BULLET_PLAYER_SPEED,
                dmg, BulletType.PLAYER_MISSILE, true, trackTarget = nearestEnemy)
        }
    }

    // 技能：导弹风暴
    override fun useSkill(): List<Bullet> {
        if (skillCooldown > 0) return emptyList()
        skillCooldown = skillMaxCooldown
        val dmg = 25
        return (0 until 8).map { i ->
            val offset = (i - 3.5f) * 12f
            Bullet(x + offset, y - height / 2, 0f, -GameConfig.BULLET_PLAYER_SPEED,
                dmg, BulletType.PLAYER_MISSILE_STORM, true, trackTarget = nearestEnemy)
        }
    }
}
