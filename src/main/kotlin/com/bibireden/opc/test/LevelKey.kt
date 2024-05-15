package com.bibireden.opc.test

import com.bibireden.opc.api.CachedPlayerKey
import com.bibireden.opc.api.OfflinePlayerCacheAPI.Companion.MOD_ID
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

class LevelKey : CachedPlayerKey<Int> {
    private val id: Identifier = Identifier(MOD_ID, "test-level-value")

    override fun get(player: ServerPlayerEntity): Int {
        return 69
    }

    override fun readFromNbt(tag: NbtCompound): Int {
        return tag.getInt(id.path)
    }

    override fun writeToNbt(tag: NbtCompound, value: Any?) {
        if (value is Int) tag.putInt(id.path, value)
    }

    override fun id(): Identifier = this.id
}