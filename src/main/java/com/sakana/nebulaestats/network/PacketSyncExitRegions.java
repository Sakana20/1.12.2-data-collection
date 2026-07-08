package com.sakana.nebulaestats.network;

import com.sakana.nebulaestats.DataCollectionMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PacketSyncExitRegions implements IMessage {
    private String regionsJson = "[]";

    public static PacketSyncExitRegions fromCurrentRegions() {
        PacketSyncExitRegions packet = new PacketSyncExitRegions();
        packet.regionsJson = DataCollectionMod.EXIT_REGIONS.toJson().toString();
        return packet;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        regionsJson = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, regionsJson);
    }

    public String getRegionsJson() {
        return regionsJson;
    }
}

