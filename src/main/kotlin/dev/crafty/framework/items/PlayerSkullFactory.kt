import com.destroystokyo.paper.profile.PlayerProfile
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.koin.core.component.KoinComponent
import java.util.UUID

object PlayerSkullFactory : KoinComponent {
    fun setSkull(item: ItemStack, username: String) {
        println("Setting skull for username: $username, item: ${item.type}")
        if (item.type != Material.PLAYER_HEAD) return

        val skullMeta = item.itemMeta as SkullMeta

        val onlinePlayer = Bukkit.getPlayerExact(username)
        if (onlinePlayer != null) {
            skullMeta.playerProfile = onlinePlayer.playerProfile
            item.itemMeta = skullMeta
            println("Set skull using online player: $username")
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().plugins.first()) { _ ->
            try {
                val profile: PlayerProfile = Bukkit.createProfile(username)
                profile.complete(true)

                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().plugins.first()) { _ ->
                    skullMeta.playerProfile = profile
                    item.itemMeta = skullMeta
                    println("Set skull using fetched profile: ${profile.name}, UUID: ${profile.id}")
                }
            } catch (e: Exception) {
                println("Failed to fetch player profile for $username: ${e.message}")
            }
        }
    }
}