package com.sakana.nebulaestats.network;

import com.sakana.nebulaestats.DataCollectionMod;
import com.sakana.nebulaestats.selection.Selection;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketOpenExitRegionGui implements IMessage {
    private boolean hasPos1;
    private boolean hasPos2;
    private int pos1X;
    private int pos1Y;
    private int pos1Z;
    private int pos2X;
    private int pos2Y;
    private int pos2Z;
    private String regionsJson = "[]";

    public static PacketOpenExitRegionGui fromPlayer(EntityPlayerMP player) {
        PacketOpenExitRegionGui packet = new PacketOpenExitRegionGui();
        Selection selection = DataCollectionMod.SELECTIONS.getSelection(player.getUniqueID());
        if (selection != null) {
            packet.writePos1(selection.getPos1());
            packet.writePos2(selection.getPos2());
        }
        packet.regionsJson = DataCollectionMod.EXIT_REGIONS.toJson().toString();
        return packet;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        hasPos1 = buf.readBoolean();
        if (hasPos1) {
            pos1X = buf.readInt();
            pos1Y = buf.readInt();
            pos1Z = buf.readInt();
        }

        hasPos2 = buf.readBoolean();
        if (hasPos2) {
            pos2X = buf.readInt();
            pos2Y = buf.readInt();
            pos2Z = buf.readInt();
        }

        regionsJson = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(hasPos1);
        if (hasPos1) {
            buf.writeInt(pos1X);
            buf.writeInt(pos1Y);
            buf.writeInt(pos1Z);
        }

        buf.writeBoolean(hasPos2);
        if (hasPos2) {
            buf.writeInt(pos2X);
            buf.writeInt(pos2Y);
            buf.writeInt(pos2Z);
        }

        ByteBufUtils.writeUTF8String(buf, regionsJson);
    }

    public boolean hasPos1() {
        return hasPos1;
    }

    public boolean hasPos2() {
        return hasPos2;
    }

    public BlockPos getPos1() {
        return hasPos1 ? new BlockPos(pos1X, pos1Y, pos1Z) : null;
    }

    public BlockPos getPos2() {
        return hasPos2 ? new BlockPos(pos2X, pos2Y, pos2Z) : null;
    }

    public String getRegionsJson() {
        return regionsJson;
    }

    private void writePos1(BlockPos pos) {
        if (pos == null) {
            return;
        }
        hasPos1 = true;
        pos1X = pos.getX();
        pos1Y = pos.getY();
        pos1Z = pos.getZ();
    }

    private void writePos2(BlockPos pos) {
        if (pos == null) {
            return;
        }
        hasPos2 = true;
        pos2X = pos.getX();
        pos2Y = pos.getY();
        pos2Z = pos.getZ();
    }
}

