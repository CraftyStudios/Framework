package dev.crafty.framework

import dev.crafty.framework.bootstrap.BootstrapLoader
import org.bukkit.plugin.java.JavaPlugin

class Framework : JavaPlugin() {

    companion object {
        lateinit var instance: Framework
            private set
    }

    override fun onEnable() {
        instance = this

        BootstrapLoader.initialize()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
