package dev.crafty.framework.api.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory

/**
 * Abstract base class for creating menus in the Crafty Framework.
 * @param player The player for whom the menu is created.
 */
abstract class Menu(
    val player: Player
) {
    /**
     * The unique identifier for the menu.
     * @return The menu ID.
     */
    protected abstract val id: String

    /**
     * Returns a map of placeholders for the menu.
     * These placeholders will be replaced in the menu's title and items.
     * @return A map of placeholder keys to their corresponding values.
     */
    protected abstract fun placeholders(): Map<String, Any>

    /**
     * Called when the menu is opened.
     * @param event The inventory open event.
     */
    open fun onOpen(event: InventoryOpenEvent) {}

    /**
     * Called when the menu is closed.
     * @param event The inventory close event.
     */
    open fun onClose(event: InventoryCloseEvent) {}

    /**
     * Opens the menu for the player.
     */
    fun open() {

    }

    /**
     * Builds the inventory for the menu.
     * @return The constructed inventory.
     */
    protected open fun buildMenu(): Inventory {
        TODO()
    }
}