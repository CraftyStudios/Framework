package dev.crafty.framework.api.i18n

import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Holder object to access the I18n instance via Koin.
 */
private object I18nHolder : KoinComponent {
    val i18n: I18n by inject()
}

/**
 * Sends a localized message to the player using the I18n system.
 * @param key The I18nKey representing the message to send.
 * @param args Optional placeholders to replace in the message.
 */
fun Player.sendI18n(key: I18nKey, vararg args: Pair<String, Any>) {
    I18nHolder.i18n.send(this, key, *args)
}