package dev.crafty.framework.api.units

/**
 * Base class for value-based units.
 * All units normalize themselves to a base unit.
 */
@JvmInline
value class UnitValue(
    val base: Long
)