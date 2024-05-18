package com.bibireden.opc

import com.bibireden.opc.api.OfflinePlayerCacheAPI
import com.bibireden.opc.bus.OPCEventHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(OfflinePlayerCacheAPI.MOD_ID)
object OfflinePlayerCacheInitializer {
    val LOGGER: Logger = LogManager.getLogger()

    init {
        MinecraftForge.EVENT_BUS.register(OPCEventHandler())

//        runForDist(clientTarget = { MOD_BUS.addListener(::onClientSetup) }, serverTarget = { MOD_BUS.addListener(::onServerSetup) })

        LOGGER.info("Offline Player Cache has initialized!")
    }

//    private fun onClientSetup(event: FMLClientSetupEvent) {}
//
//    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {}
}
