package dev.crafty.framework.config

import dev.crafty.framework.lib.RuntimeAnalyzer
import dev.s7a.ktconfig.KtConfigLoader

inline fun <reified T> setupConfig(path: String, loader: KtConfigLoader<T>, instance: T): T {
    val callingPlugin = RuntimeAnalyzer.findCallingPlugin() ?: throw IllegalStateException("Could not determine calling plugin for config setup.")
    val file = callingPlugin.dataFolder.resolve(path)

    if (!file.exists()) {
        loader.save(file, instance)
    }

    return loader.load(file)
}