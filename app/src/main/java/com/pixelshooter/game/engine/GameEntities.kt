package com.pixelshooter.game.engine

import android.graphics.RectF
import android.graphics.PointF

// ==================== 基础实体 ====================

abstract class GameEntity(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float
) {
    var isAlive: Boolean = true
    var hp: Int = 1
    var maxHp: Int = 1

    val bounds: RectF
        get() = RectF(x - width / 2, y - height / 2, x + width / 2, y + height / 2)

    val collisionBounds: RectF
        get() = CollisionSystem.shrinkRect(bounds)

    abstract fun update(deltaMs: Long)
}

// ==================== 子弹类型 ====================

enum class BulletType {
    PLAYER_SINGLE,      // 单发穿透
    PLAYER_SPREAD,      // 散射（5发）
    PLAYER_MISSILE,     // 追踪导弹
    PLAYER_CHARGED,     // 蓄力超级炮
    PLAYER_TORNADO,     // 旋风360°
    PLAYER_MISSILE_STORM, // 导弹风暴
    ENEMY_STRAIGHT,     // 敌机直线弹
    ENEMY_FAN,          // 敌机扇形弹
    ENEMY_TRACKING,     // 敌机追踪弹
    ENEMY_ARC,          // 弧形弹
    ENEMY_RING,         // 环形弹
    ENEMY_BOUNCE,       // 反弹弹（蜘蛛网）
}

class Bullet(
    x: Float, y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Int,
    val type: BulletType,
    val isPlayerBullet: Boolean,
    val isPenetrating: Boolean = false,  // 穿透
    var trackTarget: GameEntity? = null  // 追踪目标
) : GameEntity(x, y, 8f, 14f) {

    private val trackSpeed = 6f

    override fun update(deltaMs: Long) {
        if (!isAlive) return

        if (trackTarget != null && trackTarget!!.isAlive) {
            // 追踪逻辑
            val dx = trackTarget!!.x - x
            val dy = trackTarget!!.y - y
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist > 1f) {
                x += (dx / dist) * trackSpeed
                y += (dy / dist) * trackSpeed
            }
        } else {
            x += vx
            y += vy
        }

        // 超出屏幕则消失
        if (x < -50 || x > GameConfig.LOGICAL_WIDTH + 50 ||
            y < -100 || y > GameConfig.LOGICAL_HEIGHT + 100) {
            isAlive = false
        }
    }
}

// ==================== 道具类型 ====================

enum class ItemType {
    HEALTH_SMALL,    // 血包小
    HEALTH_LARGE,    // 血包大
    BOMB,            // 炸弹
    SHIELD,          // 护盾
    POWER_UP,        // 强化子弹
    DOUBLE_SHOT,     // 双倍弹药
}

class Item(
    x: Float, y: Float,
    val itemType: ItemType
) : GameEntity(x, y, 24f, 24f) {

    override fun update(deltaMs: Long) {
        y += GameConfig.ITEM_SPEED
        if (y > GameConfig.LOGICAL_HEIGHT + 50) isAlive = false
    }
}
