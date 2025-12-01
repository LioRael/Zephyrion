package com.faithl.zephyrion.storage.cache

import com.faithl.zephyrion.Zephyrion
import taboolib.common.platform.function.console
import taboolib.module.lang.sendLang

/**
 * 缓存配置管理
 * 类似于 DatabaseConfig，根据配置文件初始化内存或 Redis 缓存
 */
object CacheConfig {

    lateinit var provider: CacheProvider
        private set

    /**
     * 默认缓存过期时间（毫秒）
     */
    var defaultTtlMs: Long = 5 * 60 * 1000L
        private set

    /**
     * 缓存键前缀
     */
    var keyPrefix: String = "zephyrion"
        private set

    /**
     * 是否已初始化
     */
    val isInitialized: Boolean
        get() = ::provider.isInitialized

    fun initialize() {
        // 如果已初始化，先关闭旧的 provider
        if (isInitialized) {
            try {
                provider.close()
            } catch (_: Exception) {}
        }

        val type = Zephyrion.settings.getString("cache.type")
            ?: run {
                console().sendLang("cache-type-not-set")
                "memory"
            }

        defaultTtlMs = Zephyrion.settings.getLong("cache.ttl", 5 * 60 * 1000L)
        keyPrefix = Zephyrion.settings.getString("cache.key-prefix") ?: "zephyrion"

        provider = when (type.lowercase()) {
            "memory" -> createMemoryProvider()
            "redis" -> createRedisProvider()
            else -> run {
                console().sendLang("cache-type-unsupported", type)
                createMemoryProvider()
            }
        }

        console().sendLang("cache-initialized", provider::class.simpleName?.removeSuffix("Provider") ?: "Unknown")
    }

    private fun createMemoryProvider(): MemoryCacheProvider {
        return MemoryCacheProvider()
    }

    private fun createRedisProvider(): CacheProvider {
        val redisConfig = Zephyrion.settings.getConfigurationSection("cache.redis")
        if (redisConfig == null) {
            console().sendLang("cache-redis-config-not-found")
            return createMemoryProvider()
        }

        return try {
            RedisCacheProvider(redisConfig, keyPrefix)
        } catch (e: Exception) {
            console().sendLang("cache-redis-init-failed", e.message ?: "Unknown error")
            createMemoryProvider()
        }
    }

    /**
     * 关闭缓存提供者
     */
    fun shutdown() {
        if (isInitialized) {
            provider.close()
        }
    }

    // ==================== 便捷方法 ====================

    /**
     * 获取缓存值
     */
    inline fun <reified T : Any> get(key: String): T? {
        return provider.get(key, T::class.java)
    }

    /**
     * 设置缓存值（使用默认 TTL）
     */
    fun <T : Any> set(key: String, value: T) {
        provider.set(key, value, defaultTtlMs)
    }

    /**
     * 设置缓存值（自定义 TTL）
     */
    fun <T : Any> set(key: String, value: T, ttlMs: Long) {
        provider.set(key, value, ttlMs)
    }

    /**
     * 删除缓存
     */
    fun delete(key: String) {
        provider.delete(key)
    }

    /**
     * 按前缀删除缓存
     */
    fun deleteByPrefix(prefix: String) {
        provider.deleteByPrefix(prefix)
    }

    /**
     * 检查缓存是否存在
     */
    fun exists(key: String): Boolean {
        return provider.exists(key)
    }

    /**
     * 清空所有缓存
     */
    fun clear() {
        provider.clear()
    }
}
