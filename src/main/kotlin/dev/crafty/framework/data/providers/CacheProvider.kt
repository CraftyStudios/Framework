package dev.crafty.framework.data.providers

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.crafty.framework.api.data.DataKey
import dev.crafty.framework.api.data.StorageProvider
import dev.crafty.framework.api.logs.Logger
import dev.crafty.framework.data.DataConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

internal class CacheProvider(
    private val persistence: StorageProvider
) : StorageProvider, KoinComponent {
    private val logger: Logger by inject()
    private val config: DataConfig by inject()

    private var cache: Cache<String, Any> = Caffeine.newBuilder()
        .maximumSize(config.cache.maxSize)
        .expireAfterAccess(Duration.ofSeconds(config.cache.expirationSeconds))
        .build()

    override fun shutdown() {
        logger.debug("Cache shutdown, invalidating all entries")
        cache.invalidateAll()
    }

    override fun <T : Any> get(key: DataKey<T>): T? {
        // try to hit cache first
        @Suppress("UNCHECKED_CAST")
        val cachedValue = cache.getIfPresent(key.name) as T?
        if (cachedValue != null) {
            logger.debug("Cache hit for key: $key")
            return cachedValue
        } else {
            logger.debug("Cache miss for key: $key, fetching from persistence")
            val persistedValue = persistence.get(key)

            // hit; store in cache
            if (persistedValue != null) {
                cache.put(key.name, persistedValue as Any)
            }

            return persistedValue
        }
    }

    override fun <T : Any> set(key: DataKey<T>, value: T) {
        logger.debug("Setting value for key: $key in persistence and updating cache")

        // set in persistence
        persistence.set(key, value)

        // update cache
        cache.put(key.name, value as Any)
    }

    override fun <T : Any> remove(key: DataKey<T>) {
        logger.debug("Removing cache for key: $key")

        // remove from persistence
        persistence.remove(key)

        // invalidate cache
        cache.invalidate(key.name)
    }
}