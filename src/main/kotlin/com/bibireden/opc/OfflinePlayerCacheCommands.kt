package com.bibireden.opc

import com.bibireden.opc.api.OfflinePlayerCacheAPI
import com.bibireden.opc.cache.OfflinePlayerCache
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.command.argument.UuidArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*
import kotlin.math.abs

/**
 * The command-tree for the **`OfflinePlayerCache`**.
 *
 * Was converted to the tree-format of `brigadier` for a cleaner look.
 *
 * @author OverlordsIII, bibi-reden
 * */
internal object OfflinePlayerCacheCommands {
    private val SUGGEST_KEYS = SuggestionProvider<ServerCommandSource> { _, builder ->
        CommandSource.suggestIdentifiers(OfflinePlayerCacheAPI.keys(), builder)
    }
    private val SUGGEST_NAMES = SuggestionProvider<ServerCommandSource> { ctx, builder ->
        OfflinePlayerCacheAPI(ctx.source.server).usernames().forEach(builder::suggest)
        builder.buildFuture()
    }
    private val SUGGEST_UUIDS = SuggestionProvider<ServerCommandSource> { ctx, builder ->
        OfflinePlayerCacheAPI(ctx.source.server).uuids().forEach { builder.suggest(it.toString()) }
        builder.buildFuture()
    }

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(CommandManager.literal("opc")
            .requires { it.hasPermissionLevel(2) }
            .then(CommandManager.literal("get")
                    .then(CommandManager.literal("name")
                            .then(CommandManager.argument("name", StringArgumentType.string())
                                    .suggests(SUGGEST_NAMES)
                                    .then(CommandManager.argument("key", IdentifierArgumentType.identifier())
                                            .suggests(SUGGEST_KEYS)
                                            .executes { context -> executeGetKey(context) { ctx -> StringArgumentType.getString(ctx, "name") } }
                                    )
                            )
                    )
                    .then(CommandManager.literal("uuid")
                            .then(CommandManager.argument("uuid", UuidArgumentType.uuid())
                                    .suggests(SUGGEST_UUIDS)
                                    .then(CommandManager.argument("key", IdentifierArgumentType.identifier())
                                            .suggests(SUGGEST_KEYS)
                                            .executes { context -> executeGetKey(context) {
                                                ctx -> UuidArgumentType.getUuid(ctx, "uuid") }
                                            }
                                    )
                            )
                    )
            )
            .then(CommandManager.literal("remove")
                .then(CommandManager.literal("name")
                    .then(CommandManager.argument("name", StringArgumentType.string())
                        .suggests(SUGGEST_NAMES)
                        .executes { context ->
                            executeRemoveAllCachedTo(context) { ctx -> StringArgumentType.getString(ctx, "name") }
                        }
                        .then(CommandManager.argument("key", IdentifierArgumentType.identifier())
                            .suggests(SUGGEST_KEYS)
                            .executes { context ->
                                executeRemoveKey(context) { ctx -> StringArgumentType.getString(ctx, "name") }
                            }
                        )
                    )
                )
                .then(CommandManager.literal("uuid")
                    .then(CommandManager.argument("uuid", UuidArgumentType.uuid())
                        .suggests(SUGGEST_UUIDS)
                        .executes { context ->
                            executeRemoveAllCachedTo(context) { ctx -> UuidArgumentType.getUuid(ctx, "uuid") }
                        }
                        .then(CommandManager.argument("key", IdentifierArgumentType.identifier())
                            .suggests(SUGGEST_KEYS)
                            .executes { context ->
                                executeRemoveKey(context) { ctx: CommandContext<ServerCommandSource> -> UuidArgumentType.getUuid(ctx,"uuid") }
                            }
                        )
                    )
                )
            )
            .then(CommandManager.literal("list")
                .then(CommandManager.literal("name")
                    .then(CommandManager.argument("name", StringArgumentType.string())
                        .suggests(SUGGEST_NAMES)
                        .executes { context ->
                            executeListKeys(context) { ctx -> StringArgumentType.getString(ctx, "name")}
                        }
                    )
                )
                .then(CommandManager.literal("uuid")
                    .then(CommandManager.argument("uuid", UuidArgumentType.uuid())
                        .suggests(SUGGEST_UUIDS)
                        .executes { context ->
                            executeListKeys(context) { ctx -> UuidArgumentType.getUuid(ctx, "uuid")}
                        }
                    )
                )
            )
        )
    }

    private fun <T> executeListKeys(ctx: CommandContext<ServerCommandSource>, input: (CommandContext<ServerCommandSource>) -> T): Int {
		val id = input(ctx);

		val opcApi = OfflinePlayerCacheAPI(ctx.source.server)

        val (values, otherID) = when (id) {
            is String -> opcApi.getPlayerCache(id) to OfflinePlayerCache.get(opcApi.server)?.getUUID(opcApi.server, id as String)
            is UUID -> opcApi.getPlayerCache(id) to OfflinePlayerCache.get(opcApi.server)?.getUsername(opcApi.server, id as UUID)
            else -> null;
        } ?: return -1;

        ctx.source.sendFeedback(fetchingMessage(id), false)

        if (values.isNullOrEmpty()) {
            ctx.source.sendFeedback({Text.literal("No values for: $id@$otherID").formatted(Formatting.GRAY)}, false)
        }
        else {
            ctx.source.sendFeedback({Text.literal("Found: $otherID").formatted(Formatting.GREEN)}, false)
            ctx.source.sendFeedback({Text.literal("Listing [${values.size}] value(s):").formatted(Formatting.GREEN)}, false)
            values.forEach { (key, value) ->
                ctx.source.sendFeedback({Text.literal( "${key.id} = $value").formatted(Formatting.WHITE)}, false);
            }
        }

		return 1;
	}

    private fun <T> executeRemoveKey(ctx: CommandContext<ServerCommandSource>, input: (CommandContext<ServerCommandSource>) -> T): Int {
        val id = input(ctx)
        val identifier = IdentifierArgumentType.getIdentifier(ctx, "key")
        val value = OfflinePlayerCache.getKey(identifier)

        if (value == null) {
            ctx.source.sendFeedback(nullKeyMessage(id), false)
            return -1
        }

        val opc = OfflinePlayerCache.get(ctx.source.server) ?: return -1

        when (id) {
            is String -> opc.unCache(id, value)
            is UUID -> opc.unCache(id, value)
        }

        ctx.source.sendFeedback({ Text.literal("$id: un-cached [$identifier]").formatted(Formatting.WHITE) }, false)

        return 1
    }

    private fun <T> executeRemoveAllCachedTo(context: CommandContext<ServerCommandSource>, input: (CommandContext<ServerCommandSource>) -> T): Int {
        val uuidOrPlayer = input(context)
        val opc = OfflinePlayerCache.get(context.source.server) ?: return -1

        val executed = when (uuidOrPlayer) {
            is String -> opc.unCache(uuidOrPlayer)
            is UUID -> opc.unCache(uuidOrPlayer)
            else -> false;
        }

        context.source.sendFeedback({ Text.literal( "$uuidOrPlayer: cleared" ).formatted(Formatting.WHITE) }, false)

        return if (executed) 1 else -1
    }

    private fun <T> executeGetKey(ctx: CommandContext<ServerCommandSource>, input: (CommandContext<ServerCommandSource>) -> T): Int {
        val id = input(ctx)
        val identifier = IdentifierArgumentType.getIdentifier(ctx, "key")
        val key = OfflinePlayerCache.getKey(identifier)

        if (key == null) {
            ctx.source.sendFeedback(nullKeyMessage(id), false)
            return -1
        }

        val server = ctx.source.server

        val api = OfflinePlayerCacheAPI(server)

        val (value, otherId) = when (id) {
            is String -> (api.get(id, key) to OfflinePlayerCache.get(api.server)?.getUUID(api.server, id as String))
            is UUID -> (api.get(id, key) to OfflinePlayerCache.get(api.server)?.getUsername(api.server, id as UUID))
            else -> null
        } ?: return -1;

        ctx.source.sendFeedback(fetchingMessage(id), false)
        ctx.source.sendFeedback({Text.literal("Found: $otherId").formatted(Formatting.GREEN)}, false)
        ctx.source.sendFeedback({ Text.literal("$identifier = $value").formatted(Formatting.WHITE)}, false)

        if (value is Number) {
            return (abs(value.toDouble()) % 16).toInt()
        }

        return 1
    }

    private fun <T> fetchingMessage(id: T): () -> MutableText = { Text.literal("Fetching: $id").formatted(Formatting.BOLD).formatted(Formatting.GOLD) }
    private fun <T> nullKeyMessage(id: T): () -> MutableText = { Text.literal("$id -> <null_key>").formatted(Formatting.RED) }
}