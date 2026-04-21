package com.pixelshooter.game.engine

// 游戏常量配置
object GameConfig {
    // 屏幕逻辑尺寸（以此为基准做坐标计算）
    const val LOGICAL_WIDTH = 360f
    const val LOGICAL_HEIGHT = 720f

    // 游戏帧率
    const val TARGET_FPS = 60
    const val FRAME_TIME_MS = 1000L / TARGET_FPS

    // 玩家飞机
    const val PLAYER_WIDTH = 40f
    const val PLAYER_HEIGHT = 50f
    const val PLAYER_INVINCIBLE_MS = 2000L  // 受伤后无敌时间

    // 子弹
    const val BULLET_PLAYER_SPEED = 12f
    const val BULLET_ENEMY_SPEED = 5f

    // 道具
    const val ITEM_SPEED = 3f
    const val ITEM_DROP_CHANCE = 0.15f  // 15% 掉落概率

    // 得分
    const val SCORE_ENEMY_KILL = 100
    const val SCORE_BOSS_KILL = 5000
    const val SCORE_ITEM_PICK = 50
}
