package com.github.clevernucleus.opc.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.clevernucleus.opc.impl.OfflinePlayerCacheData;
import com.github.clevernucleus.opc.impl.OfflinePlayerCacheImpl;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.SaveVersionInfo;

@Mixin(LevelProperties.class)
abstract class LevelPropertiesMixin implements OfflinePlayerCacheData {
	
	@Unique
	private OfflinePlayerCacheImpl opc_cache = new OfflinePlayerCacheImpl();
	
	@Inject(method = "updateProperties", at = @At("HEAD"))
	private void opc_updateProperties(DynamicRegistryManager registryManager, NbtCompound levelNbt, @Nullable NbtCompound playerNbt, CallbackInfo info) {
		levelNbt.put("OfflinePlayerCache", this.opc_cache.writeToNbt());
	}
	
	@Inject(method = "readProperties", at = @At("RETURN"))
	private static void opc_readProperties(Dynamic<NbtElement> dynamic2, DataFixer dataFixer, int dataVersion, @Nullable NbtCompound playerData, LevelInfo levelInfo, SaveVersionInfo saveVersionInfo, LevelProperties.SpecialProperty specialProperty, GeneratorOptions generatorOptions, Lifecycle lifecycle, CallbackInfoReturnable<LevelProperties> info) {
		LevelProperties levelProperties = info.getReturnValue();
		dynamic2.get("OfflinePlayerCache").result().map(Dynamic::getValue).ifPresent(nbt -> ((OfflinePlayerCacheData)levelProperties).offlinePlayerCache().readFromNbt((NbtList)nbt));
	}
	
	@Override
	public OfflinePlayerCacheImpl offlinePlayerCache() {
		return this.opc_cache;
	}
}
