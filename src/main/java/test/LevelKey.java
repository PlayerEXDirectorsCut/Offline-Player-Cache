package test;

import com.bibireden.opc.api.CachedPlayerKey;
import com.bibireden.opc.api.OfflinePlayerCacheAPI;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LevelKey extends CachedPlayerKey<Integer> {
    public LevelKey() {
        super(new Identifier(OfflinePlayerCacheAPI.MOD_ID, "test-level-value-java"));
    }

    @Override
    public Integer get(@NotNull ServerPlayerEntity player) {
        // this would be up to the end user's interpretation, so for now, anything will suffice.
        return 404;
    }

    @Override
    public Integer readFromNbt(@NotNull NbtCompound tag) {
        return tag.getInt("level");
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, Object value) {
        tag.putInt("level", (Integer) value);
    }
}
