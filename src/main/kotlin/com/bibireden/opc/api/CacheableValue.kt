package com.bibireden.opc.api

import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier

interface CacheableValue<V> {
    fun get(): V;

    fun read(tag: NbtCompound);

    fun write(tag: NbtCompound, value: V);

    fun id(): Identifier;
}