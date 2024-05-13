package com.bibireden.opc

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.slf4j.LoggerFactory

object OfflinePlayerCacheServer : ModInitializer {
	val MOD_ID = "offline-player-cache"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("Offline Player Cache Initialized!")

		ServerPlayConnectionEvents.JOIN.register { listener, _, server ->
			ComponentInitializer.VALUES.maybeGet(server.overworld).ifPresent { component ->
				component.unCache(listener.player)
			}
		}

		ServerPlayConnectionEvents.DISCONNECT.register { listener, server ->
			ComponentInitializer.VALUES.maybeGet(server.overworld).ifPresent { component ->
				component.cache(listener.player)
			}
		}

	}
}