package com.bibireden.opc.cache

import com.bibireden.opc.api.CachedPlayerKey
import com.bibireden.opc.api.OfflinePlayerCacheAPI
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID



/** This is the implementation of the class. */
class OfflinePlayerCache internal constructor(
    val cache: MutableMap<UUID, MutableMap<CachedPlayerKey<out Any>, Any>> = mutableMapOf(),
    val usernameToUUID: BiMap<String, UUID> = HashBiMap.create()
) : SavedData() {
    companion object {
        /** Keys that are represented in the `nbt` data. */
        internal object Keys { const val UUID = "uuid"; const val NAME = "name"; const val KEYS = "keys" }

        private val cacheKeys = mutableMapOf<ResourceLocation, CachedPlayerKey<out Any>>()

        /** Registers internally into the cache a `CachedPlayerKey`. */
        @Suppress("UNCHECKED_CAST")
        fun <V : Any>register(key: CachedPlayerKey<V>): CachedPlayerKey<V> {
            return cacheKeys.computeIfAbsent(key.id) { key } as CachedPlayerKey<V>
        }

        /** Gets a set of registered keys. */
        fun keys(): Set<ResourceLocation> = cacheKeys.keys

        /** Gets a registered key based on the ResourceLocation. */
        fun getKey(id: ResourceLocation): CachedPlayerKey<out Any>? = cacheKeys[id]

        /** Tries to obtain the cache from the `LevelProperties` of the server, and if it is not found, it will be null. */
        fun get(server: MinecraftServer): OfflinePlayerCache = server.overworld().dataStorage.computeIfAbsent(OfflinePlayerCache::load, ::OfflinePlayerCache, OfflinePlayerCacheAPI.MOD_ID)

        private fun load(tag: CompoundTag): OfflinePlayerCache {
            val data = OfflinePlayerCache()
            data.readFromNbt(tag.get(OfflinePlayerCacheAPI.MOD_ID) as ListTag)
            return data;
        }
    }

    private fun isValidPlayerData(player: Player, function: (UUID, String) -> Unit): Boolean {
        val profile = player.gameProfile ?: return false
        if (profile.id == null || profile.name == null) return false
        function(profile.id, profile.name)
        return true
    }

    fun getPlayerValues(server: MinecraftServer, uuid: UUID): Map<CachedPlayerKey<out Any>, Any?> {
        return this.cache[uuid] ?: cacheKeys.map { (_, key) -> key to this.get(server, uuid, key) }.toMap()
    }

    fun getPlayerValues(server: MinecraftServer, username: String): Map<CachedPlayerKey<out Any>, Any?> {
        return this.cache[this.usernameToUUID[username]] ?: cacheKeys.map { (_, key) -> key to this.get(server, username, key) }.toMap()
    }

    inline fun <reified V : Any>getFromCache(uuid: UUID, key: CachedPlayerKey<out V>): V? {
        val obtained = this.cache[uuid]?.get(key)
        return if (obtained is V) obtained else null
    }

    /** Gets a cached value from a players `UUID` and a cached value key.*/
    inline fun <reified V : Any>get(server: MinecraftServer, uuid: UUID, key: CachedPlayerKey<out V>): V? {
        val player = server.playerList.getPlayer(uuid) ?: return this.getFromCache(uuid, key)
        return key.get(player)
    }

    /** Gets a cached value from a players name and a cached value key. */
    inline fun <reified V : Any>get(server: MinecraftServer, username: String, key: CachedPlayerKey<out V>): V? {
        if (username.isEmpty()) return null
        val player = server.playerList.getPlayerByName(username)
        if (player == null) {
            val uuid = this.usernameToUUID[username]?: return null
            return this.getFromCache(uuid, key)
        }
        return key.get(player)
    }

    /** Collect all the player ids from the server into a `Set`. */
    fun uuids(server: MinecraftServer): Collection<UUID> {
        val set = this.usernameToUUID.values.toHashSet()
        for (player in server.playerList.players) {
            val uuid = player?.gameProfile?.id ?: continue
            set.add(uuid)
        }
        return set
    }

    /** Attempts to get the player UUID within the cache map based on a name. If there is nothing cached, it will fetch from the server. */
    fun getUUID(server: MinecraftServer, username: String): UUID? {
        var uuid = this.usernameToUUID[username]
        if (uuid == null) uuid = server.playerList.getPlayerByName(username)?.uuid
        return uuid
    }
    /** Attempts to get the **username** within the cache map based on a UUID. */
    fun getUsername(server: MinecraftServer, uuid: UUID): String? {
        var username = this.usernameToUUID.inverse()[uuid]
        if (username == null) username = server.playerList.getPlayer(uuid)?.name.toString()
        return username
    }

    /** Collects all the **usernames** from the server into a `Set`. */
    fun usernames(server: MinecraftServer): Collection<String> {
        val set = this.usernameToUUID.keys.toHashSet()
        for (player in server.playerList.players) {
            val username = player?.gameProfile?.name
            if (!username.isNullOrEmpty()) set.add(username)
        }
        return set
    }

    fun writeToNbt(): ListTag {
        val list = ListTag()
        val uuidToUsernames = this.usernameToUUID.inverse()

        for (uuid in this.cache.keys) {
            val data = this.cache[uuid] ?: continue

            val entry = CompoundTag()
            val keys = CompoundTag()

            entry.putUUID(Keys.UUID, uuid)
            entry.putString(Keys.NAME, uuidToUsernames.getOrDefault(uuid, ""))

            for (key in data.keys) {
                val innerEntry = CompoundTag()
                key.writeToNbt(innerEntry, data[key] ?: continue)
                keys.put(key.toString(), innerEntry)
            }

            entry.put(Keys.KEYS, keys)
            list.add(entry)
        }

        return list
    }

    fun readFromNbt(list: ListTag) {
        if (list.isEmpty()) return

        this.cache.clear()
        this.usernameToUUID.clear()

        for (index in list.indices) {
            val entry = list.getCompound(index)
            val keysCompound = entry.getCompound(Keys.KEYS)
            val uuid = entry.getUUID(Keys.UUID)
            val name = entry.getString(Keys.NAME)

            if (name.isEmpty()) continue

            val data = mutableMapOf<CachedPlayerKey<out Any>, Any>()

            for (id in keysCompound.allKeys) {
                val key = cacheKeys[ResourceLocation(id)]
                val value = key?.readFromNbt(keysCompound.getCompound(id)) ?: continue
                data[key] = value
            }

            this.cache[uuid] = data
            this.usernameToUUID[name] = uuid
        }
    }

    fun isPlayerCached(uuid: UUID) = this.cache.containsKey(uuid)

    fun isPlayerCached(name: String) = this.usernameToUUID.containsKey(name)

    fun cache(player: Player): Boolean = this.isValidPlayerData(player) { uuid, username ->
        val value = mutableMapOf<CachedPlayerKey<out Any>, Any>()
        cacheKeys.values.forEach { key -> value[key] = key.get(player) ?: return@forEach }
        this.cache[uuid] = value
        this.usernameToUUID[username] = uuid
        this.setDirty()
    }

    /** Un-caches the player based on the provided `Player`. */
    fun unCache(player: Player) = this.isValidPlayerData(player) { uuid, _ ->
        this.cache.remove(uuid)
        this.usernameToUUID.inverse().remove(uuid)
        this.setDirty()
    }

    /**
     * Attempts to un-cache the player using a `UUID` and a given key.
     *
     * This could fail if the UUID is not in the cache, or the key removed is not present in the cache.
     */
    fun unCache(uuid: UUID, key: CachedPlayerKey<out Any>): Boolean {
        if (cacheKeys[key.id] == null) return false

        val value = this.cache[uuid] ?: return false

        if (value.remove(key) != null) {
            if (value.isEmpty()) {
                this.unCache(uuid)
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
        return this.unCache(this.usernameToUUID[username] ?: return false, key)
    }


    /**
     * Attempts to un-cache the player using a `UUID`.
     *
     * This could fail if the UUID is not in the cache.
     */
    fun unCache(uuid: UUID): Boolean {
        this.cache.remove(uuid)
        var success = this.usernameToUUID.inverse().remove(uuid) != null
        if (success) this.setDirty()
        return success
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

    override fun save(tag: CompoundTag): CompoundTag {
        tag.put(OfflinePlayerCacheAPI.MOD_ID, this.writeToNbt())
        return tag
    }
}