package com.sakana.datacap.selection;

import net.minecraft.util.math.BlockPos;

public class Selection {
    private BlockPos pos1;
    private BlockPos pos2;

    public BlockPos getPos1() {
        return pos1;
    }

    public void setPos1(BlockPos pos1) {
        this.pos1 = pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public void setPos2(BlockPos pos2) {
        this.pos2 = pos2;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }
}
