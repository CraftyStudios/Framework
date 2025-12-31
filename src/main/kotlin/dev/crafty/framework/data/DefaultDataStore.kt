package dev.crafty.framework.data

import dev.crafty.framework.api.data.DataKey
import dev.crafty.framework.api.data.DataStore
import dev.crafty.framework.api.data.Transaction
import dev.crafty.framework.data.providers.CacheProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class DefaultDataStore : DataStore {
    @Suppress("UNCHECKED_CAST")
    private val provider = CacheProvider(
            persistence = StorageProviderManager.getStorageProvider()
        )

    private val transactionDispatcher = Dispatchers.Default
    private val mutex = Mutex()

    override suspend fun <T : Any> get(key: DataKey<T>): T? {
        return provider.get(key)
    }

    override suspend fun transaction(block: suspend Transaction.() -> Unit) {
        withContext(transactionDispatcher) {
            // local transaction buffer
            val transactionBuffer = mutableMapOf<DataKey<*>, Any?>()

            val tx = object : Transaction {
                override suspend fun <T : Any> set(key: DataKey<T>, value: T) {
                    transactionBuffer[key] = value
                }

                override suspend fun <T : Any> remove(key: DataKey<T>) {
                    transactionBuffer[key] = null // mark for removal from the db
                }

                override suspend fun <T : Any> get(key: DataKey<T>): T? {
                    @Suppress("UNCHECKED_CAST")
                    return if (transactionBuffer.containsKey(key)) {
                        transactionBuffer[key] as T?
                    } else {
                        this@DefaultDataStore.get(key)
                    }
                }

                override suspend fun <T : Number> increment(key: DataKey<T>) {
                    @Suppress("UNCHECKED_CAST")
                    val newValue: T = when (val currentValue = get(key)) {
                        is Int -> (currentValue + 1) as T
                        is Long -> (currentValue + 1L) as T
                        is Float -> (currentValue + 1f) as T
                        is Double -> (currentValue + 1.0) as T
                        else -> throw IllegalArgumentException("Unsupported number type for increment")
                    }

                    set(key, newValue)
                }

                override suspend fun <T : Any> update(key: DataKey<T>, transform: (T?) -> T) {
                    set(key, transform(get(key)))
                }
            }

            try {
                block(tx) // run transaction

                // apply buffered changes
                mutex.withLock {
                    for ((key, value) in transactionBuffer) {
                        if (value == null) {
                            // if marked null, remove the key
                            provider.remove(key)
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            provider.set(key as DataKey<Any>, value)
                        }
                    }
                }
            } catch (t: Throwable) {
                // discards the buffer effectively rolling back the transaction
                throw t;
            }
        }
    }
}