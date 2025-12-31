package dev.crafty.framework.api.data

/**
 * Interface for a storage provider that can store and retrieve data using keys.
 */
internal interface StorageProvider {
    /**
     * Shuts down the storage provider.
     */
    fun shutdown()

    /**
     * Gets the value associated with the given key.
     * @param T The type of the value.
     * @param key The data key.
     */
    fun <T : Any> get(key: DataKey<T>): T?

    /**
     * Sets the value for the given key.
     * @param T The type of the value.
     * @param key The data key.
     * @param value The value to set.
     */
    fun <T : Any> set(key: DataKey<T>, value: T)

    /**
     * Removes the value associated with the given key.
     * @param T The type of the value.
     * @param key The data key.
     */
    fun <T : Any> remove(key: DataKey<T>)
}