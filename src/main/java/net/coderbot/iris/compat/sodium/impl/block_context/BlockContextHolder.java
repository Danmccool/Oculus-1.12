package net.coderbot.iris.compat.sodium.impl.block_context;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.block.state.IBlockState;

public class BlockContextHolder {
    private final Object2IntMap<IBlockState> blockStateIds;

    public int localPosX;
    public int localPosY;
    public int localPosZ;

    public short blockId;
    public short renderType;

    public BlockContextHolder() {
        this.blockStateIds = Object2IntMaps.emptyMap();
        this.blockId = -1;
        this.renderType = -1;
    }

    public BlockContextHolder(Object2IntMap<IBlockState> idMap) {
        this.blockStateIds = idMap;
        this.blockId = -1;
        this.renderType = -1;
    }

    public void setLocalPos(int localPosX, int localPosY, int localPosZ) {
        this.localPosX = localPosX;
        this.localPosY = localPosY;
        this.localPosZ = localPosZ;
    }

    public void set(IBlockState state, short renderType) {
        this.blockId = this.blockStateIds.getOrDefault(state, -1).shortValue();
        this.renderType = renderType;
    }

    public void reset() {
        this.blockId = -1;
        this.renderType = -1;
        this.localPosX = 0;
        this.localPosY = 0;
        this.localPosZ = 0;
    }
}
