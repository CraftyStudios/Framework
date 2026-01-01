package dev.crafty.framework.api.menu.types

import dev.crafty.framework.api.menu.ClickAction
import dev.crafty.framework.api.menu.Menu
import dev.crafty.framework.api.tasks.now
import dev.crafty.framework.lib.replaceInComponent
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import kotlin.math.max

/**
 * Async-safe paginated menu base.
 * Data is fetched off-thread, inventories are rendered on-thread.
 */
abstract class PaginatedMenu<T>(
    player: Player
) : Menu(player) {

    protected var currentPageIndex = 0

    protected var paginatedSlots: List<Int> = emptyList()

    private var dataList: List<T> = emptyList()
    private var loading = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /* =======================
     *  Abstract API
     * ======================= */

    abstract suspend fun data(): List<T>

    abstract fun paginatedPlaceholders(): Map<String, (T) -> Any>

    abstract fun materialProviders(): Map<String, (T) -> ItemStack>

    abstract fun staticPlaceholders(): Map<String, Any>

    final override fun placeholders(): Map<String, Any> = staticPlaceholders()

    /* =======================
     *  Entry point
     * ======================= */

    override fun open() {
        player.closeInventory()

        scope.launch {
            val result = runCatching { data() }.getOrDefault(emptyList())

            now {
                dataList = result
                loading = false
                player.openInventory(buildMenu())
            }
        }

        // show loading menu immediately
        player.openInventory(buildMenu())
    }

    /* =======================
     *  Menu building
     * ======================= */

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
        val inventory = super.buildMenu()

        if (loading) {
            inventory.contents = Array(inventory.size) {
                ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
                    itemMeta = itemMeta.apply {
                        displayName(
                            net.kyori.adventure.text.Component.text("Loading...")
                        )
                    }
                }
            }
            return inventory
        }

        return inventory
    }

    override fun buildItem(
        config: ConfigurationSection,
        slotIndex: Int
    ): Pair<ItemStack, List<MenuAction>> {

        val base = super.buildItem(config, slotIndex)

        if (loading) return base

        val paginatedIndex = paginatedSlots.indexOf(slotIndex)
        if (paginatedIndex == -1) return base

        val dataIndex = currentPageIndex * paginatedSlots.size + paginatedIndex
        val dataItem = dataList.getOrNull(dataIndex)
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
                    val pdc = providedMeta.persistentDataContainer
                    val mdc = meta.persistentDataContainer

                    copyPersistentData(pdc, mdc)

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

    /* =======================
     *  Pagination
     * ======================= */

    @ClickAction("next-page")
    fun nextPage(event: InventoryClickEvent) {
        if (loading) return

        val maxPage = maxPageIndex()
        if (currentPageIndex >= maxPage) return

        currentPageIndex++
        reloadAndRebuild(event.inventory)
    }

    @ClickAction("previous-page")
    fun previousPage(event: InventoryClickEvent) {
        if (loading) return
        if (currentPageIndex <= 0) return

        currentPageIndex--
        reloadAndRebuild(event.inventory)
    }

    private fun reloadAndRebuild(inventory: Inventory) {
        loading = true

        scope.launch {
            val result = runCatching { data() }.getOrDefault(emptyList())

            withContext(Dispatchers.Main) {
                dataList = result
                loading = false
                rebuild(inventory)
            }
        }
    }

    protected fun maxPageIndex(): Int {
        val pageSize = paginatedSlots.size
        if (pageSize == 0) return 0
        return max(0, (dataList.size - 1) / pageSize)
    }

    protected fun rebuild(inventory: Inventory) {
        val configFile = owningPlugin.dataFolder.resolve("$BASE_MENU_FOLDER/$id.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val patternList = config.getOrWarn("pattern", emptyList<String>())

        val pattern = Array(patternList.size) { row ->
            patternList[row].toCharArray()
        }

        val result = buildItems(pattern, config)
        inventory.contents = result.items
    }

    private fun copyPersistentData(
        from: PersistentDataContainer,
        to: PersistentDataContainer
    ) {
        for (key in from.keys) {

            when {
                from.has(key, PersistentDataType.STRING) -> {
                    to.set(
                        key,
                        PersistentDataType.STRING,
                        from.get(key, PersistentDataType.STRING)!!
                    )
                }

                from.has(key, PersistentDataType.INTEGER) -> {
                    to.set(
                        key,
                        PersistentDataType.INTEGER,
                        from.get(key, PersistentDataType.INTEGER)!!
                    )
                }

                from.has(key, PersistentDataType.LONG) -> {
                    to.set(
                        key,
                        PersistentDataType.LONG,
                        from.get(key, PersistentDataType.LONG)!!
                    )
                }

                from.has(key, PersistentDataType.DOUBLE) -> {
                    to.set(
                        key,
                        PersistentDataType.DOUBLE,
                        from.get(key, PersistentDataType.DOUBLE)!!
                    )
                }

                from.has(key, PersistentDataType.FLOAT) -> {
                    to.set(
                        key,
                        PersistentDataType.FLOAT,
                        from.get(key, PersistentDataType.FLOAT)!!
                    )
                }

                from.has(key, PersistentDataType.BYTE) -> {
                    to.set(
                        key,
                        PersistentDataType.BYTE,
                        from.get(key, PersistentDataType.BYTE)!!
                    )
                }

                from.has(key, PersistentDataType.BYTE_ARRAY) -> {
                    to.set(
                        key,
                        PersistentDataType.BYTE_ARRAY,
                        from.get(key, PersistentDataType.BYTE_ARRAY)!!
                    )
                }

                from.has(key, PersistentDataType.INTEGER_ARRAY) -> {
                    to.set(
                        key,
                        PersistentDataType.INTEGER_ARRAY,
                        from.get(key, PersistentDataType.INTEGER_ARRAY)!!
                    )
                }

                from.has(key, PersistentDataType.LONG_ARRAY) -> {
                    to.set(
                        key,
                        PersistentDataType.LONG_ARRAY,
                        from.get(key, PersistentDataType.LONG_ARRAY)!!
                    )
                }

                from.has(key, PersistentDataType.TAG_CONTAINER) -> {
                    to.set(
                        key,
                        PersistentDataType.TAG_CONTAINER,
                        from.get(key, PersistentDataType.TAG_CONTAINER)!!
                    )
                }

                from.has(key, PersistentDataType.TAG_CONTAINER_ARRAY) -> {
                    to.set(
                        key,
                        PersistentDataType.TAG_CONTAINER_ARRAY,
                        from.get(key, PersistentDataType.TAG_CONTAINER_ARRAY)!!
                    )
                }
            }
        }
    }

}
