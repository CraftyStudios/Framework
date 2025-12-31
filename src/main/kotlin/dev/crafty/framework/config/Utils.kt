package dev.crafty.framework.config

import dev.crafty.framework.Framework
import dev.s7a.ktconfig.KtConfigLoader

inline fun <reified T> setupConfig(path: String, loader: KtConfigLoader<T>, instance: T): T {
    val file = Framework.instance.dataFolder.resolve(path)
    if (!file.exists()) {
        loader.save(file, instance)
    }

    return loader.load(file)
}