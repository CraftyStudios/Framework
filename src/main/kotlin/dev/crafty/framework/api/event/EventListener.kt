package dev.crafty.framework.api.event

import dev.crafty.framework.Framework
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener

/**
 * Registers an event listener for the specified event type.
 *
 * @param E The type of event to listen for.
 * @param listener The function to be called when the event is fired.
 * @return The registered listener.
 */
inline fun <reified E : Event> on(noinline listener: (E) -> Unit): Listener {
    val listener = object : Listener {
        @EventHandler
        fun onEvent(event: E) {
            listener(event)
        }
    }

    Framework.instance.server.pluginManager.registerEvents(
        listener,
        Framework.instance
    )

    return listener
}

/**
 * Unregisters the specified event listener.
 *
 * @param listener The listener to unregister.
 */
fun unregisterListener(listener: Listener) {
    HandlerList.unregisterAll(listener)
}