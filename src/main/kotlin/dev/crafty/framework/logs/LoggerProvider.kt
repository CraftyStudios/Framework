package dev.crafty.framework.logs

import dev.crafty.framework.Framework
import dev.crafty.framework.api.logs.LogLevel
import dev.crafty.framework.api.logs.Logger
import dev.crafty.framework.lib.RuntimeAnalyzer
import java.io.File

internal class LoggerProvider : Logger {
    private var localFile: File = Framework.instance.dataFolder.resolve("logs/log-${System.currentTimeMillis()}.log")

    init {
        localFile.parentFile?.mkdirs()
        localFile.createNewFile()
    }

    override fun log(
        level: LogLevel,
        message: String,
        throwable: Throwable?
    ) {
        val pluginCalling = RuntimeAnalyzer.findCallingPlugin() ?: "Unknown Plugin"

        val colorCode = when (level) {
            LogLevel.ERROR -> "\u001B[31m" // Red
            LogLevel.WARN -> "\u001B[33m" // Yellow
            LogLevel.INFO -> "\u001B[32m" // Green
            LogLevel.DEBUG -> "\u001B[36m" // Cyan
            else -> "\u001B[37m" // White
        }
        val reset = "\u001B[0m"

        val logMessage = "$colorCode[${level.name}]$reset [$pluginCalling]: $message" + (throwable?.let { "\n${it.stackTraceToString()}" } ?: "")

        println(logMessage)
        localFile.appendText(logMessage.replace(Regex("\u001B\\[[0-9;]*m"), "") + "\n")
    }
}