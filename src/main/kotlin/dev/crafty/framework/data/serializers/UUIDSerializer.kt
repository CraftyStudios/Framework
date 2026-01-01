package dev.crafty.framework.data.serializers

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import java.util.UUID

object UUIDSerializer : Serializer<UUID>() {
    override fun write(
        p0: Kryo?,
        p1: Output?,
        p2: UUID?
    ) {
        p1?.writeLong(p2!!.mostSignificantBits)
        p2?.let { p1?.writeLong(it.leastSignificantBits) }
    }

    override fun read(
        p0: Kryo?,
        p1: Input?,
        p2: Class<out UUID?>?
    ): UUID {
        val mostSigBits = p1!!.readLong()
        val leastSigBits = p1.readLong()
        return UUID(mostSigBits, leastSigBits)
    }
}