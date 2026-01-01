package dev.crafty.framework.api.menu.types

import dev.crafty.framework.Framework
import dev.crafty.framework.api.menu.Menu
import dev.crafty.framework.api.tasks.now
import dev.crafty.framework.lib.colorize
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

abstract class LoadingMenu<T>(player: Player) : Menu(player) {

    private var isLoading = true
    protected var loadedData: T? = null
        private set

    protected abstract suspend fun supplyData(): T

    override fun open() {
        isLoading = true
        loadedData = null

        val inv = buildMenu()
        player.openInventory(inv)

        Framework.instance.scope.launch { try {
                loadedData = supplyData()
                isLoading = false
                now {
                    val newInv = buildMenu()
                    player.openInventory(newInv)
                }
            } catch (e: Exception) {
                owningPlugin.logger.warning("Failed to load data for menu '$id': ${e.message}")
                withContext(Dispatchers.Main) {
                    player.closeInventory()
                }
            }
        }
    }

    override fun buildMenu(): Inventory {
        if (isLoading) {
            val rows = 6
            val title = "Loading..."
            val inventory = createInventoryContainer(ContainerType.CHEST, rows, title.colorize())
            val items = Array(rows * 9) {
                ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
                    itemMeta = itemMeta?.apply {
                        displayName(Component.text("Loading..."))
                    }
                }
            }
            inventory.contents = items
            return inventory
        } else {
            return super.buildMenu()
        }
    }
}