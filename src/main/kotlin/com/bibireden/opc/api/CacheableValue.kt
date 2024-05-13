package com.bibireden.opc.api

import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier

interface CacheableValue<T> {
    fun get(): T
    fun getID(): Identifier

    fun readFromNBT(tag: NbtCompound): T
    fun writeToNBT(tag: NbtCompound)

}