package dev.crafty.framework.api.data

/**
 * Key-value data store interface.
 */
interface DataStore {
    /**
     * Gets the value associated with the given key.
     * @param T The type of the value.
     * @param key The data key.
     * @return The value associated with the key, or null if not found.
     */
    suspend fun <T : Any> get(key: DataKey<T>): T?

    /**
     * Gets the value associated with the given key, or returns the default value if not found.
     * @param T The type of the value.
     * @param key The data key.
     * @param default The default value to return if the key is not found.
     * @return The value associated with the key, or the default value.
     */
    suspend fun <T : Any> getOrDefault(key: DataKey<T>, default: T): T =
        get(key) ?: default

    /**
     * Opens a transaction to perform multiple operations atomically.
     * @param block The block of operations to perform within the transaction.
     */
    suspend fun transaction(block: suspend Transaction.() -> Unit)
}