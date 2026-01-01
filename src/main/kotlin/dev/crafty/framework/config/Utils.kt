package dev.crafty.framework.config

import dev.s7a.ktconfig.KtConfigLoader
import org.bukkit.plugin.java.JavaPlugin

inline fun <reified T> setupConfig(path: String, loader: KtConfigLoader<T>, instance: T, plugin: JavaPlugin): T {
    val file = plugin.dataFolder.resolve(path)

    if (!file.exists()) {
        loader.save(file, instance)
    }

    return loader.load(file)
}