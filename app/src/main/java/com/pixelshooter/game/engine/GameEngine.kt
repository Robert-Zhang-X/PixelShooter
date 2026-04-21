package com.pixelshooter.game.engine

import android.content.Context
import com.pixelshooter.game.audio.AudioManager
import com.pixelshooter.game.entities.*
import com.pixelshooter.game.entities.enemies.EnemyPlane
import com.pixelshooter.game.entities.enemies.SuicideEnemy
import com.pixelshooter.game.entities.enemies.ShieldEnemy
import com.pixelshooter.game.entities.bosses.*
import com.pixelshooter.game.levels.LevelFactory
import kotlin.math.sqrt
import kotlin.random.Random

enum class GameState { PLAYING, BOSS_APPEARING, BOSS_FIGHT, LEVEL_COMPLETE, GAME_OVER }

class GameEngine(
    val levelId: Int,
    val planeType: PlaneType,
    val onScoreUpdate: (Int) -> Unit,
    val onHpUpdate: (Int, Int) -> Unit,
    val onBombUpdate: (Int) -> Unit,
    val onStateChange: (GameState) -> Unit,
    val onBossHpUpdate: (Int, Int) -> Unit,
    val onSkillCooldown: (Float) -> Unit   // 0~1 冷却进度
) {
    // ==================== 游戏状态 ====================
    var state = GameState.PLAYING
        private set(v) { field = v; onStateChange(v) }

    // ==================== 实体列表 ====================
    val player: PlayerPlane = when (planeType) {
        PlaneType.FALCON -> FalconPlane(GameConfig.LOGICAL_WIDTH / 2, GameConfig.LOGICAL_HEIGHT - 80f)
        PlaneType.STORM  -> StormPlane(GameConfig.LOGICAL_WIDTH / 2, GameConfig.LOGICAL_HEIGHT - 80f)
        PlaneType.HUNTERII -> HunterIIPlane(GameConfig.LOGICAL_WIDTH / 2, GameConfig.LOGICAL_HEIGHT - 80f)
    }
    val enemies = mutableListOf<EnemyPlane>()
    val bullets = mutableListOf<Bullet>()
    val items = mutableListOf<Item>()
    var boss: Boss? = null
    private val bossClones = mutableListOf<BossClone>()

    // ==================== 关卡数据 ====================
    private val levelConfig = LevelFactory.createLevel(levelId)
    private var currentWaveIndex = 0
    private var waveTimer = 0L
    private var allWavesDeployed = false
    private var gameTime = 0L

    // ==================== 背景滚动 ====================
    var bgOffset = 0f

    // ==================== 主更新循环 ====================
    fun update(deltaMs: Long) {
        if (state == GameState.GAME_OVER || state == GameState.LEVEL_COMPLETE) return

        gameTime += deltaMs
        bgOffset += levelConfig.bgScrollSpeed

        // 更新玩家
        player.update(deltaMs)

        // 追踪最近敌人（猎鹰II专用）
        if (player is HunterIIPlane) {
            player.nearestEnemy = findNearest()
        }

        // 玩家自动射击
        val now = System.currentTimeMillis()
        val newPlayerBullets = player.tryFire(now)
        if (newPlayerBullets.isNotEmpty()) {
            bullets.addAll(newPlayerBullets)
            AudioManager.playSound(AudioManager.SoundType.SHOOT)
        }

        // 部署敌机波次
        if (!allWavesDeployed && state == GameState.PLAYING) {
            deployWaves(deltaMs)
        }

        // 判断是否进入Boss阶段
        if (allWavesDeployed && enemies.isEmpty() && boss == null && state == GameState.PLAYING) {
            spawnBoss()
        }

        // 更新敌机
        enemies.removeAll { !it.isAlive }
        enemies.forEach { enemy ->
            enemy.update(deltaMs)
            val fired = enemy.fire(player.x, player.y)
            bullets.addAll(fired)

            // 自爆敌机碰撞检测
            if (enemy is SuicideEnemy &&
                CollisionSystem.isColliding(enemy.collisionBounds, player.collisionBounds)) {
                enemy.isAlive = false
                player.takeDamage(enemy.explodeDamage)
                onHpUpdate(player.hp, player.maxHp)
            }
        }

        // 更新Boss
        boss?.let { b ->
            b.update(deltaMs)
            val bFired = b.fire(player.x, player.y)
            bullets.addAll(bFired)

            // Boss分身（第5关）
            if (b is Boss5CosmicDestroyer) {
                bossClones.clear()
                bossClones.addAll(b.activeClones)
            }

            onBossHpUpdate(b.hp, b.maxHp)
            onSkillCooldown(1f - player.skillCooldown.toFloat() / player.skillMaxCooldown)

            if (!b.isAlive) {
                // Boss死亡：掉落道具、关卡完成
                repeat(2) { dropItem(b.x, b.y) }
                state = GameState.LEVEL_COMPLETE
            }
        }

        // 更新子弹
        bullets.removeAll { !it.isAlive }
        bullets.forEach { it.update(deltaMs) }

        // 子弹碰撞检测
        detectBulletCollisions()

        // 更新道具
        items.removeAll { !it.isAlive }
        items.forEach { item ->
            item.update(deltaMs)
            if (CollisionSystem.isColliding(item.collisionBounds, player.collisionBounds)) {
                player.applyItem(item)
                item.isAlive = false
                onScoreUpdate(player.score)
                onHpUpdate(player.hp, player.maxHp)
                onBombUpdate(player.bombs)
            }
        }

        // 回调UI
        onScoreUpdate(player.score)
        onHpUpdate(player.hp, player.maxHp)

        if (!player.isAlive) state = GameState.GAME_OVER
    }

    // ==================== 子弹碰撞 ====================
    private fun detectBulletCollisions() {
        // 使用迭代器安全遍历，避免 ConcurrentModificationException
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()
            if (!bullet.isAlive) continue
            
            if (bullet.isPlayerBullet) {
                // 玩家子弹 vs 敌机 - 使用索引遍历避免并发修改
                for (i in enemies.size - 1 downTo 0) {
                    val enemy = enemies[i]
                    if (enemy.isAlive &&
                        CollisionSystem.isColliding(bullet.collisionBounds, enemy.collisionBounds)) {
                        if (enemy is ShieldEnemy && enemy.shieldLayers > 0) {
                            enemy.hitShield()
                        } else {
                            enemy.hp -= bullet.damage
                            if (enemy.hp <= 0) {
                                enemy.isAlive = false
                                player.score += enemy.scoreValue
                                AudioManager.playSound(AudioManager.SoundType.EXPLOSION)
                                if (Random.nextFloat() < enemy.dropChance) dropItem(enemy.x, enemy.y)
                            }
                        }
                        if (!bullet.isPenetrating) {
                            bullet.isAlive = false
                            break
                        }
                    }
                }
                
                // 玩家子弹 vs Boss
                boss?.let { b ->
                    if (b.isAlive && !b.isInvincible &&
                        CollisionSystem.isColliding(bullet.collisionBounds, b.collisionBounds)) {
                        if (b is Boss2FlameKing && b.hasArmor) {
                            b.hitArmor(bullet.damage)
                        } else {
                            b.hp -= bullet.damage
                            if (b.hp <= 0) b.isAlive = false
                        }
                        if (!bullet.isPenetrating) bullet.isAlive = false
                    }
                }
                
                // 玩家子弹 vs Boss分身 - 使用索引遍历
                for (i in bossClones.size - 1 downTo 0) {
                    val clone = bossClones[i]
                    if (clone.isAlive &&
                        CollisionSystem.isColliding(bullet.collisionBounds, clone.collisionBounds)) {
                        clone.hp -= bullet.damage
                        if (clone.hp <= 0) clone.isAlive = false
                        if (!bullet.isPenetrating) {
                            bullet.isAlive = false
                            break
                        }
                    }
                }
            } else {
                // 敌机子弹 vs 玩家
                if (CollisionSystem.isColliding(bullet.collisionBounds, player.collisionBounds)) {
                    player.takeDamage(bullet.damage)
                    bullet.isAlive = false
                    AudioManager.playSound(AudioManager.SoundType.PLAYER_HIT)
                    onHpUpdate(player.hp, player.maxHp)
                }
            }
        }
    }

    // ==================== 波次部署 ====================
    private fun deployWaves(deltaMs: Long) {
        if (currentWaveIndex >= levelConfig.waves.size) {
            allWavesDeployed = true; return
        }
        waveTimer += deltaMs
        val wave = levelConfig.waves[currentWaveIndex]
        if (waveTimer >= wave.delayMs) {
            enemies.addAll(wave.enemies)
            currentWaveIndex++
            waveTimer = 0
        }
    }

    // ==================== 生成Boss ====================
    private fun spawnBoss() {
        AudioManager.playSound(AudioManager.SoundType.BOSS_APPEAR)
        val cx = GameConfig.LOGICAL_WIDTH / 2
        boss = when (levelId) {
            1 -> Boss1IronWing(cx)
            2 -> Boss2FlameKing(cx)
            3 -> Boss3DeepLord(cx)
            4 -> Boss4ThunderLord(cx)
            5 -> Boss5CosmicDestroyer(cx)
            else -> Boss1IronWing(cx)
        }
        state = GameState.BOSS_FIGHT
    }

    // ==================== 炸弹技能 ====================
    fun useBomb() {
        if (player.bombs <= 0) return
        AudioManager.playSound(AudioManager.SoundType.BOMB)
        player.bombs--
        // 清除所有敌机子弹，对所有敌机造成200伤害
        bullets.removeAll { !it.isPlayerBullet }
        enemies.forEach { e -> e.hp -= 200; if (e.hp <= 0) { e.isAlive = false; player.score += e.scoreValue } }
        boss?.let { b -> if (!b.isInvincible) { b.hp -= 200; if (b.hp <= 0) b.isAlive = false } }
        onBombUpdate(player.bombs)
        onScoreUpdate(player.score)
    }

    // ==================== 特技技能 ====================
    fun useSkill() {
        val skillBullets = player.useSkill()
        bullets.addAll(skillBullets)
    }

    // ==================== 工具 ====================
    private fun findNearest(): GameEntity? {
        val allTargets = (enemies as List<GameEntity>) + listOfNotNull(boss)
        return allTargets.filter { it.isAlive }.minByOrNull { e ->
            val dx = e.x - player.x; val dy = e.y - player.y
            sqrt(dx * dx + dy * dy)
        }
    }

    private fun dropItem(x: Float, y: Float) {
        val type = ItemType.values()[Random.nextInt(ItemType.values().size)]
        items.add(Item(x, y, type))
    }
}
