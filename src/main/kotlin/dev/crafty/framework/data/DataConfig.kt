package dev.crafty.framework.data

import dev.s7a.ktconfig.Comment
import dev.s7a.ktconfig.KtConfig

@KtConfig(hasDefault = true)
data class DataConfig(
    @Comment("Default storage provider to use.")
    var provider: StorageProviderType = StorageProviderType.POSTGRES,

    @Comment("PostgreSQL database configuration.")
    var postgres: PostgresConfig = PostgresConfig(),

    @Comment("Cache configuration.")
    var cache: CacheConfig = CacheConfig()
)

enum class StorageProviderType {
    POSTGRES
}

@KtConfig(hasDefault = true)
data class PostgresConfig(
    @Comment("The hostname or IP address of the PostgreSQL server.")
    var host: String = "localhost",

    @Comment("The port of the PostgreSQL server.")
    var port: Int = 5432,

    @Comment("The database name of the PostgreSQL server.")
    var database: String = "mydatabase",

    @Comment("The username for the PostgreSQL database.")
    var username: String = "myuser",

    @Comment("The password for the PostgreSQL database.")
    var password: String = "mypassword"
) {
    fun toJdbcUrl(): String {
        return "jdbc:postgresql://$host:$port/$database"
    }
}

@KtConfig(hasDefault = true)
data class CacheConfig(
    @Comment("The maximum size of the cache.")
    var maxSize: Long = 10_000,

    @Comment("The expiration time for cache entries in seconds.")
    var expirationSeconds: Long = 3600
)