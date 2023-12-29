package com.github.clevernucleus.opc_dc.impl;

import java.util.Collection;
import java.util.UUID;

import com.github.clevernucleus.opc_dc.api.CacheableValue;
import com.github.clevernucleus.opc_dc.api.OfflinePlayerCache;

import net.minecraft.server.MinecraftServer;

public final class OfflinePlayerCacheProvider implements OfflinePlayerCache {
	private final MinecraftServer server;
	private final OfflinePlayerCacheImpl impl;
	
	public OfflinePlayerCacheProvider(final MinecraftServer server) {
		this.server = server;
		this.impl = OfflinePlayerCacheImpl.ifPresent(server, (OfflinePlayerCacheImpl)null, opc -> opc);
	}
	
	public boolean isEmpty() {
		return this.impl == (OfflinePlayerCacheImpl)null;
	}
	
	@Override
	public <V> V get(final UUID uuid, final CacheableValue<V> key) {
		return this.impl.get(this.server, uuid, key);
	}
	
	@Override
	public <V> V get(final String name, final CacheableValue<V> key) {
		return this.impl.get(this.server, name, key);
	}
	
	@Override
	public Collection<UUID> playerIds() {
		return this.impl.playerIds(this.server);
	}
	
	@Override
	public Collection<String> playerNames() {
		return this.impl.playerNames(this.server);
	}
	
	@Override
	public boolean isPlayerCached(final UUID uuid) {
		return this.impl.isPlayerCached(uuid);
	}
	
	@Override
	public boolean isPlayerCached(final String name) {
		return this.impl.isPlayerCached(name);
	}
}
