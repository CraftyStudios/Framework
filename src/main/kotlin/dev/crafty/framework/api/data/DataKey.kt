package dev.crafty.framework.api.data

import kotlin.reflect.KClass

data class DataKey<T : Any>(val name: String, val type: KClass<T>) {
    override fun toString(): String {
        return "DataKey<$name>"
    }
}

data class DataKeyPrefix<T : Any>(val prefix: String, val type: KClass<T>) {
    override fun toString(): String {
        return "DataKeyPrefix<$prefix>"
    }
}

inline fun <reified T : Any> key(name: String): DataKey<T> =
    DataKey(name, T::class)