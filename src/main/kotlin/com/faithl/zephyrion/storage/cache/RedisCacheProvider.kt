package com.faithl.zephyrion.storage.cache

import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import taboolib.common.platform.function.console
import taboolib.expansion.LettuceRedisClient
import taboolib.expansion.LettuceRedisConfig
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.lang.sendErrorMessage
import java.io.*
import java.util.Base64

/**
 * Redis 缓存提供者实现
 * 使用 TabooLib LettuceRedis 模块连接 Redis 服务器
 */
class RedisCacheProvider(
    config: ConfigurationSection,
    private val keyPrefix: String
) : CacheProvider {

    private val client: LettuceRedisClient

    init {
        val redisConfig = LettuceRedisConfig(config)
        client = LettuceRedisClient(redisConfig)

        // 启动客户端
        try {
            client.start().join()
        } catch (e: Exception) {
            console().sendErrorMessage("Failed to connect to Redis: ${e.message}")
            throw e
        }
    }

    private fun prefixedKey(key: String): String = "$keyPrefix:$key"

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: String, clazz: Class<T>): T? {
        return try {
            client.useCommands { commands ->
                val data = commands.get(prefixedKey(key)) ?: return@useCommands null
                deserialize(Base64.getDecoder().decode(data)) as? T
            }
        } catch (e: Exception) {
            console().sendErrorMessage("Redis get error: ${e.message}")
            null
        }
    }

    override fun <T : Any> set(key: String, value: T, ttlMs: Long) {
        try {
            client.useCommands { commands ->
                val keyStr = prefixedKey(key)
                val valueStr = Base64.getEncoder().encodeToString(serialize(value))
                if (ttlMs > 0) {
                    commands.psetex(keyStr, ttlMs, valueStr)
                } else {
                    commands.set(keyStr, valueStr)
                }
            }
        } catch (e: Exception) {
            console().sendErrorMessage("Redis set error: ${e.message}")
        }
    }

    override fun delete(key: String) {
        try {
            client.useCommands { commands ->
                commands.del(prefixedKey(key))
            }
        } catch (e: Exception) {
            console().sendErrorMessage("Redis delete error: ${e.message}")
        }
    }

    override fun deleteByPrefix(prefix: String) {
        try {
            client.useCommands { commands ->
                val pattern = "${prefixedKey(prefix)}*"
                var cursor = ScanCursor.INITIAL
                do {
                    val result = commands.scan(cursor, ScanArgs().match(pattern).limit(100))
                    cursor = result
                    val keys = result.keys
                    if (keys.isNotEmpty()) {
                        commands.del(*keys.toTypedArray())
                    }
                } while (!cursor.isFinished)
            }
        } catch (e: Exception) {
            console().sendErrorMessage("Redis deleteByPrefix error: ${e.message}")
        }
    }

    override fun clear() {
        deleteByPrefix("")
    }

    override fun exists(key: String): Boolean {
        return try {
            client.useCommands { commands ->
                commands.exists(prefixedKey(key)) > 0
            } ?: false
        } catch (e: Exception) {
            console().sendErrorMessage("Redis exists error: ${e.message}")
            false
        }
    }

    override fun keys(prefix: String): Set<String> {
        return try {
            client.useCommands { commands ->
                val pattern = "${prefixedKey(prefix)}*"
                val result = mutableSetOf<String>()
                var cursor = ScanCursor.INITIAL
                do {
                    val scanResult = commands.scan(cursor, ScanArgs().match(pattern).limit(100))
                    cursor = scanResult
                    result.addAll(scanResult.keys.map { it.removePrefix("$keyPrefix:") })
                } while (!cursor.isFinished)
                result
            } ?: emptySet()
        } catch (e: Exception) {
            console().sendErrorMessage("Redis keys error: ${e.message}")
            emptySet()
        }
    }

    override fun close() {
        client.client.close()
    }

    private fun serialize(obj: Any): ByteArray {
        ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos ->
                oos.writeObject(obj)
            }
            return baos.toByteArray()
        }
    }

    private fun deserialize(bytes: ByteArray): Any? {
        ByteArrayInputStream(bytes).use { bais ->
            ObjectInputStream(bais).use { ois ->
                return ois.readObject()
            }
        }
    }
}
