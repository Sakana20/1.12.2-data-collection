package com.sakana.nebulaestats.network;

import com.sakana.nebulaestats.DataCollectionMod;
import com.sakana.nebulaestats.exit.ExitRegion;
import com.sakana.nebulaestats.exit.ExitRegionIds;
import com.sakana.nebulaestats.selection.Selection;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

public class PacketCreateExitRegion implements IMessage {
    private String id = "";

    public PacketCreateExitRegion() {
    }

    public PacketCreateExitRegion(String id) {
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

    public static class Handler implements IMessageHandler<PacketCreateExitRegion, IMessage> {
        @Override
        public IMessage onMessage(final PacketCreateExitRegion message, final MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    handle(message, player);
                }
            });
            return null;
        }

        private void handle(PacketCreateExitRegion message, EntityPlayerMP player) {
            String id = message.id == null ? "" : message.id.trim();
            if (!ExitRegionIds.isValid(id)) {
                DataCollectionMod.sendMessage(player, ExitRegionIds.ID_RULE_MESSAGE);
                NetworkHandler.syncExitRegions(player);
                return;
            }

            Selection selection = DataCollectionMod.SELECTIONS.getSelection(player.getUniqueID());
            if (selection == null || !selection.isComplete()) {
                DataCollectionMod.sendMessage(player, "\u8bf7\u5148\u5de6\u952e\u9009\u62e9 pos1\uff0c\u518d\u53f3\u952e\u65b9\u5757\u9009\u62e9 pos2\u3002");
                NetworkHandler.syncExitRegions(player);
                return;
            }

            ExitRegion region = new ExitRegion(id, selection.getPos1(), selection.getPos2());
            if (!DataCollectionMod.EXIT_REGIONS.add(region)) {
                DataCollectionMod.sendMessage(player, "\u51fa\u53e3\u533a\u57df\u5df2\u5b58\u5728\uff1a" + id);
                NetworkHandler.syncExitRegions(player);
                return;
            }

            if (!save(player)) {
                DataCollectionMod.EXIT_REGIONS.remove(id);
                NetworkHandler.syncExitRegions(player);
                return;
            }

            DataCollectionMod.sendMessage(player, "\u5df2\u521b\u5efa\u51fa\u53e3\u533a\u57df " + id + "\u3002");
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

