package dev.crafty.framework.bootstrap

import dev.crafty.framework.Framework
import dev.crafty.framework.api.data.DataStore
import dev.crafty.framework.api.i18n.I18n
import dev.crafty.framework.api.logs.Logger
import dev.crafty.framework.config.setupConfig
import dev.crafty.framework.data.DataConfig
import dev.crafty.framework.data.DataConfigLoader
import dev.crafty.framework.data.DefaultDataStore
import dev.crafty.framework.i18n.I18nProvider
import dev.crafty.framework.logs.LoggerProvider
import dev.s7a.ktconfig.KtConfigLoader
import org.bukkit.configuration.file.YamlConfiguration
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.awt.Frame

/**
 * Bootstrap loader for initializing the framework.
 */
internal object BootstrapLoader {
    /**
     * Initializes the bootstrap loader.
     */
    fun initialize() {
        initializeDi()
    }

    /**
     * Enables the dependency injection modules.
     */
    private fun initializeDi() {
        val dataModule = module {
            single<DataStore> { DefaultDataStore() }
        }

        val i18nModule = module {
            single<I18n> { I18nProvider() }
        }

        val loggerModule = module {
            single<Logger> { LoggerProvider() }
        }

        val configModule = module {
            single<DataConfig> {
                setupConfig(
                    "data.yml",
                    DataConfigLoader,
                    DataConfig()
                )
            }
        }

        startKoin {
            modules(
                configModule,
                dataModule,
                i18nModule,
                loggerModule
            )
        }
    }
}