package dev.crafty.framework.lib

import dev.crafty.framework.api.menu.Menu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import java.util.regex.Pattern

val minimessage = MiniMessage.builder()
    .tags(
        TagResolver.builder()
                .resolver(StandardTags.color())
                .resolver(StandardTags.decorations())
                .build()
    )
    .build()

fun String.colorize(): Component =
    minimessage.deserialize(this)
        .decoration(TextDecoration.ITALIC, false)

fun List<String>.colorize(): List<Component> =
    this.map { it.colorize() }

operator fun Component.plus(other: Component): Component =
    this.append(other)


fun String.replaceInString(placeholders: Map<String, Any>): String {
    var result = this
    for ((key, value) in placeholders) {
        result = result.replace("{$key}", value.toString())
    }
    return result
}

fun List<String>.replaceInStringList(placeholders: Map<String, Any>): List<String> =
    this.map { it.replaceInString(placeholders) }

fun Component.replaceInComponent(placeholders: Map<String, Any>): Component {
    var result = this

    for ((key, value) in placeholders) {
        val literal = Pattern.quote("{$key}")

        val replacement: Component = when (value) {
            is Component -> value
            is String -> minimessage.deserialize(value)
            else -> Component.text(value.toString())
        }

        result = result.replaceText {
            it.match(literal)
                .replacement(replacement)
        }
    }

    return result
}

fun List<Component>.replaceInComponentList(
    placeholders: Map<String, Any>
): List<Component> {
    return this.mapNotNull { originalLine ->
        val replaced = originalLine.replaceInComponent(placeholders)

        if (replaced == Menu.REMOVE_LINE_COMPONENT) {
            null
        } else {
            replaced
        }
    }
}