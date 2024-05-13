package com.bibireden.opc

import com.bibireden.opc.api.CacheableValue
import dev.onyxstudios.cca.api.v3.component.Component
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.util.Identifier
import java.util.UUID

class OfflinePlayerCacheComponent: Component, AutoSyncedComponent {
    companion object {
        private val registeredCaches: MutableMap<Identifier, CacheableValue<*>> = mutableMapOf()
    }
    private val cache: MutableMap<UUID, Set<CacheableValue<*>>> = mutableMapOf<UUID, Set<CacheableValue<*>>>()

    override fun readFromNbt(tag: NbtCompound) {
        val opcTag = tag.getCompound("opc") ?: return

        val opcUUIDS = opcTag.keys

        for (uuid in opcUUIDS) {
            val playerData = opcTag.getCompound(uuid) ?: continue
            val playerUUID = UUID.fromString(uuid) ?: continue

            val playerCache = cache[playerUUID] ?: continue
            val cacheSet = mutableSetOf<CacheableValue<*>>()

            for ((id, registeredValue) in registeredCaches) {
                val value = registeredValue.readFromNBT(playerData.getCompound(id.toString()))
            }

            cache[playerUUID] = cacheSet
        }
    }

    override fun writeToNbt(tag: NbtCompound) {
        val dataHold = NbtCompound()

        for ((uuid, set) in cache)
        {
            val setComponents = NbtCompound()

            for (cacheableValue in set) {
                val valueCompound = NbtCompound()
                cacheableValue.writeToNBT(valueCompound)
                setComponents.put(cacheableValue.getID().toString(), valueCompound)
            }

            dataHold.put(uuid.toString(), setComponents)
        }

        tag.put("opc", dataHold)
    }
}