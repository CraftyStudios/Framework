package dev.crafty.framework.data

import com.esotericsoftware.kryo.kryo5.Serializer
import dev.crafty.framework.data.serializers.ItemStackSerializer
import dev.crafty.framework.data.serializers.UUIDSerializer
import org.bukkit.inventory.ItemStack
import java.util.UUID
import kotlin.reflect.KClass

object SerializerManager {
    val serializers: MutableMap<KClass<*>, Serializer<*>> = mutableMapOf()

    fun <T : Any> registerSerializer(clazz: KClass<T>, serializer: Serializer<T>) {
        serializers[clazz] = serializer
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getSerializer(clazz: KClass<T>): Serializer<T>? {
        return serializers[clazz] as? Serializer<T>
    }

    fun allSerializers(): Map<KClass<*>, Serializer<*>> {
        return serializers.toMap()
    }

    init {
        registerSerializer(ItemStack::class, ItemStackSerializer)
        registerSerializer(UUID::class, UUIDSerializer)
    }
}