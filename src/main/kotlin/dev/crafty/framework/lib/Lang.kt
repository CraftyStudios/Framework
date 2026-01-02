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
                .resolver(StandardTags.reset())
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


fun String.replaceInString(placeholders: Map<String, Component>): String {
    var result = this
    for ((key, value) in placeholders) {
        result = result.replace("{$key}", value.text())
    }
    return result
}

fun List<String>.replaceInStringList(placeholders: Map<String, Component>): List<String> =
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

        if (replaced.containsRemoveLine()) {
            null
        } else {
            replaced
        }
    }
}

fun Component.text(): String {
    if (this is TextComponent) {
        return this.content()
    }

    return this.children().joinToString("") { it.text() }
}

fun Component.containsRemoveLine(): Boolean {
    if (this is TextComponent && this.content() == "REMOVE_LINE") {
        return true
    }

    return this.children().any { it.containsRemoveLine() }
}

fun Component.isRemoveLine(): Boolean {
    return this.children().isEmpty()
            && this is TextComponent
            && this.content() == "REMOVE_LINE"
}