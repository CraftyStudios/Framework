package dev.crafty.framework.api.lifecycle

import dev.crafty.framework.bootstrap.FrameworkPluginLoader
import org.bukkit.plugin.java.JavaPlugin

/**
 * Base class for all framework plugins.
 */
abstract class FrameworkPlugin(
    val pluginName: String
) : JavaPlugin() {
    final override fun onEnable() {
        FrameworkPluginLoader.loadPlugin(this)

        initialize()
    }

    final override fun onDisable() {

        shutdown()
    }

    /**
     * Called when the plugin is being initialized.
     */
    abstract fun initialize()

    /**
     * Called when the plugin is being shut down.
     */
    abstract fun shutdown()
}