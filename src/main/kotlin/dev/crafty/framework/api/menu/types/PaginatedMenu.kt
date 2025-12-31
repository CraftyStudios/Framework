package dev.crafty.framework.api.menu.types

import dev.crafty.framework.api.menu.Menu
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * Abstract base class for creating paginated menus in the Crafty Framework.
 * @param T The type of data to be displayed in the paginated menu.
 * @param player The player for whom the menu is created.
 */
abstract class PaginatedMenu<T>(
    player: Player
) : Menu(player) {
    /**
     * Returns the list of data items to be displayed in the paginated menu.
     * @return A list of data items of type T.
     */
    abstract fun data(): List<T>

    /**
     * Returns a map of placeholders for the paginated data.
     * Each placeholder is associated with a function that takes an item of type T
     * and returns the corresponding value for that placeholder.
     * @return A map of placeholder keys to their corresponding value functions.
     */
    abstract fun paginatedPlaceholders(): Map<String, (T) -> Any>

    /**
     * Returns a map of static placeholders for the paginated menu.
     * These placeholders do not depend on the paginated data.
     * @return A map of placeholder keys to their corresponding values.
     */
    abstract fun staticPlaceholders(): Map<String, Any>

    /**
     * Returns a map of placeholders for the paginated menu.
     * This includes both static placeholders and those generated from the paginated data.
     * @return A map of placeholder keys to their corresponding values.
     */
    final override fun placeholders(): Map<String, Any> {
        val placeholders = mutableMapOf<String, Any>()
        placeholders += staticPlaceholders()

        for (item in data()) {
            placeholders += paginatedPlaceholders().mapValues { it.value(item)}
        }

        return placeholders
    }

    override fun buildMenu(): Inventory {
        TODO()
    }
}