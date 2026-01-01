package dev.crafty.framework.api.menu

import dev.crafty.framework.api.event.on
import dev.crafty.framework.api.lifecycle.FrameworkPlugin
import dev.crafty.framework.api.logs.Logger
import dev.crafty.framework.events.GlobalEventRouter
import dev.crafty.framework.lib.colorize
import dev.crafty.framework.lib.replaceInString
import dev.crafty.framework.lib.replaceInStringList
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Abstract base class for creating menus in the Crafty Framework.
 * @param player The player for whom the menu is created.
 */
abstract class Menu(
    val player: Player
) : KoinComponent {
    private val logger: Logger by inject()

    companion object {
        const val BASE_MENU_FOLDER = "menus"
    }

    private var currentInventory: Inventory? = null

    /**
     * The plugin that owns this menu.
     * @return The owning FrameworkPlugin.
     */
    protected abstract val owningPlugin: FrameworkPlugin

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
    open fun open() {
        val inv = buildMenu()
        player.openInventory(inv)
    }

    protected open fun shouldCancelClick(event: InventoryClickEvent, buildInv: Inventory): Boolean {
        if (event.inventory == buildInv) return true

        if (event.clickedInventory == event.view.bottomInventory && event.isShiftClick) {
            return true
        }

        return false
    }

    /**
     * Called before items are built.
     * Allows subclasses to prepare state derived from config/pattern.
     */
    protected open fun preBuild(config: YamlConfiguration, pattern: Array<CharArray>) {}

    /**
     * Builds the inventory for the menu.
     * @return The constructed inventory.
     */
    protected open fun buildMenu(): Inventory {
        val configFile = owningPlugin.dataFolder.resolve("$BASE_MENU_FOLDER/$id.yml")

        try {
            owningPlugin.saveResource("$BASE_MENU_FOLDER/$id.yml", false)
        } catch (e: Exception) {} // the owning plugin doesn't have a default menu resource, it will be thrown next

        if (!configFile.exists()) {
            throw IllegalStateException("Menu configuration file not found: ${configFile.path}")
        }

        val config = YamlConfiguration.loadConfiguration(configFile)

        val title = config.getOrWarn("title", "Menu")
        val rows = config.getOrWarn("rows", 1)
        val typeString = config.getOrWarn("type", "CHEST")
        val type = try {
            ContainerType.valueOf(typeString.uppercase())
        } catch (e: IllegalArgumentException) {
            owningPlugin.logger.warning(
                "Menu '$id' has invalid container type '$typeString'. Defaulting to CHEST."
            )
            ContainerType.CHEST
        }

        val patternList = config.getOrWarn("pattern", List(rows) { " ".repeat(9) })

        // convert to 9xN char array
        val pattern = Array(rows) { rowIndex ->
            patternList[rowIndex].toCharArray()
        }

        preBuild(config, pattern)

        val result = buildItems(pattern, config)

        // create inventory container
        val inventory = createInventoryContainer(type, rows, title.colorize())

        inventory.contents = result.items

        // register event listeners
        val openId = on<InventoryOpenEvent> { event, _ ->
            // not our inventory
            if (event.inventory != inventory) return@on

            onOpen(event)
        }

        val clickId = on<InventoryClickEvent> { event, _ ->
            event.isCancelled = shouldCancelClick(event, inventory)

            val slot = event.slot
            val actions = result.actions[slot] ?: return@on
            val clickType = event.click

            for (action in actions) {
                if (action.type == clickType) {
                    // execute action by id
                    for (actionId in action.actionIds) {
                        dispatchAction(event, actionId)
                    }
                }
            }
        }

        on<InventoryCloseEvent> { event, closeId ->
            // not our inventory
            if (event.inventory != inventory) return@on

            // unregister listeners
            GlobalEventRouter.unregisterListener(openId)
            GlobalEventRouter.unregisterListener(clickId)
            GlobalEventRouter.unregisterListener(closeId)

            onClose(event)
        }

        currentInventory = inventory

        return inventory
    }

    /**
     * Puts an item in the GUI at the specified slot.
     * This method can be called after the menu has been built (e.g., after open() or buildMenu()).
     * @param slot The slot index to place the item in.
     * @param item The ItemStack to place.
     */
    protected fun putItem(slot: Int, item: ItemStack) {
        currentInventory?.setItem(slot, item)
    }

    /**
     * Dispatches the action associated with the given action ID.
     * @param event The inventory click event.
     * @param actionId The action ID to dispatch.
     */
    private fun dispatchAction(event: InventoryClickEvent, actionId: String) {
        // find methods with @ClickAction in implementing class
        val kClass = this::class
        val methods = kClass.members.filter { member ->
            member.annotations.any { it is ClickAction && it.id == actionId }
        }

        logger.debug("Dispatching action '$actionId' for menu '$id' with ${methods.size} handlers.")

        for (method in methods) {
            method.call(this, event)
        }
    }

    /**
     * Builds the items for the menu based on the provided character array.
     * @param pattern A 2D array of characters representing the menu layout.
     * @return A 2D array of ItemStacks representing the menu items.
     */
    protected fun buildItems(
        pattern: Array<CharArray>,
        config: YamlConfiguration
    ): BuildItemsResult {
        val actions = mutableMapOf<Int, List<MenuAction>>()

        // construct final items array based on pattern
        val itemsArray = Array(pattern.size * 9) { ItemStack(Material.AIR) }

        for (rowIndex in pattern.indices) {
            for (colIndex in pattern[rowIndex].indices) {
                val char = pattern[rowIndex][colIndex]
                val slot = rowIndex * 9 + colIndex

                val itemConfig = config.getConfigurationSection("items.$char")
                val item = if (itemConfig != null) {
                    buildItem(itemConfig, slot)
                } else {
                    owningPlugin.logger.warning(
                        "Menu '$id' is missing item configuration for character '$char'. Using AIR as default."
                    )
                    Pair(ItemStack(Material.AIR), emptyList<MenuAction>())
                }

                itemsArray[slot] = item.first
                actions[slot] = item.second
            }
        }

        return BuildItemsResult(
            actions = actions,
            items = itemsArray
        )
    }

    /**
     * Builds an individual item for the menu based on the provided character.
     * @param config The config section of the item.
     * @return The constructed ItemStack for the menu item.
     */
    protected open fun buildItem(config: ConfigurationSection, slotIndex: Int): Pair<ItemStack, List<MenuAction>> {
        val materialString = config.getOrDefault("material", "STONE")
        val material = try {
            Material.valueOf(materialString.uppercase())
        } catch (e: IllegalArgumentException) {
            owningPlugin.logger.warning(
                "Menu '$id' has invalid material '$materialString' for item in slot $slotIndex. Defaulting to STONE."
            )
            Material.STONE
        }

        val name = config.getOrDefault("name", "Item")
        val rawLore = config.getOrDefault("lore", emptyList<String>())

        // format: ENCHANTMENT:LEVEL
        val enchantments = config.getOrDefault("enchantments", emptyList<String>())
        val flags = config.getOrDefault("flags", emptyList<String>())

        val actions = config.getConfigurationSection("actions")

        // actions
        val rightClickActionIds = actions?.getOrDefault("right-click", emptyList<String>())
        val leftClickActionIds = actions?.getOrDefault("left-click", emptyList<String>())
        val middleClickActionIds = actions?.getOrDefault("middle-click", emptyList<String>())
        val shiftRightClickActionIds = actions?.getOrDefault("shift-right-click", emptyList<String>())
        val shiftLeftClickActionIds = actions?.getOrDefault("shift-left-click", emptyList<String>())

        return ItemStack(material).apply {
            if (itemMeta != null) {
                val meta = itemMeta.apply {
                    displayName(name.replaceInString(placeholders()).colorize())
                    lore(rawLore.replaceInStringList(placeholders()).colorize())
                }

                itemMeta = meta
            }

            enchantments.forEach { ench ->
                // loop over registry, find by key, compare ignoring case, then add with level
                // add unsafe to allow for all enchants on any item
                addUnsafeEnchantment(
                    RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                        .firstOrNull {
                            it.key.key.equals(ench.split(":")[0], ignoreCase = true)
                        } ?: throw IllegalArgumentException("Invalid enchantment: ${ench.split(":")[0]}"),
                    ench.split(":").getOrNull(1)?.toIntOrNull() ?: 1
                )
            }

            flags.forEach { flag ->
                // already throws IllegalArgumentException if invalid
                addItemFlags(
                    ItemFlag.valueOf(flag.uppercase())
                )
            }
        } to listOfNotNull(
            rightClickActionIds?.let {
                MenuAction(ClickType.RIGHT, it)
            },
            leftClickActionIds?.let {
                MenuAction(ClickType.LEFT, it)
            },
            middleClickActionIds?.let {
                MenuAction(ClickType.MIDDLE, it)
            },
            shiftRightClickActionIds?.let {
                MenuAction(ClickType.SHIFT_RIGHT, it)
            },
            shiftLeftClickActionIds?.let {
                MenuAction(ClickType.SHIFT_LEFT, it)
            }
        )
    }

    /**
     * Retrieves a configuration value from the config.
     * If the value is missing or of the wrong type, a warning is logged and the default value is returned.
     * @param T The expected type of the configuration value.
     * @param path The configuration path.
     * @param default The default value to return if the configuration is missing or of the wrong type.
     * @return The configuration value or the default value.
     */
    protected inline fun <reified T> ConfigurationSection.getOrWarn(
        path: String,
        default: T
    ): T {
        val value = get(path)

        if (value == null) {
            owningPlugin.logger.warning(
                "Menu '$id' is missing configuration for '$path'. Using default value '$default'."
            )
            return default
        }

        if (value !is T) {
            owningPlugin.logger.warning(
                "Menu '$id' has wrong type for '$path'. Expected ${T::class.simpleName}, got ${value::class.simpleName}. Using default '$default'."
            )
            return default
        }

        return value
    }

    /**
     * Retrieves a configuration value from the config.
     * If the value is missing or of the wrong type, the default value is returned.
     * @param T The expected type of the configuration value.
     * @param path The configuration path.1
     * @param default The default value to return if the configuration is missing or of the wrong type.
     * @return The configuration value or the default value.
     */
    protected inline fun <reified T> ConfigurationSection.getOrDefault(
        path: String,
        default: T
    ): T {
        val value = get(path)

        if (value == null || value !is T) {
            return default
        }

        return value
    }

    /**
     * Creates an inventory container based on the specified type, rows, and title.
     * @param type The type of container to create.
     * @param rows The number of rows for the container (only applicable for CHEST type).
     * @param title The title of the container.
     * @return The created Inventory.
     */
    fun createInventoryContainer(
        type: ContainerType,
        rows: Int,
        title: Component
    ): Inventory = when (type) {
        ContainerType.CHEST -> owningPlugin.server.createInventory(
            player,
            rows * 9,
            title
        )

        ContainerType.DISPENSER -> owningPlugin.server.createInventory(
            player,
            9,
            title
        )

        ContainerType.HOPPER -> owningPlugin.server.createInventory(
            player,
            5,
            title
        )
    }

    /**
     * Enum representing the types of container menus.
     */
    enum class ContainerType {
        CHEST,
        DISPENSER,
        HOPPER
    }

    /**
     * Data class representing a menu action with its type and associated action IDs.
     * @param type The type of action.
     * @param actionIds The list of action IDs associated with the action.
     */
    protected data class MenuAction(
        val type: ClickType,
        val actionIds: List<String>
    )

    /**
     * Data class representing the result of building the items.
     * @param actions A map of ItemStacks to their associated menu actions.
     * @param items An array of ItemStacks representing the menu items.
     */
    protected data class BuildItemsResult(
        val actions: Map<Int, List<MenuAction>>,
        val items: Array<ItemStack> // not comparing or hashing, don't need these methods
    )
}