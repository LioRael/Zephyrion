package com.faithl.zephyrion.core.cache

import com.faithl.zephyrion.core.models.Item
import com.faithl.zephyrion.storage.cache.CacheConfig
import com.faithl.zephyrion.storage.cache.get

object ItemCache {

    private const val KEY_PREFIX = "items"
    private const val KEY_ALL_PREFIX = "items:all"

    private val provider get() = CacheConfig.provider
    private val ttl get() = CacheConfig.defaultTtlMs

    private fun getPageKey(vaultId: Int, page: Int, ownerUUID: String?): String {
        return "$KEY_PREFIX:$vaultId:$page:${ownerUUID ?: "shared"}"
    }

    private fun getAllKey(vaultId: Int, ownerUUID: String?): String {
        return "$KEY_ALL_PREFIX:$vaultId:${ownerUUID ?: "shared"}"
    }

    fun getPageItems(vaultId: Int, page: Int, ownerUUID: String?, isIndependent: Boolean): List<Item>? {
        val key = getPageKey(vaultId, page, if (isIndependent) ownerUUID else null)
        return provider.get<List<Item>>(key)
    }

    fun updatePageItems(vaultId: Int, page: Int, ownerUUID: String?, isIndependent: Boolean, items: List<Item>) {
        val key = getPageKey(vaultId, page, if (isIndependent) ownerUUID else null)
        provider.set(key, items, ttl)
    }

    fun getAllItems(vaultId: Int, ownerUUID: String?, isIndependent: Boolean): List<Item>? {
        val key = getAllKey(vaultId, if (isIndependent) ownerUUID else null)
        return provider.get<List<Item>>(key)
    }

    fun updateAllItems(vaultId: Int, ownerUUID: String?, isIndependent: Boolean, items: List<Item>) {
        val key = getAllKey(vaultId, if (isIndependent) ownerUUID else null)
        provider.set(key, items, ttl)
    }

    /**
     * 添加或更新单个物品到缓存（写时更新）
     */
    fun addOrUpdate(vaultId: Int, page: Int, slot: Int, ownerUUID: String?, isIndependent: Boolean, item: Item) {
        val effectiveOwner = if (isIndependent) ownerUUID else null
        val pageKey = getPageKey(vaultId, page, effectiveOwner)
        val allKey = getAllKey(vaultId, effectiveOwner)

        val pageItems = provider.get<List<Item>>(pageKey)
        if (pageItems != null) {
            val updatedItems = pageItems.filter { it.slot != slot } + item
            provider.set(pageKey, updatedItems, ttl)
        }

        val allItems = provider.get<List<Item>>(allKey)
        if (allItems != null) {
            val updatedItems = allItems.filter { !(it.page == page && it.slot == slot) } + item
            provider.set(allKey, updatedItems, ttl)
        }
    }

    /**
     * 从缓存中移除单个物品（写时更新）
     */
    fun remove(vaultId: Int, page: Int, slot: Int, ownerUUID: String?, isIndependent: Boolean) {
        val effectiveOwner = if (isIndependent) ownerUUID else null
        val pageKey = getPageKey(vaultId, page, effectiveOwner)
        val allKey = getAllKey(vaultId, effectiveOwner)

        val pageItems = provider.get<List<Item>>(pageKey)
        if (pageItems != null) {
            val updatedItems = pageItems.filter { it.slot != slot }
            provider.set(pageKey, updatedItems, ttl)
        }

        val allItems = provider.get<List<Item>>(allKey)
        if (allItems != null) {
            val updatedItems = allItems.filter { !(it.page == page && it.slot == slot) }
            provider.set(allKey, updatedItems, ttl)
        }
    }

    fun invalidatePage(vaultId: Int, page: Int, ownerUUID: String?, isIndependent: Boolean) {
        val key = getPageKey(vaultId, page, if (isIndependent) ownerUUID else null)
        provider.delete(key)
        val allKey = getAllKey(vaultId, if (isIndependent) ownerUUID else null)
        provider.delete(allKey)
    }

    fun invalidateAll(vaultId: Int, ownerUUID: String? = null) {
        provider.deleteByPrefix("$KEY_PREFIX:$vaultId:")
        provider.deleteByPrefix("$KEY_ALL_PREFIX:$vaultId:")
    }
}
