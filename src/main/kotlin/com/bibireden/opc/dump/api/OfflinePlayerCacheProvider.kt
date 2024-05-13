package com.bibireden.opc.dump.api

import com.bibireden.opc.dump.api.components.CacheComponent
import com.bibireden.opc.ComponentInitializer
import net.minecraft.server.MinecraftServer
import java.util.*

/**
 * Internal provider that is meant to be used to take in a `MinecraftServer`, and interface directly with the cached component.
 */
internal class OfflinePlayerCacheProvider(
    private val server: MinecraftServer,
    private val component: CacheComponent = ComponentInitializer.useCacheComponent(server)
) : OfflinePlayerCache {
    override fun <V> get(uuid: UUID, key: CacheableValue<V>): V? = this.component.get(this.server, uuid, key)

    override fun <V> get(name: String, key: CacheableValue<V>): V? = this.component.get(this.server, name, key)

    override fun getPlayerUUID(name: String): UUID? = this.component.getPlayerUUID(name)

    override fun getPlayerName(uuid: UUID): String? = this.component.getPlayerName(uuid)

    override fun playerIds(): Collection<UUID> = this.component.playerIds(this.server)

    override fun playerNames(): Collection<String> = this.component.playerNames(this.server)

    override fun isPlayerCached(uuid: UUID): Boolean = this.component.isPlayerCached(uuid)

    override fun isPlayerCached(name: String): Boolean = this.component.isPlayerCached(name)
}