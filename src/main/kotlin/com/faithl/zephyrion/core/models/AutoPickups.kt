package com.faithl.zephyrion.core.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object AutoPickups : IntIdTable() {

    val type = enumerationByName("setting", 255, Type::class)
    val value = varchar("value", 255)
    val vault = reference("vault", Vaults)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    enum class Type {
        ITEM_PICKUP,
        ITEM_NOT_PICKUP,
    }

}

class AutoPickup(id: EntityID<Int>) : IntEntity(id) {

    var type by AutoPickups.type
    var value by AutoPickups.value
    var vault by Vault referencedOn AutoPickups.vault
    var createdAt by AutoPickups.createdAt
    var updatedAt by AutoPickups.updatedAt

    companion object : IntEntityClass<AutoPickup>(AutoPickups)

}