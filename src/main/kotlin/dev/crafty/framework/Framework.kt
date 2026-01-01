package dev.crafty.framework

import co.aikar.commands.PaperCommandManager
import dev.crafty.framework.bootstrap.BootstrapLoader
import dev.crafty.framework.commands.FrameworkCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.bukkit.plugin.java.JavaPlugin

class Framework : JavaPlugin() {

    companion object {
        lateinit var instance: Framework
            private set
    }

    lateinit var commandManager: PaperCommandManager
    lateinit var scope: CoroutineScope

    override fun onEnable() {
        instance = this
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        saveResource("lang.yml", false)

        commandManager = PaperCommandManager(this)
        commandManager.registerCommand(FrameworkCommand())

        BootstrapLoader.initialize()
    }

    override fun onDisable() {

    }
}
