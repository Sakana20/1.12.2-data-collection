package com.sakana.nebulaestats.network;

import com.sakana.nebulaestats.DataCollectionMod;
import com.sakana.nebulaestats.exit.ExitRegion;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTeleportToExitRegion implements IMessage {
    private String id = "";

    public PacketTeleportToExitRegion() {
    }

    public PacketTeleportToExitRegion(String id) {
        this.id = id;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        id = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, id);
    }

    public static class Handler implements IMessageHandler<PacketTeleportToExitRegion, IMessage> {
        @Override
        public IMessage onMessage(final PacketTeleportToExitRegion message, final MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    handle(message, player);
                }
            });
            return null;
        }

        private void handle(PacketTeleportToExitRegion message, EntityPlayerMP player) {
            String id = message.id == null ? "" : message.id;
            ExitRegion region = DataCollectionMod.EXIT_REGIONS.get(id);
            if (region == null) {
                DataCollectionMod.sendMessage(player, "\u672a\u627e\u5230\u51fa\u53e3\u533a\u57df\uff1a" + id);
                NetworkHandler.syncExitRegions(player);
                return;
            }

            double x = region.getMin().getX() + 0.5D;
            double y = region.getMin().getY() + 1.0D;
            double z = region.getMin().getZ() + 0.5D;
            player.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
            DataCollectionMod.sendMessage(player, "\u5df2\u4f20\u9001\u5230\u51fa\u53e3\u533a\u57df " + id + "\u3002");
            NetworkHandler.syncExitRegions(player);
        }
    }
}

