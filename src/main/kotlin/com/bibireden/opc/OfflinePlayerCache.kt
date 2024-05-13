package com.bibireden.opc

import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object OfflinePlayerCache : ModInitializer {
	val MOD_ID = "offline-player-cache"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("Offline Player Cache Initialized!")
	}
}