package com.bibireden.opc.cache

import com.bibireden.opc.api.CachedPlayerKey
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import java.util.UUID

/** This is the implementation of the class. */
class OfflinePlayerCache internal constructor(
    val cache: MutableMap<UUID, MutableMap<CachedPlayerKey<out Any>, Any>> = mutableMapOf(),
    val usernameToUUID: BiMap<String, UUID> = HashBiMap.create()
) {
    companion object {
        /** Keys that are represented in the `nbt` data. */
        internal object Keys { const val UUID = "uuid"; const val NAME = "name"; const val KEYS = "keys" }

        private val cacheKeys = mutableMapOf<Identifier, CachedPlayerKey<out Any>>()

        /** Registers internally into the cache a `CachedPlayerKey`. */
        @Suppress("UNCHECKED_CAST")
        fun <V : Any>register(key: CachedPlayerKey<V>): CachedPlayerKey<V> {
            return cacheKeys.computeIfAbsent(key.id) { key } as CachedPlayerKey<V>
        }

        /** Gets a set of registered keys. */
        fun keys(): Set<Identifier> = cacheKeys.keys

        /** Gets a registered key based on the identifier. */
        fun getKey(id: Identifier): CachedPlayerKey<out Any>? = cacheKeys[id]

        /** Tries to obtain the cache from the `LevelProperties` of the server, and if it is not found, it will be null. */
        fun get(server: MinecraftServer): OfflinePlayerCache? = (server.overworld.levelProperties as? OfflinePlayerCacheData)?.offlinePlayerCache()
    }

    private fun isValidPlayerData(player: ServerPlayerEntity, function: (UUID, String) -> Unit): Boolean {
        val profile = player.gameProfile ?: return false
        if (profile.id == null || profile.name == null) return false
        function(profile.id, profile.name)
        return true
    }

    fun getPlayerValues(server: MinecraftServer, uuid: UUID): Map<CachedPlayerKey<out Any>, Any?> {
        return this.cache[uuid] ?: cacheKeys.map { (_, key) -> key to this.get(server, uuid, key) }.toMap()
    }

    fun getPlayerValues(server: MinecraftServer, username: String): Map<CachedPlayerKey<out Any>, Any?> {
        return this.cache[this.getUUID(username)] ?: cacheKeys.map { (_, key) -> key to this.get(server, username, key) }.toMap()
    }

    inline fun <reified V : Any>getFromCache(uuid: UUID, key: CachedPlayerKey<out V>): V? {
        val obtained = this.cache[uuid]?.get(key)
        return if (obtained is V) obtained else null
    }

    /** Gets a cached value from a players `UUID` and a cached value key.*/
    inline fun <reified V : Any>get(server: MinecraftServer, uuid: UUID, key: CachedPlayerKey<out V>): V? {
        val player = server.playerManager.getPlayer(uuid)
        return if (player == null) this.getFromCache(uuid, key) else key.get(player)
    }

    /** Gets a cached value from a players name and a cached value key. */
    inline fun <reified V : Any>get(server: MinecraftServer, username: String, key: CachedPlayerKey<out V>): V? {
        if (username.isEmpty()) return null
        val player = server.playerManager.getPlayer(username)
        if (player == null) {
            val uuid = this.usernameToUUID[username]?: return null
            return this.getFromCache(uuid, key)
        }
        return key.get(player)
    }

    /** Collect all the player ids from the server into a `Set`. */
    fun playerIDs(server: MinecraftServer): Collection<UUID> {
        val set = this.usernameToUUID.values.toHashSet()
        for (player in server.playerManager.playerList) {
            val uuid = player?.gameProfile?.id ?: continue
            set.add(uuid)
        }
        return set
    }

    /** Attempts to get the player UUID within the cache map based on a name. */
    private fun getUUID(username: String) = this.usernameToUUID[username]

    /** Attempts to get the **username** within the cache map based on a UUID. */
    private fun getUsername(uuid: UUID) = this.usernameToUUID.inverse()[uuid]

    /** Collects all the **usernames** from the server into a `Set`. */
    fun usernames(server: MinecraftServer): Collection<String> {
        val set = this.usernameToUUID.keys.toHashSet()
        for (player in server.playerManager.playerList) {
            val username = player?.gameProfile?.name
            if (!username.isNullOrEmpty()) set.add(username)
        }
        return set
    }

    fun writeToNbt(): NbtList {
        val list = NbtList()
        val uuidToUsernames = this.usernameToUUID.inverse()

        for (uuid in this.cache.keys) {
            val data = this.cache[uuid] ?: continue

            val entry = NbtCompound()
            val keys = NbtCompound()

            entry.putUuid(Keys.UUID, uuid)
            entry.putString(Keys.NAME, uuidToUsernames.getOrDefault(uuid, ""))

            for (key in data.keys) {
                val innerEntry = NbtCompound()
                key.writeToNbt(innerEntry, data[key] ?: continue)
                keys.put(key.toString(), innerEntry)
            }

            entry.put(Keys.KEYS, keys)
            list.add(entry)
        }

        return list
    }

    fun readFromNbt(list: NbtList) {
        if (list.isEmpty()) return

        this.cache.clear()
        this.usernameToUUID.clear()

        for (index in list.indices) {
            val entry = list.getCompound(index)
            val keysCompound = entry.getCompound(Keys.KEYS)
            val uuid = entry.getUuid(Keys.UUID)
            val name = entry.getString(Keys.NAME)

            if (name.isEmpty()) continue

            val data = mutableMapOf<CachedPlayerKey<out Any>, Any>()

            for (id in keysCompound.keys) {
                val key = cacheKeys[Identifier(id)]
                val value = key?.readFromNbt(keysCompound.getCompound(id)) ?: continue
                data[key] = value
            }

            this.cache[uuid] = data
            this.usernameToUUID[name] = uuid
        }
    }

    fun isPlayerCached(uuid: UUID) = this.cache.containsKey(uuid)

    fun isPlayerCached(name: String) = this.usernameToUUID.containsKey(name)

    fun cache(player: ServerPlayerEntity): Boolean = this.isValidPlayerData(player) { uuid, username ->
        val value = mutableMapOf<CachedPlayerKey<out Any>, Any>()
        cacheKeys.values.forEach { key -> value[key] = key.get(player) ?: return@forEach }
        this.cache[uuid] = value
        this.usernameToUUID[username] = uuid
    }

    /** Un-caches the player based on the provided `ServerPlayerEntity`. */
    fun unCache(player: ServerPlayerEntity) = this.isValidPlayerData(player) { uuid, _ ->
        this.cache.remove(uuid)
        this.usernameToUUID.inverse().remove(uuid)
    }

    /**
     * Attempts to un-cache the player using a `UUID` and a given key.
     *
     * This could fail if the UUID is not in the cache, or the key removed is not present in the cache.
     */
    fun unCache(uuid: UUID, key: CachedPlayerKey<out Any>): Boolean {
        if (cacheKeys.containsKey(key.id)) return false

        val value = this.cache[uuid] ?: return false

        if (value.remove(key) != null) {
            if (value.isEmpty()) {
                this.cache.remove(uuid)
                this.usernameToUUID.inverse().remove(uuid)
            }
            return true
        }
        return false
    }

    /**
     * Attempts to un-cache the player using their name and a given key.
     *
     * This could fail if the player name is not in the cache, or the key removed is not present in the cache.
     */
    fun unCache(username: String, key: CachedPlayerKey<out Any>): Boolean {
        if (username.isEmpty()) return false
        return this.unCache(this.getUUID(username) ?: return false, key)
    }


    /**
     * Attempts to un-cache the player using a `UUID`.
     *
     * This could fail if the UUID is not in the cache.
     */
    fun unCache(uuid: UUID): Boolean {
        this.cache.remove(uuid)
        return this.usernameToUUID.inverse().remove(uuid) != null
    }

    /**
     * Attempts to un-cache the player using their name.
     *
     * This could fail if the UUID is not in the cache.
     */
    fun unCache(username: String): Boolean {
        if (username.isEmpty()) return false
        val uuid = this.usernameToUUID[username] ?: return false
        return this.unCache(uuid)
    }
}