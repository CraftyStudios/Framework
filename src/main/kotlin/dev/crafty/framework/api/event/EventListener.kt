package dev.crafty.framework.api.event

import dev.crafty.framework.events.GlobalEventRouter
import org.bukkit.event.Event

/**
 * Registers an event listener for the specified event type.
 *
 * @param E The type of event to listen for.
 * @param listener The function to be called when the event is fired.
 * @return The listener id (used to cancel it).
 */
inline fun <reified E : Event> on(noinline listener: (E) -> Unit): Int =
    GlobalEventRouter.registerListener(E::class.java, listener)

/**
 * Unregisters the specified event listener.
 *
 * @param id The id of the listener to unregister.
 */
fun unregisterListener(listener: Int) =
    GlobalEventRouter.unregisterListener(listener)