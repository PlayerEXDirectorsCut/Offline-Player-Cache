package com.bibireden.opc

import com.bibireden.opc.api.OfflinePlayerCacheAPI
import com.bibireden.opc.cache.OfflinePlayerCache
import com.bibireden.opc.test.LevelKey
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents

object OfflinePlayerCacheInitializer : ModInitializer {
    val LEVEL_KEY = OfflinePlayerCacheAPI.register(LevelKey())
    
    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            OfflinePlayerCacheCommands.register(dispatcher)
        }

        ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
            OfflinePlayerCache.get(server)?.unCache(handler.player)
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            OfflinePlayerCache.get(server)?.cache(handler.player)
        }
    }
}