package dev.crafty.framework.events

import dev.crafty.framework.Framework
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.plugin.EventExecutor
import org.reflections.Reflections

object GlobalEventRouter : Listener {
    private var plugin = Framework.instance
    private val ignoredEvents: Set<Class<out Event>> = setOf(
        PlayerLoginEvent::class.java // causes HorriblePlayerLoginEventHack warnings, use PlayerJoinEvent instead
    )

    private val listeners = HashSet<ListenerEntry<*>>()

    init {
        // search event classes
        val reflections = Reflections("org.bukkit")
        val eventClasses: Set<Class<out Event>> = reflections.getSubTypesOf(Event::class.java)
            .filter { clazz ->
                clazz.declaredFields.any { field ->
                    field.type.name.endsWith("HandlerList")
                }
            }
            .toSet()

        // register events
        val eventExecutor = EventExecutor { _, event ->
            onEvent(event)
        }

        eventClasses.forEach { clazz ->
            if (ignoredEvents.contains(clazz)) {
                return@forEach
            }

            plugin.server.pluginManager.registerEvent(
                clazz,
                this,
                EventPriority.MONITOR,
                eventExecutor,
                plugin
            )
        }
    }

    fun <T : Event> registerListener(eventClass: Class<T>, listener: (T) -> Unit): Int {
        val entry = ListenerEntry(
            id = listener.hashCode(),
            eventClass = eventClass,
            listener = listener
        )

        listeners.add(entry)

        return entry.id
    }

    fun unregisterListener(id: Int) {
        listeners.removeIf { it.id == id }
    }

    private fun onEvent(event: Event) {
        val eventClass = event.javaClass
        listeners.forEach { entry ->
            if (entry.eventClass == eventClass) {
                @Suppress("UNCHECKED_CAST")
                (entry.listener as (Event) -> Unit).invoke(event)
            }
        }
    }

    data class ListenerEntry<T>(
        val id: Int,
        val eventClass: Class<T>,
        val listener: (T) -> Unit
    )
}