package com.github.clevernucleus.opc_dc.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.github.clevernucleus.opc_dc.impl.OfflinePlayerCacheData;
import com.github.clevernucleus.opc_dc.impl.OfflinePlayerCacheImpl;

import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.UnmodifiableLevelProperties;

@Mixin(UnmodifiableLevelProperties.class)
abstract class UnmodifiableLevelPropertiesMixin implements OfflinePlayerCacheData {
	
	@Final
	@Shadow
	private ServerWorldProperties worldProperties;
	
	@Override
	public OfflinePlayerCacheImpl offlinePlayerCache() {
		return ((OfflinePlayerCacheData)this.worldProperties).offlinePlayerCache();
	}
}
