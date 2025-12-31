package dev.crafty.framework.bootstrap

import dev.crafty.framework.api.lifecycle.FrameworkPlugin

internal object FrameworkPluginLoader {
    val loadedPlugins: MutableList<FrameworkPlugin> = mutableListOf()

    fun loadPlugin(plugin: FrameworkPlugin) {
        loadedPlugins += plugin
    }
}