package com.bibireden.opc.components

import com.bibireden.opc.OfflinePlayerCacheServer
import dev.onyxstudios.cca.api.v3.component.ComponentKey
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3
import dev.onyxstudios.cca.api.v3.world.WorldComponentFactoryRegistry
import dev.onyxstudios.cca.api.v3.world.WorldComponentInitializer
import net.minecraft.util.Identifier

class ComponentInitializer : WorldComponentInitializer {
    companion object {
        val ID = Identifier(OfflinePlayerCacheServer.MOD_ID, "values")
        val VALUES: ComponentKey<CacheComponent> = ComponentRegistryV3.INSTANCE.getOrCreate(ID, CacheComponent::class.java)
    }

    override fun registerWorldComponentFactories(registry: WorldComponentFactoryRegistry) {
        registry.register(VALUES) { CacheComponent() }
    }
}