package com.bibireden.opc.api

import com.bibireden.opc.components.CacheComponent
import com.bibireden.opc.components.ComponentInitializer
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import java.util.*

interface OfflinePlayerCache {
    companion object {
        /**
         * Registers a cacheable value to the server, will be keys that will instruct the server to cache some data from players when they disconnect.
         */
        fun <V> register(key: CacheableValue<V>): CacheableValue<V> = CacheComponent.register(key)

        /** Will attempt to get access to the cache object based on the world component. Should only be used on the server. */
        fun getCache(server: MinecraftServer): CacheComponent? = ComponentInitializer.VALUES.getNullable(server.overworld)

        /** Attempts to get a key from the cache. */
        fun getKey(id: Identifier) = CacheComponent.getKey(id)
    }

    fun <V> get(uuid: UUID, key: CacheableValue<V>)

    fun <V> get(name: String, key: CacheableValue<V>)

    fun playerIds(): Set<UUID>

    fun playerNames(): Set<String>

    fun isPlayerCached(uuid: UUID): Boolean

    fun isPlayerCached(name: String): Boolean
}