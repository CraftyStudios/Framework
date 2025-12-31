package dev.crafty.framework.api.tasks

import dev.crafty.framework.Framework
import dev.crafty.framework.api.units.Time
import dev.crafty.framework.api.units.ticks
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

/**
 * Schedules a repeating task to run every [time] ticks, with an optional [delay] before the first execution.
 * The task can be run asynchronously if [async] is set to true.
 *
 * @param time The interval between task executions.
 * @param delay The delay before the first execution.
 * @param async Whether to run the task asynchronously.
 * @param task The task to be executed.
 * @return The scheduled BukkitTask.
 */
inline fun every(time: Time, delay: Time = 0.ticks, async: Boolean = false, crossinline task: () -> Unit): BukkitTask {
    return if (async) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
            Framework.instance, Runnable { task() }, delay.ticks, time.ticks
        )
    } else {
        Bukkit.getScheduler().runTaskTimer(
            Framework.instance, Runnable { task() }, delay.ticks, time.ticks
        )
    }
}

/**
 * Schedules a task to run after a specified [time] delay.
 * The task can be run asynchronously if [async] is set to true.
 *
 * @param time The delay before the task execution.
 * @param async Whether to run the task asynchronously.
 * @param task The task to be executed.
 * @return The scheduled BukkitTask.
 */
inline fun later(time: Time, async: Boolean = false, crossinline task: () -> Unit): BukkitTask {
    return if (async) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(
            Framework.instance, Runnable { task() }, time.ticks
        )
    } else {
        Bukkit.getScheduler().runTaskLater(
            Framework.instance, Runnable { task() }, time.ticks
        )
    }
}

/**
 * Schedules a task to run immediately.
 * The task can be run asynchronously if [async] is set to true.
 *
 * @param async Whether to run the task asynchronously.
 * @param task The task to be executed.
 * @return The scheduled BukkitTask.
 */
inline fun now(async: Boolean = false, crossinline task: () -> Unit): BukkitTask {
    return if (async) {
        Bukkit.getScheduler().runTaskAsynchronously(
            Framework.instance, Runnable { task() }
        )
    } else {
        Bukkit.getScheduler().runTask(
            Framework.instance, Runnable { task() }
        )
    }
}

/**
 * Cancels a scheduled task with the given [taskId].
 *
 * @param taskId The ID of the task to cancel.
 */
fun cancelTask(taskId: Int) {
    Bukkit.getScheduler().cancelTask(taskId)
}