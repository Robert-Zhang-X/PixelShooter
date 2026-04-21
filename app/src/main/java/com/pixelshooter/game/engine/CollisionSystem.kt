package com.pixelshooter.game.engine

import android.graphics.RectF

/**
 * AABB 碰撞检测系统
 */
object CollisionSystem {

    /**
     * 检测两个矩形是否相交
     */
    fun isColliding(a: RectF, b: RectF): Boolean {
        return a.left < b.right &&
               a.right > b.left &&
               a.top < b.bottom &&
               a.bottom > b.top
    }

    /**
     * 缩小碰撞盒（避免像素边缘误判），默认缩小20%
     */
    fun shrinkRect(rect: RectF, factor: Float = 0.8f): RectF {
        val dx = rect.width() * (1f - factor) / 2f
        val dy = rect.height() * (1f - factor) / 2f
        return RectF(rect.left + dx, rect.top + dy, rect.right - dx, rect.bottom - dy)
    }
}
