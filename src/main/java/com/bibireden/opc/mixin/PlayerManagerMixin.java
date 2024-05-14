package com.bibireden.opc.mixin;

import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;

// todo: We need to obtain three methods for this class. (onPlayerConnect (@HEAD), onPlayerDisconnect (@TAIL), disconnectAllPlayers (@RETURN))
// As this is an API, we will negate even doing an entrypoint, and handle these events within our mod though mixins.
@Mixin(PlayerManager.class)
abstract class PlayerManagerMixin {}
