package com.bibireden.opc

import com.bibireden.opc.cache.OfflinePlayerCache
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.command.argument.UuidArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*
import java.util.function.Function
import kotlin.math.abs

object OfflinePlayerCacheCommands {
    private val SUGGEST_KEYS = SuggestionProvider<ServerCommandSource?> { _, builder ->
        CommandSource.suggestIdentifiers(OfflinePlayerCache.keys(), builder)
    }
    private val SUGGEST_NAMES =
        SuggestionProvider<ServerCommandSource> { ctx: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder ->
            val server = ctx.source.server
            val cache = OfflinePlayerCache.get(server) ?: return@SuggestionProvider builder.buildFuture()
            cache.playerNames(server).forEach(builder::suggest)
            builder.buildFuture()
        }
    private val SUGGEST_UUIDS =
        SuggestionProvider<ServerCommandSource> { ctx: CommandContext<ServerCommandSource>, builder: SuggestionsBuilder ->
            val server = ctx.source.server
            val cache = OfflinePlayerCache.get(server) ?: return@SuggestionProvider builder.buildFuture()
            cache.playerIDs(server).forEach { id -> builder.suggest(java.lang.String.valueOf(id)) }
            builder.buildFuture()
        }

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(CommandManager.literal("opc")
            .requires { serverCommandSource: ServerCommandSource ->
                serverCommandSource.hasPermissionLevel(
                    2
                )
            }
            .then(CommandManager.literal("get")
                    .then(CommandManager.literal("name")
                            .then(CommandManager.argument("name", StringArgumentType.string())
                                    .suggests(SUGGEST_NAMES)
                                    .then(CommandManager.argument("key", IdentifierArgumentType.identifier())
                                            .suggests(SUGGEST_KEYS)
                                            .executes { context: CommandContext<ServerCommandSource> -> executeGetKey(context) { ctx: CommandContext<ServerCommandSource>? ->
                                                    StringArgumentType.getString(ctx, "name")
                                                }
                                            }
                                    )
                            )
                    )
                    .then(CommandManager.literal("uuid")
                            .then(CommandManager.argument("uuid", StringArgumentType.string())
                                    .suggests(SUGGEST_UUIDS)
                                    .then(CommandManager.argument("key", IdentifierArgumentType.identifier())
                                            .suggests(SUGGEST_KEYS)
                                            .executes { context: CommandContext<ServerCommandSource> -> executeGetKey(context) { ctx: CommandContext<ServerCommandSource>? ->
                                                    UuidArgumentType.getUuid(ctx, "uuid")
                                                }
                                            })
                            )
                    )
            )
            .then(CommandManager.literal("remove")
                .then(CommandManager.literal("name")
                    .then(CommandManager.argument("name", StringArgumentType.string())
                        .suggests(SUGGEST_NAMES)
                        .executes { context: CommandContext<ServerCommandSource> ->
                            executeRemoveAllCachedTo(
                                context
                            ) { ctx: CommandContext<ServerCommandSource>? ->
                                StringArgumentType.getString(
                                    ctx,
                                    "name"
                                )
                            }
                        }
                        .then(
                            CommandManager.argument("key", IdentifierArgumentType.identifier())
                                .suggests(SUGGEST_KEYS)
                                .executes { context: CommandContext<ServerCommandSource> ->
                                    executeRemoveKey(
                                        context
                                    ) { ctx: CommandContext<ServerCommandSource>? ->
                                        StringArgumentType.getString(
                                            ctx,
                                            "name"
                                        )
                                    }
                                })
                    )
                )
                .then(CommandManager.literal("uuid")
                    .then(CommandManager.argument("uuid", UuidArgumentType.uuid())
                        .suggests(SUGGEST_UUIDS)
                        .executes { context: CommandContext<ServerCommandSource> ->
                            executeRemoveAllCachedTo(
                                context
                            ) { ctx: CommandContext<ServerCommandSource>? ->
                                UuidArgumentType.getUuid(
                                    ctx,
                                    "uuid"
                                )
                            }
                        }
                        .then(
                            CommandManager.argument("key", IdentifierArgumentType.identifier())
                                .suggests(SUGGEST_KEYS)
                                .executes { context: CommandContext<ServerCommandSource> ->
                                    executeRemoveKey(context) { ctx: CommandContext<ServerCommandSource> -> UuidArgumentType.getUuid(ctx,"uuid") }
                                })
                    )
                )
            ).then(CommandManager.literal("list") //TODO fix listing of cache values
					.then(CommandManager.literal("name")
						.then(CommandManager.argument("name", StringArgumentType.string())
							.suggests(SUGGEST_NAMES)
								.executes { ctx -> executeListKeys(ctx) { ctx -> StringArgumentType.getString(ctx, "name")} }
					.then(CommandManager.literal("uuid")
						.then(CommandManager.argument("uuid", UuidArgumentType.uuid())
							.suggests(SUGGEST_UUIDS)
                            .executes { ctx -> executeListKeys(ctx)
                                { c -> UuidArgumentType.getUuid(c, "uuid")} }
                            )
                        )
                    )
                )
            )
        )
    }

    private fun <T> executeListKeys(context: CommandContext<ServerCommandSource>, input: (CommandContext<ServerCommandSource>) -> T): Int {
		val id = input(context);

		val server = context.source.server;
		val opc = OfflinePlayerCache.get(server) ?: return -1;

        val values = when (id) {
            is String -> opc.getPlayerCache(id)
            is UUID -> opc.getPlayerCache(id)
            else -> return -1;
        }

		if (values == null) {
			return -1;
		}

        values.forEach { (key, value) ->
            context.source.sendFeedback({Text.literal( "$id -> ${key.id()} = $value").formatted(Formatting.GRAY)}, false);
        }

		return 1;
	}

    private fun <T> executeRemoveKey(
        ctx: CommandContext<ServerCommandSource>,
        input: Function<CommandContext<ServerCommandSource>, T>
    ): Int {
        val id = input.apply(ctx)
        val identifier = IdentifierArgumentType.getIdentifier(ctx, "key")
        val value = OfflinePlayerCache.getKey(identifier)

        if (value == null) {
            ctx.source.sendFeedback({
                Text.literal(
                    id.toString() + " -> null key"
                ).formatted(Formatting.RED)
            }, false)
            return -1
        }

        val server = ctx.source.server

        val opc = OfflinePlayerCache.get(server) ?: return -1

        if (id is String) {
            opc.unCache(id, value)
        } else if (id is UUID) {
            opc.unCache(id, value)
        }

        ctx.source.sendFeedback({
            Text.literal(
                "-$id -$identifier"
            ).formatted(Formatting.GRAY)
        }, false)

        return 1
    }

    private fun <T> executeRemoveAllCachedTo(
        context: CommandContext<ServerCommandSource>,
        input: Function<CommandContext<ServerCommandSource>, T>
    ): Int {
        val uuidOrPlayer = input.apply(context)
        val server = context.source.server
        val opc = OfflinePlayerCache.get(server) ?: return -1
        val executed = (if (uuidOrPlayer is String) opc.unCache(uuidOrPlayer) else (uuidOrPlayer is UUID && opc.unCache(uuidOrPlayer)))
        context.source.sendFeedback({
            Text.literal(
                "-$uuidOrPlayer -*"
            ).formatted(Formatting.GRAY)
        }, false)
        return if (executed) 1 else -1
    }

    private fun <T> executeGetKey(
        ctx: CommandContext<ServerCommandSource>,
        input: (CommandContext<ServerCommandSource>) -> T
    ): Int {
        val id = input(ctx)
        val identifier = IdentifierArgumentType.getIdentifier(ctx, "key")
        val value = OfflinePlayerCache.getKey(identifier)

        if (value == null) {
            ctx.source.sendFeedback(
                { Text.literal(id.toString() + " -> null key").formatted(Formatting.RED) },
                false
            )
            return -1
        }

        val server = ctx.source.server

        val opc = OfflinePlayerCache.get(server) ?: return -1

        val obj: Any = ((if (id is String) opc.get(server, id, value) else (if (id is UUID) opc.get(
            server,
            id,
            value
        ) else null))!!)

        ctx.source.sendFeedback({ Text.literal(id.toString() + " -> " + identifier + " = " + obj).formatted(Formatting.GRAY)}, false)

        if (obj is Number) {
            return (abs((obj as Int).toDouble()) % 16).toInt()
        }

        return 1
    }
}