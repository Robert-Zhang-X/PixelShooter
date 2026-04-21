package com.pixelshooter.game.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.AudioAttributes

/**
 * 音频管理器 - 管理背景音乐和音效
 */
object AudioManager {
    
    private var soundPool: SoundPool? = null
    private var bgmPlayer: MediaPlayer? = null
    
    // 音效ID映射
    private var soundIds = mutableMapOf<SoundType, Int>()
    private var loadedSounds = mutableMapOf<Int, Boolean>()
    
    private var isInitialized = false
    private var isBgmEnabled = true
    private var isSfxEnabled = true
    private var bgmVolume = 0.5f
    private var sfxVolume = 0.8f
    
    enum class SoundType {
        SHOOT,          // 射击
        EXPLOSION,      // 爆炸
        POWER_UP,       // 道具
        BOMB,           // 炸弹
        PLAYER_HIT,     // 玩家受伤
        BOSS_APPEAR,    // Boss出现
        GAME_OVER,      // 游戏结束
        VICTORY         // 胜利
    }
    
    /**
     * 初始化音频管理器
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        // 创建 SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()
            .apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    loadedSounds[sampleId] = status == 0
                }
            }
        
        // 加载音效资源（使用系统默认音效）
        soundPool?.let { pool ->
            // 由于我们没有自定义音效文件，使用系统音效或留空
            // 实际项目中应该加载 R.raw.xxx 资源
            // soundIds[SoundType.SHOOT] = pool.load(context, R.raw.shoot, 1)
        }
        
        isInitialized = true
    }
    
    /**
     * 播放背景音乐
     */
    fun playBGM(context: Context) {
        if (!isBgmEnabled) return
        
        stopBGM()
        
        // 创建循环播放的背景音乐
        // 实际项目中应该使用 R.raw.bgm
        bgmPlayer = MediaPlayer().apply {
            // 由于没有音乐文件，暂时不播放
            // val afd = context.resources.openRawResourceFd(R.raw.bgm)
            // setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            // afd.close()
            // isLooping = true
            // setVolume(bgmVolume, bgmVolume)
            // prepare()
            // start()
        }
    }
    
    /**
     * 停止背景音乐
     */
    fun stopBGM() {
        bgmPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        bgmPlayer = null
    }
    
    /**
     * 暂停背景音乐
     */
    fun pauseBGM() {
        bgmPlayer?.pause()
    }
    
    /**
     * 恢复背景音乐
     */
    fun resumeBGM() {
        if (isBgmEnabled) {
            bgmPlayer?.start()
        }
    }
    
    /**
     * 播放音效
     */
    fun playSound(soundType: SoundType) {
        if (!isSfxEnabled || !isInitialized) return
        
        soundIds[soundType]?.let { soundId ->
            if (loadedSounds[soundId] == true) {
                soundPool?.play(soundId, sfxVolume, sfxVolume, 1, 0, 1.0f)
            }
        }
    }
    
    /**
     * 设置背景音乐开关
     */
    fun setBgmEnabled(enabled: Boolean) {
        isBgmEnabled = enabled
        if (enabled) {
            resumeBGM()
        } else {
            pauseBGM()
        }
    }
    
    /**
     * 设置音效开关
     */
    fun setSfxEnabled(enabled: Boolean) {
        isSfxEnabled = enabled
    }
    
    /**
     * 设置背景音乐音量
     */
    fun setBgmVolume(volume: Float) {
        bgmVolume = volume.coerceIn(0f, 1f)
        bgmPlayer?.setVolume(bgmVolume, bgmVolume)
    }
    
    /**
     * 设置音效音量
     */
    fun setSfxVolume(volume: Float) {
        sfxVolume = volume.coerceIn(0f, 1f)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stopBGM()
        soundPool?.release()
        soundPool = null
        isInitialized = false
    }
}
