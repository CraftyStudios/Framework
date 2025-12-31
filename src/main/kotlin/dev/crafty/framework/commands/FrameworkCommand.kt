package dev.crafty.framework.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import dev.crafty.framework.Framework
import dev.crafty.framework.api.i18n.sendI18n
import dev.crafty.framework.keys.InfoKey
import org.bukkit.entity.Player

@CommandAlias("framework")
class FrameworkCommand : BaseCommand() {
    @Default
    fun onFrameworkCommand(player: Player) {
        player.sendI18n(InfoKey, "version" to Framework.instance.description.version)
    }
}