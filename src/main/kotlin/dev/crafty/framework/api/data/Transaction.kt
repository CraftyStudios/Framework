package dev.crafty.framework.api.data

/**
 * Interface representing a transaction for batch data operations.
 */
interface Transaction {
    /**
     * Sets the value for the given key.
     * @param T The type of the value.
     * @param key The data key.
     * @param value The value to set.
     */
    suspend fun <T : Any> set(key: DataKey<T>, value: T)

    /**
     * Removes the value associated with the given key.
     * @param T The type of the value.
     * @param key The data key.
     */
    suspend fun <T : Any> remove(key: DataKey<T>)

    /**
     * Gets the value associated with the given key.
     * @param T The type of the value.
     * @param key The data key.
     * @return The value associated with the key, or null if not found.
     */
    suspend fun <T : Any> get(key: DataKey<T>): T?

    /**
     * Increments the numeric value associated with the given key.
     * @param T The type of the numeric value.
     * @param key The data key.
     */
    suspend fun <T : Number> increment(key: DataKey<T>)

    /**
     * Updates the value associated with the given key using the provided transform function.
     * @param T The type of the value.
     * @param key The data key.
     * @param transform The function to transform the current value.
     */
    suspend fun <T : Any> update(key: DataKey<T>, transform: (T?) -> T)
}