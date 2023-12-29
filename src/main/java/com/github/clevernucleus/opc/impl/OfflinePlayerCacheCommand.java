package com.github.clevernucleus.opc.impl;

import java.util.UUID;
import java.util.function.Function;

import com.github.clevernucleus.opc.api.CacheableValue;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class OfflinePlayerCacheCommand {
	private static final SuggestionProvider<ServerCommandSource> SUGGEST_KEYS = (context, builder) -> CommandSource.suggestIdentifiers(OfflinePlayerCacheImpl.keys(), builder);
	private static final SuggestionProvider<ServerCommandSource> SUGGEST_NAMES = (ctx, builder) -> {
		final MinecraftServer server = ctx.getSource().getServer();
		return OfflinePlayerCacheImpl.ifPresent(server, builder.buildFuture(), opc -> {
			opc.playerNames(server).forEach(builder::suggest);
			return builder.buildFuture();
		});
	};
	private static final SuggestionProvider<ServerCommandSource> SUGGEST_UUIDS = (ctx, builder) -> {
		final MinecraftServer server = ctx.getSource().getServer();
		return OfflinePlayerCacheImpl.ifPresent(server, builder.buildFuture(), opc -> {
			opc.playerIds(server).forEach(id -> builder.suggest(String.valueOf(id)));
			return builder.buildFuture();
		});
	};
	
	private static <T> ArgumentCommandNode<ServerCommandSource, Identifier> getKey(Function<CommandContext<ServerCommandSource>, T> input) {
		return CommandManager.argument("key", IdentifierArgumentType.identifier()).suggests(SUGGEST_KEYS).executes(ctx -> {
			T id = input.apply(ctx);
			Identifier identifier = IdentifierArgumentType.getIdentifier(ctx, "key");
			CacheableValue<?> value = OfflinePlayerCacheImpl.getKey(identifier);
			
			if(value == null) {
				ctx.getSource().sendFeedback(() -> (Text.literal(id + " -> null key")).formatted(Formatting.RED), false);
				return -1;
			}
			
			MinecraftServer server = ctx.getSource().getServer();
			
			return OfflinePlayerCacheImpl.ifPresent(server, -1, opc -> {
				Object obj = (id instanceof String ? opc.get(server, (String)id, value) : (id instanceof UUID ? opc.get(server, (UUID)id, value) : null));
				ctx.getSource().sendFeedback(() -> (Text.literal(id + " -> " + identifier + " = " + obj)).formatted(Formatting.GRAY), false);
				
				if(obj instanceof Number) {
					int number = (int)(Integer)obj;
					return Math.abs(number) % 16;
				}
				
				return 1;
			});
		}).build();
	}
	
	private static <T> ArgumentCommandNode<ServerCommandSource, Identifier> removeKey(Function<CommandContext<ServerCommandSource>, T> input) {
		return CommandManager.argument("key", IdentifierArgumentType.identifier()).suggests(SUGGEST_KEYS).executes(ctx -> {
			T id = input.apply(ctx);
			Identifier identifier = IdentifierArgumentType.getIdentifier(ctx, "key");
			CacheableValue<?> value = OfflinePlayerCacheImpl.getKey(identifier);
			
			if(value == null) {
				ctx.getSource().sendFeedback(() -> (Text.literal(id + " -> null key")).formatted(Formatting.RED), false);
				return -1;
			}
			
			MinecraftServer server = ctx.getSource().getServer();
			
			return OfflinePlayerCacheImpl.ifPresent(server, -1, opc -> {
				if(id instanceof String) {
					opc.uncache((String)id, value);
				} else if(id instanceof UUID) {
					opc.uncache((UUID)id, value);
				}
				
				ctx.getSource().sendFeedback(() -> (Text.literal("-" + id + " -" + identifier)).formatted(Formatting.GRAY), false);
				
				return 1;
			});
		}).build();
	}
	
	private static void get(LiteralCommandNode<ServerCommandSource> root) {
		LiteralCommandNode<ServerCommandSource> get = CommandManager.literal("get").build();
		LiteralCommandNode<ServerCommandSource> id1 = CommandManager.literal("name").build();
		LiteralCommandNode<ServerCommandSource> id2 = CommandManager.literal("uuid").build();
		ArgumentCommandNode<ServerCommandSource, String> name = CommandManager.argument("name", StringArgumentType.string()).suggests(SUGGEST_NAMES).build();
		ArgumentCommandNode<ServerCommandSource, Identifier> key1 = getKey(ctx -> StringArgumentType.getString(ctx, "name"));
		ArgumentCommandNode<ServerCommandSource, UUID> uuid = CommandManager.argument("uuid", UuidArgumentType.uuid()).suggests(SUGGEST_UUIDS).build();
		ArgumentCommandNode<ServerCommandSource, Identifier> key2 = getKey(ctx -> UuidArgumentType.getUuid(ctx, "uuid"));
		
		root.addChild(get);
		get.addChild(id1);
		get.addChild(id2);
		id1.addChild(name);
		id2.addChild(uuid);
		name.addChild(key1);
		uuid.addChild(key2);
	}
	
	private static void remove(LiteralCommandNode<ServerCommandSource> root) {
		LiteralCommandNode<ServerCommandSource> remove = CommandManager.literal("remove").build();
		LiteralCommandNode<ServerCommandSource> id1 = CommandManager.literal("name").build();
		LiteralCommandNode<ServerCommandSource> id2 = CommandManager.literal("uuid").build();
		ArgumentCommandNode<ServerCommandSource, String> name = CommandManager.argument("name", StringArgumentType.string()).suggests(SUGGEST_NAMES).executes(ctx -> {
			final MinecraftServer server = ctx.getSource().getServer();
			return OfflinePlayerCacheImpl.ifPresent(server, -1, opc -> {
				String player = StringArgumentType.getString(ctx, "name");
				opc.uncache(player);
				ctx.getSource().sendFeedback(() -> (Text.literal("-" + player + " -*")).formatted(Formatting.GRAY), false);
				return 1;
			});
		}).build();
		ArgumentCommandNode<ServerCommandSource, Identifier> key1 = removeKey(ctx -> StringArgumentType.getString(ctx, "name"));
		ArgumentCommandNode<ServerCommandSource, UUID> uuid = CommandManager.argument("uuid", UuidArgumentType.uuid()).suggests(SUGGEST_UUIDS).executes(ctx -> {
			final MinecraftServer server = ctx.getSource().getServer();
			return OfflinePlayerCacheImpl.ifPresent(server, -1, opc -> {
				UUID player = UuidArgumentType.getUuid(ctx, "uuid");
				opc.uncache(player);
				ctx.getSource().sendFeedback(() -> (Text.literal("-" + player + " -*")).formatted(Formatting.GRAY), false);
				return 1;
			});
		}).build();
		ArgumentCommandNode<ServerCommandSource, Identifier> key2 = removeKey(ctx -> UuidArgumentType.getUuid(ctx, "uuid"));
		
		root.addChild(remove);
		remove.addChild(id1);
		remove.addChild(id2);
		id1.addChild(name);
		id2.addChild(uuid);
		name.addChild(key1);
		uuid.addChild(key2);
	}
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralCommandNode<ServerCommandSource> root = CommandManager.literal("opc").requires(source -> source.hasPermissionLevel(2)).build();
		dispatcher.getRoot().addChild(root);
		get(root);
		remove(root);
	}
}
