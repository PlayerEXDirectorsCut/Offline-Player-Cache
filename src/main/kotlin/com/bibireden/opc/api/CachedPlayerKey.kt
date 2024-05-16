package com.bibireden.opc.api

import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

/**
 * A key that is meant to be registered into the **`OfflinePlayerCacheAPI`**, and
 * is extended from to implement values meant to be stored on the server no matter a player's online or offline state.
 *
 * @author bibi-reden, DataEncoded
 * @see OfflinePlayerCacheAPI
 */
interface CachedPlayerKey<V> {
    /**
     * Used to get the cached value from the player.
     * */
    fun get(player: ServerPlayerEntity): V

    /** Reads a value from a nbt. */
    fun readFromNbt(tag: NbtCompound): V

    /** Writes a value to a nbt. */
    fun writeToNbt(tag: NbtCompound, value: Any?)

    /** The key of the value. This would be used in the form `modid:<path>`. */
    fun id(): Identifier
}