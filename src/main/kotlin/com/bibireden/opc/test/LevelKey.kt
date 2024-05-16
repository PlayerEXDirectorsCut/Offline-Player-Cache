package com.bibireden.opc.test

import com.bibireden.opc.api.CachedPlayerKey
import com.bibireden.opc.api.OfflinePlayerCacheAPI.Companion.MOD_ID
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

class LevelKey : CachedPlayerKey<Int>(Identifier(MOD_ID, "test-level-value-kotlin")) {
    override fun get(player: ServerPlayerEntity): Int {
        // this would be up to the end user's interpretation, so for now, anything will suffice.
        return 69
    }

    override fun readFromNbt(tag: NbtCompound): Int {
        return tag.getInt(id.path)
    }

    override fun writeToNbt(tag: NbtCompound, value: Any?) {
        if (value is Int) tag.putInt(id.path, value)
    }
}