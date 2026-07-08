package com.sakana.nebulaestats.network;

import com.sakana.nebulaestats.DataCollectionMod;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class NetworkHandler {
    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(DataCollectionMod.MODID);

    private NetworkHandler() {
    }

    public static void init(Side side) {
        int id = 0;
        if (side == Side.CLIENT) {
            CHANNEL.registerMessage(
                    com.sakana.nebulaestats.client.network.ClientOpenExitRegionGuiHandler.class,
                    PacketOpenExitRegionGui.class,
                    id++,
                    Side.CLIENT
            );
            CHANNEL.registerMessage(
                    com.sakana.nebulaestats.client.network.ClientSyncExitRegionsHandler.class,
                    PacketSyncExitRegions.class,
                    id++,
                    Side.CLIENT
            );
            CHANNEL.registerMessage(
                    com.sakana.nebulaestats.client.network.ClientSyncSelectionHandler.class,
                    PacketSyncSelection.class,
                    id++,
                    Side.CLIENT
            );
        } else {
            CHANNEL.registerMessage(NoopOpenExitRegionGuiHandler.class, PacketOpenExitRegionGui.class, id++, Side.CLIENT);
            CHANNEL.registerMessage(NoopSyncExitRegionsHandler.class, PacketSyncExitRegions.class, id++, Side.CLIENT);
            CHANNEL.registerMessage(NoopSyncSelectionHandler.class, PacketSyncSelection.class, id++, Side.CLIENT);
        }

        CHANNEL.registerMessage(PacketCreateExitRegion.Handler.class, PacketCreateExitRegion.class, id++, Side.SERVER);
        CHANNEL.registerMessage(PacketDeleteExitRegion.Handler.class, PacketDeleteExitRegion.class, id++, Side.SERVER);
        CHANNEL.registerMessage(PacketTeleportToExitRegion.Handler.class, PacketTeleportToExitRegion.class, id, Side.SERVER);
    }

    public static void openExitRegionGui(EntityPlayerMP player) {
        CHANNEL.sendTo(PacketOpenExitRegionGui.fromPlayer(player), player);
    }

    public static void syncExitRegions(EntityPlayerMP player) {
        CHANNEL.sendTo(PacketSyncExitRegions.fromCurrentRegions(), player);
    }

    public static void syncSelection(EntityPlayerMP player) {
        CHANNEL.sendTo(PacketSyncSelection.fromPlayer(player), player);
    }

    public static class NoopOpenExitRegionGuiHandler implements IMessageHandler<PacketOpenExitRegionGui, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenExitRegionGui message, MessageContext ctx) {
            return null;
        }
    }

    public static class NoopSyncExitRegionsHandler implements IMessageHandler<PacketSyncExitRegions, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncExitRegions message, MessageContext ctx) {
            return null;
        }
    }

    public static class NoopSyncSelectionHandler implements IMessageHandler<PacketSyncSelection, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncSelection message, MessageContext ctx) {
            return null;
        }
    }
}

