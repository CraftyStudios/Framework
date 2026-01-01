package dev.crafty.framework.api.menu.types

import dev.crafty.framework.api.menu.Menu
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory

abstract class InputMenu(player: Player) : Menu(player) {
    protected val inputSlots = mutableSetOf<Int>()

    override fun preBuild(config: YamlConfiguration, pattern: Array<CharArray>) {
        for (row in pattern.indices) {
            for (col in pattern[row].indices) {
                val char = pattern[row][col]
                val itemConfig = config.getConfigurationSection("items.$char") ?: continue

                val isInput = itemConfig.getBoolean("input", false)
                if (isInput) {
                    inputSlots += row * 9 + col
                }
            }
        }
    }

    override fun shouldCancelClick(event: InventoryClickEvent, buildInv: Inventory): Boolean {
        // Allow clicks in input slots
        if (event.inventory == buildInv && inputSlots.contains(event.slot)) {
            return false
        }

        return (event.inventory == buildInv)
    }
}