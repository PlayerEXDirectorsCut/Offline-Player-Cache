package com.bibireden.opc.api

import com.bibireden.opc.cache.OfflinePlayerCache
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import java.util.UUID

/**
 * The API for the **`OfflinePlayerCache`**.
 *
 * For static members, you can register values into the API.
 * In order to utilize it in its full capacity, the server needs to be passed in the constructor.
 *
 * @author bibi-reden, DataEncoded, OverlordsIII
 * */
class OfflinePlayerCacheAPI(private val server: MinecraftServer) {
    companion object {
        const val MOD_ID = "offline-player-cache"
        /**
         * Registers the provided **`CachedPlayerKey`** to the server's cache.
         *
         * This function should be normally utilized or incorporated into your mods initializer statically.
         *
         * ```
         * // Kotlin Example
         * object ExampleMod : ModInitializer {
         *     val exampleKey = OfflinePlayerCacheAPI.register(ExampleKey())
         * }
         * ```
         */
        fun <V>register(key: CachedPlayerKey<V>): CachedPlayerKey<V> = OfflinePlayerCache.register(key)

        /** The current collection of registered keys in the cache, represented by their associated `Identifiers`. */
        fun keys(): Set<Identifier> = OfflinePlayerCache.keys()
    }

    /** Based on the **`UUID`** of a player, it will fetch the last cached value if offline, otherwise it will get the current value from the player. */
    fun <V>get(uuid: UUID, key: CachedPlayerKey<V>): V? = OfflinePlayerCache.get(this.server)?.get(server, uuid, key)

    /** Based on the **username** of a player, it will fetch the last cached value if offline, otherwise it will get the current value from the player. */
    fun <V>get(username: String, key: CachedPlayerKey<V>): V? = OfflinePlayerCache.get(this.server)?.get(server, username, key)

    /** Returns all offline & online player **`UUIDs`**. */
    fun uuids(): Collection<UUID> = OfflinePlayerCache.get(this.server)?.playerIDs(this.server) ?: emptyList()

    /** Returns all offline & online **usernames**. */
    fun usernames(): Collection<String> = OfflinePlayerCache.get(this.server)?.usernames(this.server) ?: emptyList()

    /** Checks if the player with the **`UUID`** is in the cache or not. */
    fun isPlayerCached(uuid: UUID): Boolean = OfflinePlayerCache.get(this.server)?.isPlayerCached(uuid) ?: false

    /** Checks if the player with the **username** is in the cache or not. */
    fun isPlayerCached(username: String): Boolean = OfflinePlayerCache.get(this.server)?.isPlayerCached(username) ?: false

    /** Obtains the cached data relating to the player's `UUID`. */
    fun getPlayerValues(uuid: UUID): Map<CachedPlayerKey<*>, *>? = OfflinePlayerCache.get(this.server)?.getPlayerCache(uuid)

    /** Obtains the cached data relating to the player's **username**. */
    fun getPlayerValues(username: String): Map<CachedPlayerKey<*>, *>? = OfflinePlayerCache.get(this.server)?.getPlayerCache(username)
}