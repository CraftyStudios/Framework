package dev.crafty.framework.lib

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags

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


operator fun Component.plus(other: Component): Component =
    this.append(other)