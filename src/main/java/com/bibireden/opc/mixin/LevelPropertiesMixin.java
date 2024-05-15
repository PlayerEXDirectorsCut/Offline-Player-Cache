package com.bibireden.opc.mixin;

import com.bibireden.opc.api.OfflinePlayerCacheAPI;
import com.bibireden.opc.cache.OfflinePlayerCache;
import com.bibireden.opc.cache.OfflinePlayerCacheData;
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
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// todo: We need to implement the cache into the `levelNbt`.
@Mixin(LevelProperties.class)
abstract class LevelPropertiesMixin implements OfflinePlayerCacheData {
    @Unique
    private final OfflinePlayerCache opcCache = new OfflinePlayerCache();

    @Inject(method = "updateProperties", at = @At("HEAD"))
    private void opc_updateProperties(DynamicRegistryManager registryManager, NbtCompound levelNbt, NbtCompound playerNbt, CallbackInfo ci) {
        levelNbt.put(OfflinePlayerCacheAPI.MOD_ID, opcCache.writeToNbt());
    }

    @Inject(method = "readProperties", at = @At("RETURN"))
    private static void opc_readProperties(Dynamic<NbtElement> dynamic, DataFixer dataFixer, int dataVersion, NbtCompound playerData, LevelInfo levelInfo, SaveVersionInfo saveVersionInfo, LevelProperties.SpecialProperty specialProperty, GeneratorOptions generatorOptions, Lifecycle lifecycle, CallbackInfoReturnable<LevelProperties> cir) {
        LevelProperties levelProperties = cir.getReturnValue();
        dynamic.get(OfflinePlayerCacheAPI.MOD_ID).result()
                .map(Dynamic::getValue)
                .ifPresent(nbt -> ((OfflinePlayerCacheData) levelProperties).offlinePlayerCache().readFromNbt((NbtList) nbt));
    }

    @NotNull
    @Override
    public OfflinePlayerCache offlinePlayerCache() {
        return this.opcCache;
    }
}
