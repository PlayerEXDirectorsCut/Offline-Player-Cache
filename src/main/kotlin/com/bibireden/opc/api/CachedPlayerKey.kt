package com.bibireden.opc.api

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player

/**
 * A key that is meant to be registered into the **`OfflinePlayerCacheAPI`**, and
 * is extended from to implement values meant to be stored on the server no matter a player's online or offline state.
 *
 * @author bibi-reden, DataEncoded
 * @see OfflinePlayerCacheAPI
 */
abstract class CachedPlayerKey<V : Any>(
    /** The key of the value. This would be used in the form `modid:<path>`. */
    val id: ResourceLocation
) {
    /** Used to get the value associated with the cached key from the player. */
    abstract fun get(player: Player): V?

    /** Reads a value from a nbt. */
    abstract fun readFromNbt(tag: CompoundTag): V

    /** Writes a value to a nbt. */
    abstract fun writeToNbt(tag: CompoundTag, value: Any)

    override fun toString(): String = this.id.toString()
}