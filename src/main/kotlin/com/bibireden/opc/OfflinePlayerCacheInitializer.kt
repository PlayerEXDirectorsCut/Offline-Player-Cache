package com.bibireden.opc

import com.bibireden.opc.cache.OfflinePlayerCache
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents

object OfflinePlayerCacheInitializer : ModInitializer {
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