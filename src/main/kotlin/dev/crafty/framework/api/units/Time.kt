package dev.crafty.framework.api.units

@JvmInline
value class Time(val ticks: Long) : Comparable<Time> {

    operator fun plus(other: Time) = Time(ticks + other.ticks)
    operator fun minus(other: Time) = Time(ticks - other.ticks)
    operator fun times(multiplier: Long) = Time(ticks * multiplier)
    operator fun div(divisor: Long) = Time(ticks / divisor)

    override fun compareTo(other: Time): Int =
        ticks.compareTo(other.ticks)

    fun inTicks(): Long = ticks
    fun inSeconds(): Double = ticks / 20.0
    fun inMinutes(): Double = ticks / (20.0 * 60)
    fun inHours(): Double = ticks / (20.0 * 60 * 60)

    override fun toString(): String = "$ticks ticks"
}

val Int.ticks: Time get() = Time(this.toLong())

val Int.seconds: Time get() = Time(this.toLong() * 20)
val Int.minutes: Time get() = Time(this.toLong() * 20 * 60)
val Int.hours: Time get() = Time(this.toLong() * 20 * 60 * 60)
