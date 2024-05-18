package com.bibireden.opc

import com.bibireden.opc.api.OfflinePlayerCacheAPI
import com.bibireden.opc.cache.OfflinePlayerCache
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.commands.arguments.UuidArgument
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import java.util.UUID
import kotlin.math.abs

/**
 * The command-tree for the **`OfflinePlayerCache`**.
 *
 * Was converted to the tree-format of `brigadier` for a cleaner look.
 *
 * @author OverlordsIII, bibi-reden
 * */
internal object OfflinePlayerCacheCommands {
    private val SUGGEST_KEYS = SuggestionProvider<CommandSourceStack> { _, builder ->
        SharedSuggestionProvider.suggestResource(OfflinePlayerCacheAPI.keys().stream(), builder)
        builder.buildFuture()
    }
    private val SUGGEST_NAMES = SuggestionProvider<CommandSourceStack> { ctx, builder ->
        OfflinePlayerCacheAPI(ctx.source.server).usernames().forEach(builder::suggest)
        builder.buildFuture()
    }
    private val SUGGEST_UUIDS = SuggestionProvider<CommandSourceStack> { ctx, builder ->
        OfflinePlayerCacheAPI(ctx.source.server).uuids().forEach { builder.suggest(it.toString()) }
        builder.buildFuture()
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(Commands.literal("opc")
            .requires { it.hasPermission(2) }
            .then(Commands.literal("get")
                    .then(Commands.literal("name")
                            .then(Commands.argument("name", StringArgumentType.string())
                                    .suggests(SUGGEST_NAMES)
                                    .then(Commands.argument("key", ResourceLocationArgument.id())
                                            .suggests(SUGGEST_KEYS)
                                            .executes { context -> executeGetKey(context) { ctx -> StringArgumentType.getString(ctx, "name") } }
                                    )
                            )
                    )
                    .then(Commands.literal("uuid")
                            .then(Commands.argument("uuid", UuidArgument.uuid())
                                    .suggests(SUGGEST_UUIDS)
                                    .then(Commands.argument("key", ResourceLocationArgument.id())
                                            .suggests(SUGGEST_KEYS)
                                            .executes { context -> executeGetKey(context) {
                                                ctx -> UuidArgument.getUuid(ctx, "uuid") }
                                            }
                                    )
                            )
                    )
            )
            .then(Commands.literal("remove")
                .then(Commands.literal("name")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(SUGGEST_NAMES)
                        .executes { context ->
                            executeRemoveAllCachedTo(context) { ctx -> StringArgumentType.getString(ctx, "name") }
                        }
                        .then(Commands.argument("key", ResourceLocationArgument.id())
                            .suggests(SUGGEST_KEYS)
                            .executes { context ->
                                executeRemoveKey(context) { ctx -> StringArgumentType.getString(ctx, "name") }
                            }
                        )
                    )
                )
                .then(Commands.literal("uuid")
                    .then(Commands.argument("uuid", UuidArgument.uuid())
                        .suggests(SUGGEST_UUIDS)
                        .executes { context ->
                            executeRemoveAllCachedTo(context) { ctx -> UuidArgument.getUuid(ctx, "uuid") }
                        }
                        .then(Commands.argument("key", ResourceLocationArgument.id())
                            .suggests(SUGGEST_KEYS)
                            .executes { context ->
                                executeRemoveKey(context) { ctx: CommandContext<CommandSourceStack> -> UuidArgument.getUuid(ctx,"uuid") }
                            }
                        )
                    )
                )
            )
            .then(Commands.literal("list")
                .then(Commands.literal("name")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(SUGGEST_NAMES)
                        .executes { context ->
                            executeListKeys(context) { ctx -> StringArgumentType.getString(ctx, "name")}
                        }
                    )
                )
                .then(Commands.literal("uuid")
                    .then(Commands.argument("uuid", UuidArgument.uuid())
                        .suggests(SUGGEST_UUIDS)
                        .executes { context ->
                            executeListKeys(context) { ctx -> UuidArgument.getUuid(ctx, "uuid")}
                        }
                    )
                )
            )
        )
    }

    private fun <T> executeListKeys(ctx: CommandContext<CommandSourceStack>, input: (CommandContext<CommandSourceStack>) -> T): Int {
		val id = input(ctx);

		val api = OfflinePlayerCacheAPI(ctx.source.server)

        val (values, otherID) = when (id) {
            is String -> api.getPlayerCache(id) to OfflinePlayerCache.get(api.server).getUUID(api.server, id as String)
            is UUID -> api.getPlayerCache(id) to OfflinePlayerCache.get(api.server).getUsername(api.server, id as UUID)
            else -> null;
        } ?: return -1;

        ctx.source.sendSuccess(fetchingMessage(id), false)

        if (values.isEmpty()) {
            ctx.source.sendSuccess({Component.literal("No values for: $id@$otherID").withStyle(ChatFormatting.GRAY)}, false)
        }
        else {
            ctx.source.sendSuccess({Component.literal("Found: $otherID").withStyle(ChatFormatting.GREEN)}, false)
            ctx.source.sendSuccess({Component.literal("Listing [${values.size}] value(s):").withStyle(ChatFormatting.GREEN)}, false)
            values.forEach { (key, value) ->
                ctx.source.sendSuccess({Component.literal( "${key.id} = $value").withStyle(ChatFormatting.WHITE)}, false);
            }
        }

		return 1;
	}

    private fun <T> executeRemoveKey(ctx: CommandContext<CommandSourceStack>, input: (CommandContext<CommandSourceStack>) -> T): Int {
        val id = input(ctx)
        val identifier = ResourceLocationArgument.getId(ctx, "key")
        val value = OfflinePlayerCache.getKey(identifier)

        if (value == null) {
            ctx.source.sendSuccess(nullKeyMessage(id), false)
            return -1
        }

        val opc = OfflinePlayerCache.get(ctx.source.server)

        when (id) {
            is String -> opc.unCache(id, value)
            is UUID -> opc.unCache(id, value)
        }

        ctx.source.sendSuccess({ Component.literal("$id: un-cached [$identifier]").withStyle(ChatFormatting.WHITE) }, false)

        return 1
    }

    private fun <T> executeRemoveAllCachedTo(context: CommandContext<CommandSourceStack>, input: (CommandContext<CommandSourceStack>) -> T): Int {
        val uuidOrPlayer = input(context)
        val opc = OfflinePlayerCache.get(context.source.server)

        val executed = when (uuidOrPlayer) {
            is String -> opc.unCache(uuidOrPlayer)
            is UUID -> opc.unCache(uuidOrPlayer)
            else -> false;
        }

        context.source.sendSuccess({ Component.literal( "$uuidOrPlayer: cleared" ).withStyle(ChatFormatting.WHITE) }, false)

        return if (executed) 1 else -1
    }

    private fun <T> executeGetKey(ctx: CommandContext<CommandSourceStack>, input: (CommandContext<CommandSourceStack>) -> T): Int {
        val id = input(ctx)
        val identifier = ResourceLocationArgument.getId(ctx, "key")
        val key = OfflinePlayerCache.getKey(identifier)

        if (key == null) {
            ctx.source.sendSuccess(nullKeyMessage(id), false)
            return -1
        }

        val server = ctx.source.server

        val api = OfflinePlayerCacheAPI(server)

        val (value, otherId) = when (id) {
            is String -> (api.get(id, key) to OfflinePlayerCache.get(api.server).getUUID(api.server, id as String))
            is UUID -> (api.get(id, key) to OfflinePlayerCache.get(api.server).getUsername(api.server, id as UUID))
            else -> null
        } ?: return -1;

        ctx.source.sendSuccess(fetchingMessage(id), false)
        ctx.source.sendSuccess({Component.literal("Found: $otherId").withStyle(ChatFormatting.GREEN)}, false)
        ctx.source.sendSuccess({ Component.literal("$identifier = $value").withStyle(ChatFormatting.WHITE)}, false)

        if (value is Number) {
            return (abs(value.toDouble()) % 16).toInt()
        }

        return 1
    }

    private fun <T> fetchingMessage(id: T): () -> MutableComponent = { Component.literal("Fetching: $id").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.GOLD) }
    private fun <T> nullKeyMessage(id: T): () -> MutableComponent = { Component.literal("$id -> <null_key>").withStyle(ChatFormatting.RED) }
}