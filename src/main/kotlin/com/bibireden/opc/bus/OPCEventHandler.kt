package com.bibireden.opc.bus

import com.bibireden.opc.OfflinePlayerCacheCommands
import com.bibireden.opc.api.OfflinePlayerCacheAPI
import com.bibireden.opc.cache.OfflinePlayerCache
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus

@Mod.EventBusSubscriber(modid = OfflinePlayerCacheAPI.MOD_ID, bus = Bus.FORGE, value = [Dist.DEDICATED_SERVER])
class OPCEventHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPlayerConnect(event: PlayerEvent.PlayerLoggedInEvent) {
        OfflinePlayerCache.get(event.entity.server ?: return).unCache(event.entity)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPlayerDisconnect(event: PlayerEvent.PlayerLoggedOutEvent) {
        OfflinePlayerCache.get(event.entity.server ?: return).cache(event.entity)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun registerCommands(event: RegisterCommandsEvent) {
        OfflinePlayerCacheCommands.register(event.dispatcher)
    }
}