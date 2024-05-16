package com.bibireden.opc.mixin;

import com.bibireden.opc.cache.OfflinePlayerCache;
import com.bibireden.opc.cache.OfflinePlayerCacheData;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(UnmodifiableLevelProperties.class)
abstract class UnmodifiableLevelPropertiesMixin implements OfflinePlayerCacheData {
    @Final
    @Shadow
    private ServerWorldProperties worldProperties;

    @NotNull
    @Override
    public OfflinePlayerCache offlinePlayerCache() {
        return ((OfflinePlayerCacheData) this.worldProperties).offlinePlayerCache();
    }
}
