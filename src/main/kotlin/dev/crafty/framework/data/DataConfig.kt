package dev.crafty.framework.data

import dev.s7a.ktconfig.Comment
import dev.s7a.ktconfig.KtConfig

@KtConfig(hasDefault = true)
data class DataConfig(
    @Comment("Default storage provider to use.")
    val provider: StorageProviderType = StorageProviderType.POSTGRES,

    @Comment("PostgreSQL database configuration.")
    val postgres: PostgresConfig = PostgresConfig(),

    @Comment("Cache configuration.")
    val cache: CacheConfig = CacheConfig()
)

enum class StorageProviderType {
    POSTGRES
}

@KtConfig(hasDefault = true)
data class PostgresConfig(
    @Comment("The hostname or IP address of the PostgreSQL server.")
    val host: String = "localhost",

    @Comment("The port of the PostgreSQL server.")
    val port: Int = 5432,

    @Comment("The database name of the PostgreSQL server.")
    val database: String = "mydatabase",

    @Comment("The username for the PostgreSQL database.")
    val username: String = "myuser",

    @Comment("The password for the PostgreSQL database.")
    val password: String = "mypassword"
) {
    fun toJdbcUrl(): String {
        return "jdbc:postgresql://$host:$port/$database"
    }
}

@KtConfig(hasDefault = true)
data class CacheConfig(
    @Comment("The maximum size of the cache.")
    val maxSize: Long = 10_000,

    @Comment("The expiration time for cache entries in seconds.")
    val expirationSeconds: Long = 3600
)