package dev.crafty.framework.api.menu

/**
 * Annotation to mark a function as a click action handler in a menu.
 * @param id The unique identifier for the click action.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ClickAction(val id: String)
