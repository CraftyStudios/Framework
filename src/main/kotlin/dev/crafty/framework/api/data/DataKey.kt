package dev.crafty.framework.api.data

import kotlin.reflect.KClass

data class DataKey<T : Any>(val name: String, val type: KClass<T>) {
    override fun toString(): String {
        return "DataKey<$name>"
    }
}

inline fun <reified T : Any> key(name: String): DataKey<T> =
    DataKey(name, T::class)