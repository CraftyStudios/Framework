package dev.crafty.framework.api.logs

/**
 * Interface for logging messages at various levels.
 */
interface Logger {
    /**
     * Logs a message at the specified log level.
     * @param level The log level.
     * @param message The message to log.
     * @param throwable An optional throwable associated with the log message.
     */
    fun log(level: LogLevel, message: String, throwable: Throwable? = null)

    /**
     * Logs an informational message.
     * @param message The message to log.
     */
    fun info(message: String) = log(LogLevel.INFO, message)

    /**
     * Logs a warning message.
     * @param message The message to log.
     */
    fun warn(message: String) = log(LogLevel.WARN, message)

    /**
     * Logs an error message.
     * @param message The message to log.
     * @param throwable An optional throwable associated with the error.
     */
    fun error(message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, message, throwable)

    /**
     * Logs a debug message.
     * @param message The message to log.
     */
    fun debug(message: String) = log(LogLevel.DEBUG, message)
}