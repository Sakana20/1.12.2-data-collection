package com.sakana.nebulaestats.network;

import com.sakana.nebulaestats.DataCollectionMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

public class PacketDeleteExitRegion implements IMessage {
    private String id = "";

    public PacketDeleteExitRegion() {
    }

    public PacketDeleteExitRegion(String id) {
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

    public static class Handler implements IMessageHandler<PacketDeleteExitRegion, IMessage> {
        @Override
        public IMessage onMessage(final PacketDeleteExitRegion message, final MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    handle(message, player);
                }
            });
            return null;
        }

        private void handle(PacketDeleteExitRegion message, EntityPlayerMP player) {
            String id = message.id == null ? "" : message.id;
            if (!DataCollectionMod.EXIT_REGIONS.remove(id)) {
                DataCollectionMod.sendMessage(player, "\u672a\u627e\u5230\u51fa\u53e3\u533a\u57df\uff1a" + id);
                NetworkHandler.syncExitRegions(player);
                return;
            }

            if (!save(player)) {
                NetworkHandler.syncExitRegions(player);
                return;
            }

            DataCollectionMod.sendMessage(player, "\u5df2\u5220\u9664\u51fa\u53e3\u533a\u57df " + id + "\u3002");
            NetworkHandler.syncExitRegions(player);
        }

        private boolean save(EntityPlayerMP player) {
            MinecraftServer server = player.getServer();
            if (server == null) {
                DataCollectionMod.sendMessage(player, "\u4fdd\u5b58\u51fa\u53e3\u533a\u57df\u5931\u8d25\uff1a\u670d\u52a1\u7aef\u5bf9\u8c61\u7f3a\u5931\u3002");
                return false;
            }

            try {
                DataCollectionMod.EXIT_REGION_STORAGE.save(server);
                return true;
            } catch (IOException exception) {
                DataCollectionMod.sendMessage(player, "\u4fdd\u5b58\u51fa\u53e3\u533a\u57df\u5931\u8d25\uff1a" + exception.getMessage());
                return false;
            }
        }
    }
}

