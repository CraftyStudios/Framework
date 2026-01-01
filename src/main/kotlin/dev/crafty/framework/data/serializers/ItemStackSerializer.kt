package dev.crafty.framework.data.serializers

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object ItemStackSerializer : Serializer<ItemStack>() {
    override fun write(kryo: Kryo, output: Output, itemStack: ItemStack) {
        val baos = ByteArrayOutputStream()
        val boos = BukkitObjectOutputStream(baos)
        boos.writeObject(itemStack)
        boos.close()
        val bytes = baos.toByteArray()
        output.writeInt(bytes.size)
        output.writeBytes(bytes)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out ItemStack>): ItemStack {
        val length = input.readInt()
        val bytes = input.readBytes(length)
        val bais = ByteArrayInputStream(bytes)
        val bois = BukkitObjectInputStream(bais)
        val itemStack = bois.readObject() as ItemStack
        bois.close()
        return itemStack
    }
}