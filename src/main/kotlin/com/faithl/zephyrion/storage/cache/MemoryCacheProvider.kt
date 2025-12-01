package com.faithl.zephyrion.storage.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * 内存缓存提供者实现
 * 使用 ConcurrentHashMap 存储缓存数据，支持 TTL 过期机制
 */
class MemoryCacheProvider : CacheProvider {

    private data class CacheEntry<T>(
        val data: T,
        val expireAt: Long // 0 表示永不过期
    ) {
        fun isExpired(): Boolean = expireAt > 0 && System.currentTimeMillis() > expireAt
    }

    private val cache = ConcurrentHashMap<String, CacheEntry<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: String, clazz: Class<T>): T? {
        val entry = cache[key] ?: return null
        if (entry.isExpired()) {
            cache.remove(key)
            return null
        }
        return entry.data as? T
    }

    override fun <T : Any> set(key: String, value: T, ttlMs: Long) {
        val expireAt = if (ttlMs > 0) System.currentTimeMillis() + ttlMs else 0L
        cache[key] = CacheEntry(value, expireAt)
    }

    override fun delete(key: String) {
        cache.remove(key)
    }

    override fun deleteByPrefix(prefix: String) {
        cache.keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
    }

    override fun clear() {
        cache.clear()
    }

    override fun exists(key: String): Boolean {
        val entry = cache[key] ?: return false
        if (entry.isExpired()) {
            cache.remove(key)
            return false
        }
        return true
    }

    override fun keys(prefix: String): Set<String> {
        cleanExpired()
        return cache.keys.filter { it.startsWith(prefix) }.toSet()
    }

    override fun close() {
        cache.clear()
    }

    /**
     * 清理过期缓存
     */
    fun cleanExpired() {
        cache.entries.removeIf { it.value.isExpired() }
    }

    /**
     * 获取缓存统计信息
     */
    fun stats(): Map<String, Int> {
        return mapOf("size" to cache.size)
    }
}
