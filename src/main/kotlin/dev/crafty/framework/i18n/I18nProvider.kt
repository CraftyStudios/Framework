package dev.crafty.framework.i18n

import dev.crafty.framework.api.i18n.I18n
import dev.crafty.framework.api.i18n.I18nKey
import dev.crafty.framework.lib.RuntimeAnalyzer
import dev.crafty.framework.lib.colorize
import dev.crafty.framework.lib.plus
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

internal class I18nProvider : I18n {
    private val prefixes: MutableMap<JavaPlugin, Component> = mutableMapOf()

    override fun setPluginPrefix(prefix: Component) {
        RuntimeAnalyzer.findCallingPlugin()?.let { plugin ->
            prefixes[plugin] = prefix
        } ?: run {
            throw IllegalStateException("Could not determine calling plugin for setting i18n prefix.")
        }
    }

    override fun send(
        player: Player,
        key: I18nKey,
        vararg placeholders: Pair<String, Any>
    ) {
        RuntimeAnalyzer.findCallingPlugin()?.let { plugin ->
            val config = getLangConfig(plugin)
            player.sendMessage(
                getMessage(
                    config,
                    key,
                    placeholders.toMap(),
                    plugin
                )
            )
        }
    }

    override fun translate(
        player: Player,
        key: I18nKey,
        vararg placeholders: Pair<String, Any>
    ): Component {
        return RuntimeAnalyzer.findCallingPlugin()?.let { plugin ->
            val config = getLangConfig(plugin)
            getMessage(
                config,
                key,
                placeholders.toMap(),
                plugin
            )
        } ?: run {
            throw IllegalStateException("Could not determine calling plugin for i18n translation.")
        }
    }

    override fun broadcast(
        key: I18nKey,
        vararg placeholders: Pair<String, Any>
    ) {
        RuntimeAnalyzer.findCallingPlugin()?.let { plugin ->
            val config = getLangConfig(plugin)
            val message = getMessage(
                config,
                key,
                placeholders.toMap(),
                plugin
            )

            Bukkit.getOnlinePlayers().forEach { it.sendMessage(message) }
        }
    }

    private fun getMessage(
        config: FileConfiguration,
        key: I18nKey,
        placeholders: Map<String, Any>,
        plugin: JavaPlugin
    ): Component {
        val rawMessage = config.getString(key.path) ?: run {
            throw IllegalArgumentException("No translation found for key '$key.path' in plugin '${plugin.name}'")
        }

        placeholders.forEach { (key, value) ->
            rawMessage.replace("{$key}", value.toString())
        }

        val prefix = prefixes[plugin]
        return if (prefix != null) prefix + rawMessage.colorize() else rawMessage.colorize()
    }

    private fun getLangConfig(plugin: JavaPlugin): YamlConfiguration {
        return plugin.getResource("lang.yml")?.let { inputStream ->
            YamlConfiguration.loadConfiguration(inputStream.reader())
        } ?: YamlConfiguration()
    }
}