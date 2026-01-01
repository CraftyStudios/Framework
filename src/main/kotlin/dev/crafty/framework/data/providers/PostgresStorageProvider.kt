package dev.crafty.framework.data.providers

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.crafty.framework.api.data.DataKey
import dev.crafty.framework.api.data.StorageProvider
import dev.crafty.framework.api.logs.Logger
import dev.crafty.framework.data.DataConfig
import dev.crafty.framework.data.SerializerManager
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class)
internal class PostgresStorageProvider : StorageProvider, KoinComponent {
    private val logger: Logger by inject()
    private val config: DataConfig by inject()

    private var ds: HikariDataSource
    private val protoBuf: ProtoBuf

    init {
        logger.debug("Setting up Postgres storage provider")
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.postgres.toJdbcUrl()
            username = config.postgres.username
            password = config.postgres.password
        }

        ds = HikariDataSource(hikariConfig)

        ds.connection.use { conn ->
            if (conn.isValid(5)) {
                logger.info("Successfully connected to Postgres database")
            } else {
                logger.error("Failed to connect to Postgres database")
                throw IllegalStateException("Could not connect to Postgres database")
            }

            logger.debug("Setting up framework data table")
            val stmt = conn.prepareStatement(
                """
                CREATE TABLE IF NOT EXISTS framework_data_store (
                    framework_data_key TEXT PRIMARY KEY,
                    framework_data_value BYTEA NOT NULL
                );
            """.trimIndent()
            )

            stmt.execute()
            stmt.close()
        }

        val protoModule = SerializersModule {
            SerializerManager.allSerializers().forEach { (kClass, serializer) ->
                logger.debug("Registered serializer for ${kClass.simpleName} in ProtoBuf module")
                @Suppress("UNCHECKED_CAST")
                contextual(kClass as KClass<Any>, serializer as KSerializer<Any>)
            }
        }

        protoBuf = ProtoBuf {
            serializersModule = protoModule
        }
    }

    override fun shutdown() {
        logger.debug("Shutting down Postgres storage provider")
        ds.close()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: DataKey<T>): T? {
        logger.debug("Getting value for $key")

        return ds.connection.use { conn ->
            val stmt = conn.prepareStatement(
                """
                SELECT framework_data_value
                FROM framework_data_store
                WHERE framework_data_key = ?;
            """.trimIndent()
            )

            stmt.setString(1, key.name)

            val rs = stmt.executeQuery()
            val value = if (rs.next()) {
                var value: T

                try {
                    val binary = rs.getObject("framework_data_value") as ByteArray
                    val serializer = SerializerManager.getSerializer(key.type)
                        ?: serializer(key.type.java)

                    value = protoBuf.decodeFromByteArray(serializer, binary) as T
                } catch (ex: Exception) {
                    logger.error("Failed to cast value for $key from database", ex)
                    throw ex
                }

                logger.debug("Found value for $key: $value")
                value
            } else {
                logger.debug("No value found for $key")
                // key not found
                null
            }

            rs.close()
            stmt.close()

            value
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> set(key: DataKey<T>, value: T) {
        logger.debug("Setting value for $key")

        ds.connection.use { conn ->
            conn.prepareStatement(
                """
            INSERT INTO framework_data_store (framework_data_key, framework_data_value)
            VALUES (?, ?)
            ON CONFLICT (framework_data_key)
            DO UPDATE SET framework_data_value = EXCLUDED.framework_data_value;
            """.trimIndent()
            ).use { stmt ->
                val serializer = SerializerManager.getSerializer(key.type)
                    ?: serializer(key.type.java) as kotlinx.serialization.KSerializer<T>

                val bytes = protoBuf.encodeToByteArray(serializer, value)

                stmt.setString(1, key.name)
                stmt.setBytes(2, bytes)
                stmt.executeUpdate()
            }
        }
    }


    override fun <T : Any> remove(key: DataKey<T>) {
        logger.debug("Removing value for $key")

        ds.connection.use { conn ->
            val stmt = conn.prepareStatement(
                """
                DELETE FROM framework_data_store
                WHERE framework_data_key = ?;
            """.trimIndent()
            )

            stmt.setString(1, key.name)

            stmt.executeUpdate()
            stmt.close()
        }
    }

    override fun getKeyNames(): Set<String> {
        logger.debug("Getting all keys from Postgres storage provider")

        val keys = mutableSetOf<String>()

        ds.connection.use { conn ->
            val stmt = conn.prepareStatement(
                """
                SELECT framework_data_key
                FROM framework_data_store;
            """.trimIndent()
            )

            val rs = stmt.executeQuery()
            while (rs.next()) {
                val keyName = rs.getString("framework_data_key")
                keys.add(keyName)
            }

            rs.close()
            stmt.close()
        }

        return keys
    }
}