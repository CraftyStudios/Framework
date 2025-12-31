package dev.crafty.framework.api.logs

/**
 * Enum representing different log levels.
 */
enum class LogLevel {
    /**
     * Informational messages.
     */
    INFO,

    /**
     * Warning messages.
     */
    WARN,

    /**
     * Error messages.
     */
    ERROR,

    /**
     * Debugging messages. Only shown when debug mode is enabled.
     */
    DEBUG
}