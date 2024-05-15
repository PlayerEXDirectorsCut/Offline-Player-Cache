package com.bibireden.opc.api

import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

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