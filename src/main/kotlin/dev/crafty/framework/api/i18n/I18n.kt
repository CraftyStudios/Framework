package dev.crafty.framework.api.i18n

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/**
 * Internationalization module for sending translated messages to players.
 */
interface I18n {
    /**
     * Sets a prefix to be added to all translated messages.
     * @param prefix The prefix component to set.
     */
    fun setPluginPrefix(prefix: Component)

    /**
     * Sends a translated message to the specified player.
     * @param player The player to send the message to.
     * @param key The i18n key representing the message.
     * @param placeholders Optional placeholders to replace in the message.
     */
    fun send(player: Player, key: I18nKey, vararg placeholders: Pair<String, Any>)

    /**
     * Translates a message for the specified player and returns it as a Component.
     * @param player The player for whom the message is translated.
     * @param key The i18n key representing the message.
     * @param placeholders Optional placeholders to replace in the message.
     * @return The translated message as a Component.
     */
    fun translate(player: Player, key: I18nKey, vararg placeholders: Pair<String, Any>): Component

    /**
     * Broadcasts a translated message to all players on the server.
     * @param key The i18n key representing the message.
     * @param placeholders Optional placeholders to replace in the message.
     */
    fun broadcast(key: I18nKey, vararg placeholders: Pair<String, Any>)

    /**
     * Retrieves the raw translated message as a Component without any placeholders replaced.
     * Does not include the prefix.
     * @param key The i18n key representing the message.
     */
    fun getRaw(key: I18nKey): Component
}