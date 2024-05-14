package com.bibireden.opc.mixin;

import net.minecraft.server.command.CommandManager;
import org.spongepowered.asm.mixin.Mixin;

// todo: We will need to implement our own Command root to the CommandManager's dispatcher.
@Mixin(CommandManager.class)
abstract class CommandManagerMixin {}
