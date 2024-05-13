package com.bibireden.opc

import dev.onyxstudios.cca.api.v3.component.ComponentKey
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3
import dev.onyxstudios.cca.api.v3.level.LevelComponentFactoryRegistry
import dev.onyxstudios.cca.api.v3.level.LevelComponentInitializer
import dev.onyxstudios.cca.api.v3.world.WorldComponentFactoryRegistry
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier

class ComponentInitializer : LevelComponentInitializer {
    companion object {
        private val cacheKey: ComponentKey<OfflinePlayerCacheComponent> = ComponentRegistryV3.INSTANCE.getOrCreate(Identifier(OfflinePlayerCacheServer.MOD_ID, "values"), CacheComponent::class.java)
        fun useCacheComponent(server: MinecraftServer) = cacheKey.get(server.overworld)
    }

    override fun registerLevelComponentFactories(registry: LevelComponentFactoryRegistry) {
        registry.register(cacheKey) {OfflinePlayerCacheComponent()}
    }
}