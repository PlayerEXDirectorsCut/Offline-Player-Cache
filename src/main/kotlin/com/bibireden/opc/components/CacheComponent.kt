package com.bibireden.opc.components

import com.bibireden.opc.OfflinePlayerCacheServer
import com.bibireden.opc.api.CacheableValue
import com.google.common.collect.HashBiMap
import dev.onyxstudios.cca.api.v3.component.Component
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import java.util.*

typealias UserName = String

class CacheComponent : Component, AutoSyncedComponent {
    companion object {
        // All the registered key identifiers that map to a cached value (that are done through the register method).
        private val _keys = mutableMapOf<Identifier, CacheableValue<*>>()

        /**
         * Verifies if the player is currently connected to the server and is cacheable/not cacheable.
         * I know, this seems dumb and redundant, because it is! But null safety is a real issue...
         */
        private fun isPlayerValid(player: ServerPlayerEntity): Boolean {
            return player.gameProfile != null && player.uuid != null && player.name.toString().isNotEmpty()
        }

        /** Registers a `CacheableValue` into the keys of the component. */
        fun <V> register(key: CacheableValue<V>): CacheableValue<V> = _keys.computeIfAbsent(key.id()) { id -> key } as CacheableValue<V>

        /** Returns all the keys into an immutable set. */
        fun keys(): Set<Identifier> = _keys.keys

        /** Attempts to get a specific key based on the identifier. */
        fun getKey(key: Identifier): CacheableValue<*>? = _keys[key]

        object NbtKeys {
            const val KEYS = "keys"
            const val UUID = "uuid"
            const val USERNAME = "username"
        }
    }
    // The cache of data stored with the corresponding entity UUID, then stored with the cacheable value along with data
    private val _cache = mutableMapOf<UUID, MutableMap<CacheableValue<*>, *>>()
    private val _usernameToUUID = HashBiMap.create<UserName, UUID>()

    override fun readFromNbt(tag: NbtCompound) {
        val list = tag.get(OfflinePlayerCacheServer.MOD_ID)
        if (list !is NbtList) return

        this._cache.clear()
        this._usernameToUUID.clear()

        for (i in 0..list.size) {
            val entry = list.getCompound(i)

            val entryKeys = entry.getCompound(NbtKeys.KEYS)
            val entryUuid = entry.getUuid(NbtKeys.UUID)
            val entryUsername = entry.getString(NbtKeys.USERNAME)

            if (entryUsername.isNullOrEmpty()) continue

            val data = mutableMapOf<CacheableValue<*>, Any?>()

            for (id in entryKeys.keys) {
                val key = _keys[Identifier(id)] ?: continue
                val value = key.readFromNbt(entryKeys.getCompound(id))
                data.put(key, value)
            }

            this._cache[entryUuid] = data
            this._usernameToUUID[entryUsername] = entryUuid
        }
    }

    /**
     * Okay. I think this has been figured out.
     * Using the NBTList from before, we can store each uuid/username entry into a compound.
     *
     * Next step is the keys. Using the keys that will be **registered**, we will create a compound for these keys,
     * iterate through the keys in the cache for the respective UUID, and insert an inner compound into the former compound stated.
     *
     * Finally, we insert that into the entry compound, and add it to the list. We continue iteration, and when iteration is completed,
     * insert the list into the `tag` NbtCompound, which will refer to the world's NBT. We will just put in the identifier "cache".
     * **/
    override fun writeToNbt(tag: NbtCompound) {
        val list = NbtList()
        val usernames = this._usernameToUUID.inverse()

        for (uuid in this._cache.keys) {
            val data = this._cache[uuid] ?: return
            val entry = NbtCompound()
                entry.putUuid(NbtKeys.UUID, uuid)
                entry.putString(NbtKeys.USERNAME, usernames[uuid] ?: continue) // if we fail to resolve username, ditch.

            val keys = NbtCompound()

            for ((key, value) in data) {
                if (value == null) continue // Ignore null data
                val innerEntry = NbtCompound()
                key.writeToNbt(innerEntry, value)
                keys.put(key.id().toString(), innerEntry)
            }

            entry.put(NbtKeys.KEYS, keys)
            list.add(entry)
        }
        tag.put(OfflinePlayerCacheServer.MOD_ID, list)
    }

    /**
     * Seeks to cache a `ServerPlayerEntity` to the component.
     *
     * This will obtain the players `UUID` and player name
     * (though an issue with this approach is that changed names need to be dealt with)
     * and stores it to the cache and the UUID -> PlayerName BiMap.
     * */
    fun cache(player: ServerPlayerEntity): Boolean {
        if (!isPlayerValid(player)) return false
        val value = mutableMapOf<CacheableValue<*>, Any?>()
        _keys.entries.forEach { entry -> value[entry.value] = entry.value.get(player) }
        this._cache[player.uuid] = value
        this._usernameToUUID[player.name.toString()] = player.uuid
        return true
    }

    /** Un-caches a `ServerPlayerEntity`. */
    fun unCache(player: ServerPlayerEntity): Boolean {
        if (!isPlayerValid(player)) return false
        this._cache.remove(player.uuid)
        this._usernameToUUID.inverse().remove(player.uuid)
        return true
    }

    /**
     * Get data from the cache when available, if not returns null
     * @param uuid
     * @param key
     * @return Type of generic passed in or null
     */
    private inline fun <reified V>_getFromCache(uuid: UUID, key: CacheableValue<V>): V?  {
        val value: Map<CacheableValue<*>, *> = this._cache[uuid] ?: return null

        val data = value[key]

        if (data !is V) return null

        return data
    }

    /**
     * Gets data from player if it exists, if not gets from cache. Returns null if data exists nowhere
     * @param server
     * @param uuid
     * @param key
     * @return Type of generic passed in or null
     */
    private inline fun <reified V> _get(server: MinecraftServer, uuid: UUID, key: CacheableValue<V>): V? {
        val player : ServerPlayerEntity = server.playerManager.getPlayer(uuid) ?: return null
        return this._getFromCache(uuid, key)
    }

    /**
     * Gets data from player if it exists, if not gets from cache. Returns null if data exists nowhere
     * @param server
     * @param username
     * @param key
     * @return Type of generic passed in or null
     */
    private inline fun <reified V> get(server: MinecraftServer, username: String, key: CacheableValue<V>): V? {
        server.playerManager.getPlayer(username) ?: return null
        val uuid: UUID = this._usernameToUUID[username] ?: return null

        return this._getFromCache(uuid, key)
    }

    fun playerIds(server: MinecraftServer): Collection<UUID> = server.playerManager.playerList
        .filter { isPlayerValid(it) }
        .map { it.uuid }

    fun playerNames(server: MinecraftServer): Collection<String> = server.playerManager.playerList
        .filter { isPlayerValid(it) }
        .map { it.name.toString() }

    fun isPlayerCached(uuid: UUID): Boolean = this._cache.containsKey(uuid)

    fun isPlayerCached(name: String): Boolean = this._usernameToUUID.containsKey(name)

    fun getPlayerName(uuid: UUID) = this._usernameToUUID.inverse()[uuid]

    fun getPlayerUUID(name: String) = this._usernameToUUID[name]
}