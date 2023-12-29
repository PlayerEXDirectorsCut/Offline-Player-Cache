package com.github.clevernucleus.opc_dc.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.clevernucleus.opc_dc.impl.OfflinePlayerCacheImpl;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(PlayerManager.class)
abstract class PlayerManagerMixin {
	
	@Final
	@Shadow
	private MinecraftServer server;
	
	@Final
	@Shadow
	List<ServerPlayerEntity> players;
	
	@Inject(method = "onPlayerConnect", at = @At("TAIL"))
	private void opc_onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo info) {
		OfflinePlayerCacheImpl.ifPresent(this.server, (Object)null, opc -> {
			opc.uncache(player);
			return (Object)null;
		});
	}
	
	@Inject(method = "remove", at = @At("HEAD"))
	private void opc_remove(ServerPlayerEntity player, CallbackInfo info) {
		OfflinePlayerCacheImpl.ifPresent(this.server, (Object)null, opc -> {
			opc.cache(player);
			return (Object)null;
		});
	}
	
	/*
	 * We could also do the following:
	 * 
	 * 
	 * @Inject(method = "disconnectAllPlayers", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"), locals = LocalCapture.CAPTURE_FAILHARD)
	 * private void opc_disconnectAllPlayers(CallbackInfo info, int i) {
	 *     ServerPlayerEntity player = this.players.get(i);
	 * }
	 * 
	 * This may seem faster at first since we're not iterating over PlayerManager#players twice, but it is actually slower due to getting the WorldProperties 
	 * and checking for correct instanceof, and casting for each player. Instead, we move this out of the loop, which ends up being faster. Why do we check 
	 * for instanceof OfflinePlayerCacheData in the first place when we used Mixins? Because otherwise it occasionally gets unhappy for unknown reason. Also, if 
	 * for some reason this gets called on a client or an integrated server (maybe from some other poorly coded mod) it will still be safe.
	 * 
	 */
	@Inject(method = "disconnectAllPlayers", at = @At("HEAD"))
	private void opc_disconnectAllPlayers(CallbackInfo info) {
		OfflinePlayerCacheImpl.ifPresent(this.server, (Object)null, opc -> {
			for(int i = 0; i < this.players.size(); i++) {
				opc.cache(this.players.get(i));
			}
			
			return (Object)null;
		});
	}
}
