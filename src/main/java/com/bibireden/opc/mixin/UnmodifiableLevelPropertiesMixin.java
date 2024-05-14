package com.bibireden.opc.mixin;

import net.minecraft.world.level.UnmodifiableLevelProperties;
import org.spongepowered.asm.mixin.Mixin;

// todo: We will access our data from the `ServerWorldProperties` within this mixin.
@Mixin(UnmodifiableLevelProperties.class)
abstract class UnmodifiableLevelPropertiesMixin {}
