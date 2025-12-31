package dev.crafty.framework.lib

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

internal object RuntimeAnalyzer {
    private const val LIBRARY_PLUGIN_NAME = "Framework"

    fun findCallingPlugin(): JavaPlugin? {
        val stackTrace = Thread.currentThread().stackTrace
        for (i in 2 until stackTrace.size) {
            try {
                val callingClass = Class.forName(stackTrace[i].className)
                val classLoader = callingClass.classLoader

                if (classLoader != null && classLoader.toString().contains("PluginClassLoader")) {
                    val pluginName = extractPluginName(classLoader.toString())
                    val baseName = pluginName?.substringBefore(" ")
                    if (baseName != null && baseName != LIBRARY_PLUGIN_NAME) {
                        return Bukkit.getPluginManager().getPlugin(baseName) as? JavaPlugin
                    }
                }
            } catch (ignored: ClassNotFoundException) {}
        }

        return null
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