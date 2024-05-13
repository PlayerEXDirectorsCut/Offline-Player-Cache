package com.bibireden.opc

import dev.onyxstudios.cca.api.v3.component.ComponentKey
import dev.onyxstudios.cca.api.v3.level.LevelComponentFactoryRegistry
import dev.onyxstudios.cca.api.v3.level.LevelComponentInitializer

class CachedLevelComponents : LevelComponentInitializer {
    val VALUES: ComponentKey<>

    override fun registerLevelComponentFactories(registry: LevelComponentFactoryRegistry) {
        registry.register()
    }
}