package dev.crafty.framework.api.menu.types

import dev.crafty.framework.api.menu.ClickAction
import dev.crafty.framework.api.menu.Menu
import dev.crafty.framework.lib.replaceInComponent
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Abstract base class for creating paginated menus in the Crafty Framework.
 * @param T The type of data to be displayed in the paginated menu.
 * @param player The player for whom the menu is created.
 */
abstract class PaginatedMenu<T>(
    player: Player
) : Menu(player) {

    protected var currentPageIndex = 0
        private set

    /**
     * Holds indices of slots used for pagination.
     */
    protected var paginatedSlots: List<Int> = emptyList()

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
     * Returns a map of material providers for the paginated data.
     * Each entry in the map associates a key with a function that takes an item of type T
     * and returns the corresponding ItemStack to be used as the material for that item.
     * @return A map of keys to their corresponding material provider functions.
     */
    abstract fun materialProviders(): Map<String, (T) -> ItemStack>

    /**
     * Returns a map of static placeholders for the paginated menu.
     * These placeholders do not depend on the paginated data.
     * @return A map of placeholder keys to their corresponding values.
     */
    abstract fun staticPlaceholders(): Map<String, Any>

    /**
     * Returns a map of placeholders for the paginated menu.
     * @return A map of placeholder keys to their corresponding values.
     */
    final override fun placeholders(): Map<String, Any> = staticPlaceholders()

    override fun preBuild(
        config: YamlConfiguration,
        pattern: Array<CharArray>
    ) {
        val slots = mutableListOf<Int>()

        for (row in pattern.indices) {
            for (col in pattern[row].indices) {
                val char = pattern[row][col]
                val itemConfig = config.getConfigurationSection("items.$char") ?: continue

                val isPaginated = itemConfig
                    .getConfigurationSection("paginated-options")
                    ?.getBoolean("is-paginated") == true

                if (isPaginated) {
                    slots += row * 9 + col
                }
            }
        }

        paginatedSlots = slots
    }

    override fun buildMenu(): Inventory {
        val inventory = super.buildMenu() // build base menu

        val configFile = owningPlugin.dataFolder.resolve("$BASE_MENU_FOLDER/$id.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val patternList = config.getOrWarn("pattern", emptyList<String>())

        // find all paginated slots by scanning the pattern
        val paginatedIndices = mutableListOf<Int>()
        patternList.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, char ->
                val itemConfig = config.getConfigurationSection("items.$char")
                val isPaginated = itemConfig?.getConfigurationSection("paginated-options")
                    ?.getOrDefault("is-paginated", false) == true
                if (isPaginated) {
                    paginatedIndices.add(rowIndex * 9 + colIndex)
                }
            }
        }
        paginatedSlots = paginatedIndices

        return inventory
    }

    override fun buildItem(
        config: ConfigurationSection,
        slotIndex: Int
    ): Pair<ItemStack, List<MenuAction>> {

        val base = super.buildItem(config, slotIndex)

        val paginatedIndex = paginatedSlots.indexOf(slotIndex)
        if (paginatedIndex == -1) return base

        val dataIndex = currentPageIndex * paginatedSlots.size + paginatedIndex
        val dataItem = data().getOrNull(dataIndex)
            ?: return ItemStack(Material.AIR) to emptyList()

        val paginatedOptions = config.getConfigurationSection("paginated-options")
        val provideMaterial = paginatedOptions?.getBoolean("provide-material") ?: false
        val materialKey = paginatedOptions?.getString("material-key")

        val item = base.first.clone().apply {
            val meta = itemMeta.apply {
                val ph = paginatedPlaceholders().mapValues { it.value(dataItem) }
                displayName(displayName()?.replaceInComponent(ph))
                lore(lore()?.map { it.replaceInComponent(ph) })
            }
            itemMeta = meta

            if (provideMaterial && materialKey != null) {
                materialProviders()[materialKey]?.invoke(dataItem)?.let {
                    type = it.type

                    val providedMeta = it.itemMeta
                    val meta = itemMeta

                    if (providedMeta.hasCustomModelData()) {
                        meta.setCustomModelData(providedMeta.customModelData)
                    }

                    if (providedMeta.enchants.isNotEmpty()) {
                        providedMeta.enchants.forEach { (ench, level) ->
                            meta.addEnchant(ench, level, true)
                        }
                    }

                    meta.addItemFlags(*providedMeta.itemFlags.toTypedArray())
                    itemMeta = meta
                }
            }
        }

        return item to base.second
    }

    @ClickAction("next-page")
    fun nextPage(event: InventoryClickEvent) {
        val maxPage = maxPageIndex()
        if (currentPageIndex >= maxPage) return

        currentPageIndex++
        rebuild(event)
    }

    @ClickAction("previous-page")
    fun previousPage(event: InventoryClickEvent) {
        if (currentPageIndex <= 0) return

        currentPageIndex--
        rebuild(event)
    }

    private fun maxPageIndex(): Int {
        val pageSize = paginatedSlots.size
        if (pageSize == 0) return 0

        return (data().size - 1) / pageSize
    }

    private fun rebuild(event: InventoryClickEvent) {
        val inventory = event.inventory

        val configFile = owningPlugin.dataFolder.resolve("$BASE_MENU_FOLDER/$id.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val patternList = config.getOrWarn("pattern", emptyList<String>())

        val pattern = Array(patternList.size) { row ->
            patternList[row].toCharArray()
        }

        val result = buildItems(pattern, config)
        inventory.contents = result.items
    }
}