package com.faithl.zephyrion.core.models

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Settings : IntIdTable() {

    val setting = enumerationByName("setting", 255, SettingType::class)
    val value = varchar("value", 255)
    val vault = reference("vault", Vaults)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    enum class SettingType {

    }

}

class Setting(id: EntityID<Int>) : IntEntity(id) {

    var setting by Settings.setting
    var value by Settings.value
    var vault by Vault referencedOn Settings.vault
    var createdAt by Settings.createdAt
    var updatedAt by Settings.updatedAt

    companion object : IntEntityClass<Setting>(Settings)

}