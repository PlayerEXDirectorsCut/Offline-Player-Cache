package com.github.clevernucleus.opc_dc.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.clevernucleus.opc_dc.api.CacheableValue;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.authlib.GameProfile;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.WorldProperties;

public final class OfflinePlayerCacheImpl {
	private static final Map<Identifier, CacheableValue<?>> KEYS = new HashMap<>();
	private final Map<UUID, Map<CacheableValue<?>, ?>> cache;
	private final BiMap<String, UUID> nameToId;
	
	public OfflinePlayerCacheImpl() {
		this.cache = new HashMap<>();
		this.nameToId = HashBiMap.create();
	}
	
	@SuppressWarnings("unchecked")
	private <V> V getFromCache(final UUID uuid, final CacheableValue<V> key) {
		Map<CacheableValue<?>, ?> value = this.cache.get(uuid);
		
		if(value == null) return (V)null;
		return (V)value.getOrDefault(key, null);
	}
	
	private boolean isValid(final ServerPlayerEntity player, final BiFunction<UUID, String, Boolean> function) {
		if(player == null) return false;
		GameProfile profile = player.getGameProfile();
		
		if(profile == null) return false;
		UUID uuid = profile.getId();
		String name = profile.getName();
		
		if(uuid == null || name == null || name.isEmpty()) return false;
		return function.apply(uuid, name);
	}
	
	protected <V> V get(final MinecraftServer server, final UUID uuid, final CacheableValue<V> key) {
		if(uuid == null) return (V)null;
		ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
		
		if(player == null) return this.getFromCache(uuid, key);
		return key.get(player);
	}
	
	protected <V> V get(final MinecraftServer server, final String name, final CacheableValue<V> key) {
		if(name == null || name.isEmpty()) return (V)null;
		ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
		
		if(player == null) {
			UUID uuid = this.nameToId.get(name);
			
			if(uuid == null) return (V)null;
			return this.getFromCache(uuid, key);
		}
		
		return key.get(player);
	}
	
	protected Collection<UUID> playerIds(final MinecraftServer server) {
		Set<UUID> set = new HashSet<UUID>();
		this.nameToId.values().forEach(set::add);
		
		for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			GameProfile profile = player.getGameProfile();
			
			if(profile == null) continue;
			UUID uuid = profile.getId();
			
			if(uuid == null) continue;
			set.add(uuid);
		}
		
		return set;
	}
	
	protected Collection<String> playerNames(final MinecraftServer server) {
		Set<String> set = new HashSet<String>();
		this.nameToId.keySet().forEach(set::add);
		
		for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			GameProfile profile = player.getGameProfile();
			
			if(profile == null) continue;
			String name = profile.getName();
			
			if(name == null || name.isEmpty()) continue;
			set.add(name);
		}
		
		return set;
	}
	
	protected boolean isPlayerCached(final UUID uuid) {
		if(uuid == null) return false;
		return this.cache.containsKey(uuid);
	}
	
	protected boolean isPlayerCached(final String name) {
		if(name == null || name.isEmpty()) return false;
		return this.nameToId.containsKey(name);
	}
	
	public boolean cache(final ServerPlayerEntity player) {
		return this.isValid(player, (uuid, name) -> {
			Map<CacheableValue<?>, Object> value = new HashMap<>();
			KEYS.forEach((identifier, key) -> value.put(key, key.get(player)));
			this.cache.put(uuid, value);
			this.nameToId.forcePut(name, uuid);
			return true;
		});
	}
	
	public boolean uncache(final ServerPlayerEntity player) {
		return this.isValid(player, (uuid, name) -> {
			this.cache.remove(uuid);
			this.nameToId.inverse().remove(uuid);
			return true;
		});
	}
	
	public boolean uncache(final UUID uuid, final CacheableValue<?> key) {
		if(uuid == null || KEYS.containsKey(key.id())) return false;
		Map<CacheableValue<?>, ?> value = this.cache.get(uuid);
		
		if(value == null) return false;
		if(value.remove(key) != null) {
			if(value.isEmpty()) {
				this.cache.remove(uuid);
				this.nameToId.inverse().remove(uuid);
			}
			
			return true;
		}
		
		return false;
	}
	
	public boolean uncache(final String name, final CacheableValue<?> key) {
		if(name == null || name.isEmpty()) return false;
		UUID uuid = this.nameToId.get(name);
		return this.uncache(uuid, key);
	}
	
	public boolean uncache(final UUID uuid) {
		if(uuid == null) return false;
		this.cache.remove(uuid);
		return this.nameToId.inverse().remove(uuid) != null;
	}
	
	public boolean uncache(final String name) {
		if(name == null || name.isEmpty()) return false;
		UUID uuid = this.nameToId.get(name);
		return this.uncache(uuid);
	}
	
	public NbtList writeToNbt() {
		NbtList list = new NbtList();
		Map<UUID, String> names = this.nameToId.inverse();
		
		for(UUID uuid : this.cache.keySet()) {
			Map<CacheableValue<?>, ?> data = this.cache.get(uuid);
			NbtCompound entry = new NbtCompound();
			entry.putUuid("Uuid", uuid);
			entry.putString("Name", names.getOrDefault(uuid, ""));
			
			NbtCompound keys = new NbtCompound();
			
			for(CacheableValue<?> key : data.keySet()) {
				NbtCompound entry2 = new NbtCompound();
				key.writeToNbt(entry2, data.get(key));
				keys.put(key.id().toString(), entry2);
			}
			
			entry.put("Keys", keys);
			list.add(entry);
		}
		
		return list;
	}
	
	public void readFromNbt(final NbtList list) {
		if(list == null || list.isEmpty()) return;
		this.cache.clear();
		this.nameToId.clear();
		
		for(int i = 0; i < list.size(); i++) {
			NbtCompound entry = list.getCompound(i);
			NbtCompound keys = entry.getCompound("Keys");
			UUID uuid = entry.getUuid("Uuid");
			String name = entry.getString("Name");
			
			if(name.isEmpty()) continue;
			Map<CacheableValue<?>, Object> data = new HashMap<>();
			
			for(String id : keys.getKeys()) {
				CacheableValue<?> key = KEYS.get(new Identifier(id));
				
				if(key == null) continue;
				Object value = key.readFromNbt(keys.getCompound(id));
				data.put(key, value);
			}
			
			this.cache.put(uuid, data);
			this.nameToId.put(name, uuid);
		}
	}
	
	public static Collection<Identifier> keys() {
		return KEYS.keySet();
	}
	
	public static CacheableValue<?> getKey(final Identifier key) {
		return KEYS.getOrDefault(key, (CacheableValue<?>)null);
	}
	
	public static <T> T ifPresent(final MinecraftServer server, final T fallback, final Function<OfflinePlayerCacheImpl, T> function) {
		WorldProperties worldProperties = server.getOverworld().getLevelProperties();
		
		if(!(worldProperties instanceof OfflinePlayerCacheData)) return fallback;
		return function.apply(((OfflinePlayerCacheData)worldProperties).offlinePlayerCache());
	}
	
	@SuppressWarnings("unchecked")
	public static <V> CacheableValue<V> register(final CacheableValue<V> key) {
		return (CacheableValue<V>)KEYS.computeIfAbsent(key.id(), id -> key);
	}
}
