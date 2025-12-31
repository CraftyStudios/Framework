package dev.crafty.framework.data

import dev.crafty.framework.api.data.StorageProvider
import dev.crafty.framework.data.providers.PostgresStorageProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal object StorageProviderManager : KoinComponent {

    private val dataConfig: DataConfig by inject()

    private val provider: StorageProvider<*> by lazy {
        when (dataConfig.provider) {
            StorageProviderType.POSTGRES -> PostgresStorageProvider()
        }
        PostgresStorageProvider()
    }

    fun getStorageProvider(): StorageProvider<*> = provider
}