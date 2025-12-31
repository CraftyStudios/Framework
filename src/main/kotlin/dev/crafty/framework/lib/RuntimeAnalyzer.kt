package dev.crafty.framework.lib

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

internal object RuntimeAnalyzer {
    fun findCallingPlugin(): JavaPlugin? {
        println("Finding calling plugin")
        val stackTrace = Thread.currentThread().stackTrace
        for (i in 2..<stackTrace.size) {
            try {
                val callingClass = Class.forName(stackTrace[i].className)
                val classLoader = callingClass.getClassLoader()
                println("Checking class loader $classLoader")
                if (classLoader != null &&
                    classLoader.toString().contains("PluginClassLoader") && !classLoader.toString()
                        .contains("Framework")
                ) {
                    val pluginName = extractPluginName(classLoader.toString())
                    if (pluginName != null) {
                        println("Found calling plugin $pluginName")
                        return Bukkit.getPluginManager().getPlugin(pluginName) as? JavaPlugin
                    }
                }
            } catch (ignored: ClassNotFoundException) {
            }
        }

        println("Could not find calling plugin")
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