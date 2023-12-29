package com.github.clevernucleus.opc_dc.api;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * This is the base abstract CacheableValue key to be implemented. Serves as a key to get specific data, as well as an 
 * instruction set to tell the server how to read/write this data and how to get the data from a player initially when 
 * cached as well as when the player is online.
 * 
 * @author CleverNucleus
 *
 * @param <V> The value type to be cached: can be anything (primitives, objects) as long as you have a valid read/write from/to 
 * nbt implementation.
 */
public interface CacheableValue<V> {
	
	/**
	 * When a player is online, gets the value from the player. When the player disconnects, this is used to get the value 
	 * to store in the server cache.
	 * @param player
	 * @return
	 */
	public V get(final ServerPlayerEntity player);
	
	/** 
	 * Reads the value from nbt.
	 * @param tag
	 * @return
	 */
	public V readFromNbt(final NbtCompound tag);
	
	/**
	 * Writes the value to nbt.
	 * @param tag
	 * @param value
	 */
	public void writeToNbt(final NbtCompound tag, final Object value);
	
	/**
	 * @return This value's 'key': should be in the form <code>modid:value_name</code>; example: <code>opc:current_health</code>.
	 */
	public Identifier id();
}
