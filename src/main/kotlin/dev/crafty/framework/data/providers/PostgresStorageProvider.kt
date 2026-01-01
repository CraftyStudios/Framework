package dev.crafty.framework.data.providers

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.crafty.framework.api.data.DataKey
import dev.crafty.framework.api.data.RegisterClass
import dev.crafty.framework.api.data.StorageProvider
import dev.crafty.framework.api.lifecycle.FrameworkPlugin
import dev.crafty.framework.api.logs.Logger
import dev.crafty.framework.api.tasks.now
import dev.crafty.framework.bootstrap.FrameworkPluginLoader
import dev.crafty.framework.data.DataConfig
import dev.crafty.framework.data.SerializerManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.reflections.Reflections

internal class PostgresStorageProvider : StorageProvider, KoinComponent {
    private val logger: Logger by inject()
    private val config: DataConfig by inject()

    private var ds: HikariDataSource
    private var kryo: Kryo

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

        kryo = Kryo()

        // works for objects without no-arg constructors
        kryo.instantiatorStrategy = StdInstantiatorStrategy()

        kryo.isRegistrationRequired = false

        now {
            SerializerManager.allSerializers().forEach { (kClass, serializer) ->
                logger.debug("Registering serializer for ${kClass.simpleName}")
                kryo.register(kClass.java, serializer)
            }
        }
    }

    override fun shutdown() {
        logger.debug("Shutting down Postgres storage provider")
        ds.close()
    }

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

                    value = kryo.readObject(Input(binary), key.type.java) as T
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
                Output(4096, -1).use { output ->
                    kryo.writeObject(output, value)

                    stmt.setString(1, key.name)
                    stmt.setBytes(2, output.toBytes())
                    stmt.executeUpdate()
                }
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