package com.pixelshooter.game.levels

import com.pixelshooter.game.engine.GameEntity
import com.pixelshooter.game.entities.enemies.*

// ==================== 关卡数据结构 ====================

data class EnemyWave(
    val delayMs: Long,                    // 距上一波的延迟时间
    val enemies: List<com.pixelshooter.game.entities.enemies.EnemyPlane>  // 本波敌机列表
)

data class LevelConfig(
    val levelId: Int,
    val levelName: String,
    val backgroundColorTop: Int,           // 背景渐变色（上）
    val backgroundColorBottom: Int,        // 背景渐变色（下）
    val bgScrollSpeed: Float,              // 背景滚动速度
    val waves: List<EnemyWave>,            // 敌机波次
    val bossHpOverride: Int? = null        // 可选：覆盖Boss血量
)

// ==================== 关卡工厂 ====================

object LevelFactory {

    fun createLevel(levelId: Int): LevelConfig = when (levelId) {
        1 -> level1()
        2 -> level2()
        3 -> level3()
        4 -> level4()
        5 -> level5()
        else -> level1()
    }

    private val W = com.pixelshooter.game.engine.GameConfig.LOGICAL_WIDTH
    private val step get() = W / 6f

    // ===== 第1关：蓝天边境 =====
    private fun level1(): LevelConfig {
        val waves = mutableListOf<EnemyWave>()
        // 第1波：5架杂兵，从上方匀速飞过
        waves.add(EnemyWave(0, listOf(
            BasicEnemy(step * 1, -30f), BasicEnemy(step * 2, -30f),
            BasicEnemy(step * 3, -30f), BasicEnemy(step * 4, -30f),
            BasicEnemy(step * 5, -30f)
        )))
        // 第2波：3架侦察机
        waves.add(EnemyWave(3000, listOf(
            ScoutEnemy(step * 1.5f, -30f), ScoutEnemy(step * 3f, -30f),
            ScoutEnemy(step * 4.5f, -30f)
        )))
        // 第3波：5架杂兵 + 2架侦察
        waves.add(EnemyWave(4000, listOf(
            BasicEnemy(step * 1, -30f), BasicEnemy(step * 2, -50f),
            BasicEnemy(step * 3, -30f), BasicEnemy(step * 4, -50f),
            BasicEnemy(step * 5, -30f),
            ScoutEnemy(step * 1.5f, -80f), ScoutEnemy(step * 4.5f, -80f)
        )))
        return LevelConfig(1, "蓝天边境",
            0xFF87CEEB.toInt(), 0xFF4169E1.toInt(), 1.5f, waves)
    }

    // ===== 第2关：火山峡谷 =====
    private fun level2(): LevelConfig {
        val waves = mutableListOf<EnemyWave>()
        waves.add(EnemyWave(0, listOf(
            FireEnemy(step * 1, -30f), FireEnemy(step * 3, -30f), FireEnemy(step * 5, -30f)
        )))
        waves.add(EnemyWave(3500, listOf(
            SuicideEnemy(step * 2, -30f), SuicideEnemy(step * 4, -30f)
        )))
        waves.add(EnemyWave(3000, listOf(
            BomberEnemy(step * 1.5f, -40f), BomberEnemy(step * 3f, -40f),
            BomberEnemy(step * 4.5f, -40f)
        )))
        waves.add(EnemyWave(4000, listOf(
            FireEnemy(step * 1, -30f), FireEnemy(step * 2, -60f),
            FireEnemy(step * 3, -30f), FireEnemy(step * 4, -60f),
            FireEnemy(step * 5, -30f),
            SuicideEnemy(step * 1.5f, -100f), SuicideEnemy(step * 4.5f, -100f),
            BomberEnemy(step * 3, -150f)
        )))
        return LevelConfig(2, "火山峡谷",
            0xFFFF4500.toInt(), 0xFF8B0000.toInt(), 2f, waves)
    }

