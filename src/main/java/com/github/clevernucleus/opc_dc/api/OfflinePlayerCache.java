package com.github.clevernucleus.opc_dc.api;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.server.MinecraftServer;

/**
 * Interface and API to use methods from the offline player cache.
 * 
 * @author CleverNucleus
 *
 */
public interface OfflinePlayerCache {
	/** The mod id. */
	public static final String MODID = "opc";
	
	/**
	 * Registers a cacheable value to the server: these are keys that instruct the server to cache some data from players when they disconnect.
	 * @param <V>
	 * @param key
	 * @return
	 */
	public static <V> CacheableValue<V> register(final CacheableValue<V> key) {
		return com.github.clevernucleus.opc_dc.impl.OfflinePlayerCacheImpl.register(key);
	}
	
	/**
	 * Get access to the offline player cache object. This should only be used on the logical server.
	 * @param <T>
	 * @param server
	 * @param fallback
	 * @param function
	 * @return
	 */
	public static <T> T getOfflinePlayerCache(final MinecraftServer server, final T fallback, final Function<OfflinePlayerCache, T> function) {
		com.github.clevernucleus.opc_dc.impl.OfflinePlayerCacheProvider provider = new com.github.clevernucleus.opc_dc.impl.OfflinePlayerCacheProvider(server);
		
		if(provider.isEmpty()) return fallback;
		return function.apply(provider);
	}
	
	/**
	 * If the Player is offline and exists in the cache, retrieves the last cached value. If the player is online, retrieves the player's 
	 * current value.
	 * @param <V>
	 * @param uuid Player UUID
	 * @param key
	 * @return
	 */
	<V> V get(final UUID uuid, final CacheableValue<V> key);
	
	/**
	 * If the Player is offline and exists in the cache, retrieves the last cached value. If the player is online, retrieves the player's 
	 * current value.
	 * @param <V>
	 * @param uuid Player Name
	 * @param key
	 * @return
	 */
	<V> V get(final String name, final CacheableValue<V> key);
	
	/**
	 * @return Returns all offline/cached and online players' UUIDs.
	 */
	Collection<UUID> playerIds();
	
	/**
	 * @return Returns all offline/cached and online players' names.
	 */
	Collection<String> playerNames();
	
	/**
	 * Tests if the player with the input UUID exists in the cache.
	 * @param uuid
	 * @return
	 */
	boolean isPlayerCached(final UUID uuid);
	
	/**
	 * Tests if the player with the input name exists in the cache.
	 * @param uuid
	 * @return
	 */
	boolean isPlayerCached(final String name);
}
