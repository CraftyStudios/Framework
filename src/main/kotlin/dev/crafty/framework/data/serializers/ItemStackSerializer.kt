package dev.crafty.framework.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

object ItemStackSerializer : KSerializer<ItemStack> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ItemStack", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ItemStack) {
        val baos = ByteArrayOutputStream()
        BukkitObjectOutputStream(baos).use { boos ->
            boos.writeObject(value)
        }
        encoder.encodeString(Base64.getEncoder().encodeToString(baos.toByteArray()))
    }

    override fun deserialize(decoder: Decoder): ItemStack {
        val bytes = Base64.getDecoder().decode(decoder.decodeString())
        return BukkitObjectInputStream(ByteArrayInputStream(bytes)).use { bois ->
            bois.readObject() as ItemStack
        }
    }
}