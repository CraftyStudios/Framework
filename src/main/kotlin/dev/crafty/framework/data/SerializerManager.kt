package dev.crafty.framework.data

import dev.crafty.framework.data.serializers.ItemStackSerializer
import dev.crafty.framework.data.serializers.UUIDSerializer
import kotlinx.serialization.KSerializer
import org.bukkit.inventory.ItemStack
import java.util.UUID
import kotlin.reflect.KClass

object SerializerManager {
    val serializers: MutableMap<KClass<*>, KSerializer<*>> = mutableMapOf()

    fun <T : Any> registerSerializer(clazz: KClass<T>, serializer: KSerializer<T>) {
        serializers[clazz] = serializer
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getSerializer(clazz: KClass<T>): KSerializer<T>? {
        return serializers[clazz] as? KSerializer<T>
    }

    fun allSerializers(): Map<KClass<*>, KSerializer<*>> {
        return serializers.toMap()
    }

    init {
        registerSerializer(ItemStack::class, ItemStackSerializer)
        registerSerializer(UUID::class, UUIDSerializer)
    }
}