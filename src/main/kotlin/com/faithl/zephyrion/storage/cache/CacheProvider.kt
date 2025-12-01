package com.faithl.zephyrion.storage.cache

/**
 * 缓存提供者抽象接口
 * 定义了缓存的基本操作，支持内存缓存和 Redis 缓存的实现
 */
interface CacheProvider {

    /**
     * 获取缓存值
     * @param key 缓存键
     * @return 缓存值，不存在或已过期返回 null
     */
    fun <T : Any> get(key: String, clazz: Class<T>): T?

    /**
     * 设置缓存值
     * @param key 缓存键
     * @param value 缓存值
     * @param ttlMs 过期时间（毫秒），0 表示永不过期
     */
    fun <T : Any> set(key: String, value: T, ttlMs: Long = 0)

    /**
     * 删除指定缓存
     * @param key 缓存键
     */
    fun delete(key: String)

    /**
     * 删除匹配前缀的所有缓存
     * @param prefix 键前缀
     */
    fun deleteByPrefix(prefix: String)

    /**
     * 清空所有缓存
     */
    fun clear()

    /**
     * 检查缓存是否存在且未过期
     * @param key 缓存键
     */
    fun exists(key: String): Boolean

    /**
     * 获取所有匹配前缀的键
     * @param prefix 键前缀
     */
    fun keys(prefix: String): Set<String>

    /**
     * 关闭缓存提供者，释放资源
     */
    fun close()
}

inline fun <reified T : Any> CacheProvider.get(key: String): T? = get(key, T::class.java)
