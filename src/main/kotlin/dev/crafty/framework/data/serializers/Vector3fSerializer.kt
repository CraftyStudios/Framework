package dev.crafty.framework.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.joml.Vector3f

object Vector3fSerializer : KSerializer<Vector3f> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Vector3f", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Vector3f) {
        val str = "${value.x},${value.y},${value.z}"
        encoder.encodeString(str)
    }

    override fun deserialize(decoder: Decoder): Vector3f {
        val str = decoder.decodeString()
        val parts = str.split(",")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid Vector3f format: $str")
        }
        val x = parts[0].toFloat()
        val y = parts[1].toFloat()
        val z = parts[2].toFloat()
        return Vector3f(x, y, z)
    }
}