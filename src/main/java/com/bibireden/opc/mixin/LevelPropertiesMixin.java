package com.bibireden.opc.mixin;

import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;

// todo: We need to implement the cache into the `levelNbt`.
@Mixin(LevelProperties.class)
abstract class LevelPropertiesMixin {}
