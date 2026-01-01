package dev.crafty.framework.lib

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

object RuntimeAnalyzer {
    private const val LIBRARY_PLUGIN_NAME = "Framework"

    fun findCallingPlugin(): JavaPlugin? {
        var potentialPlugin: JavaPlugin? = null

        val stackTrace = Thread.currentThread().stackTrace
        for (i in 2 until stackTrace.size) {
            try {
                val callingClass = Class.forName(stackTrace[i].className)
                val classLoader = callingClass.classLoader

                if (classLoader != null && classLoader.toString().contains("PluginClassLoader")) {
                    val pluginName = extractPluginName(classLoader.toString())
                    val baseName = pluginName?.substringBefore(" ")
                    if (baseName != null) {
                        if (baseName == LIBRARY_PLUGIN_NAME) {
                            potentialPlugin = Bukkit.getPluginManager().getPlugin(baseName) as? JavaPlugin
                            continue // skip for now
                        } else {
                            return Bukkit.getPluginManager().getPlugin(baseName) as? JavaPlugin
                        }
                    }
                }
            } catch (ignored: ClassNotFoundException) {}
        }

        // the calling plugin is the library itself
        return potentialPlugin
    }

    private fun extractPluginName(classLoaderString: String): String? {
        //PluginClassLoader{plugin=ndbTest v1.0-SNAPSHOT, pluginEnabled=false}
        // plugin name would be ndbTest v1.0-SNAPSHOT
        val start = classLoaderString.indexOf("plugin=") + 7
        val end = classLoaderString.indexOf(", pluginEnabled=", start)
        if (start in 1..<end) {
            return classLoaderString.substring(start, end)
        }
        return null
    }
}