    // ===== 第3关：深海上空 =====
    private fun level3(): LevelConfig {
        val waves = mutableListOf<EnemyWave>()
        waves.add(EnemyWave(0, listOf(
            TorpedoEnemy(step * 1, -30f), TorpedoEnemy(step * 2.5f, -30f),
            TorpedoEnemy(step * 4f, -30f), TorpedoEnemy(step * 5.5f, -30f)
        )))
        waves.add(EnemyWave(4000, listOf(
            FormationEnemy(step * 1, -30f), FormationEnemy(step * 2, -40f),
            FormationEnemy(step * 3, -30f), FormationEnemy(step * 4, -40f),
            FormationEnemy(step * 5, -30f)
        )))
        waves.add(EnemyWave(4000, (0 until 12).map {
            FormationEnemy(step * (it % 5 + 1), -30f - (it / 5 * 40f))
        }))
        waves.add(EnemyWave(5000, listOf(
            TorpedoEnemy(step * 1, -30f), TorpedoEnemy(step * 5.5f, -30f),
            FormationEnemy(step * 2, -60f), FormationEnemy(step * 3, -60f),
            FormationEnemy(step * 4, -60f),
            TorpedoEnemy(step * 2.5f, -100f), TorpedoEnemy(step * 4f, -100f)
        )))
        return LevelConfig(3, "深海上空",
            0xFF006994.toInt(), 0xFF003366.toInt(), 2.5f, waves)
    }

    // ===== 第4关：雷暴之城 =====
    private fun level4(): LevelConfig {
        val waves = mutableListOf<EnemyWave>()
        waves.add(EnemyWave(0, listOf(
            JammerEnemy(step * 2, -30f), JammerEnemy(step * 4, -30f),
            JammerEnemy(step * 3, -70f)
        )))
        waves.add(EnemyWave(4000, listOf(
            ShieldEnemy(step * 1.5f, -30f), ShieldEnemy(step * 3f, -30f),
            ShieldEnemy(step * 4.5f, -30f)
        )))
        waves.add(EnemyWave(4500, listOf(
            JammerEnemy(step * 1, -30f), JammerEnemy(step * 5.5f, -30f),
            ShieldEnemy(step * 2, -60f), ShieldEnemy(step * 4, -60f),
            JammerEnemy(step * 3, -100f)
        )))
        waves.add(EnemyWave(5000, (0 until 15).map {
            if (it % 3 == 0) ShieldEnemy(step * (it % 5 + 1), -30f - (it / 3 * 50f))
            else JammerEnemy(step * (it % 5 + 1), -30f - (it / 3 * 50f))
        }))
        return LevelConfig(4, "雷暴之城",
            0xFF2F4F4F.toInt(), 0xFF1C1C1C.toInt(), 3f, waves)
    }

    // ===== 第5关：宇宙终点 =====
    private fun level5(): LevelConfig {
        val waves = mutableListOf<EnemyWave>()
        waves.add(EnemyWave(0, (0 until 8).map {
            SpaceEnemy(step * (it % 5 + 1), -30f - (it / 5 * 50f))
        }))
        waves.add(EnemyWave(5000, (0 until 6).map {
            StealthEnemy(step * (it % 5 + 1), -30f - (it / 3 * 60f))
        }))
        waves.add(EnemyWave(5000, (0 until 20).map {
            if (it % 4 == 0) StealthEnemy(step * (it % 5 + 1), -30f - (it / 5 * 40f))
            else SpaceEnemy(step * (it % 5 + 1), -30f - (it / 5 * 40f))
        }))
        waves.add(EnemyWave(6000, (0 until 25).map {
            if (it % 3 == 0) StealthEnemy(step * (it % 6 + 0.5f), -30f - (it / 6 * 45f))
            else SpaceEnemy(step * (it % 6 + 0.5f), -30f - (it / 6 * 45f))
        }))
        return LevelConfig(5, "宇宙终点",
            0xFF0D0D2B.toInt(), 0xFF000000.toInt(), 3.5f, waves)
    }
}
