package com.sakana.nebulaestats.client.network;

import com.sakana.nebulaestats.client.ExitRegionClientCache;
import com.sakana.nebulaestats.network.PacketSyncSelection;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ClientSyncSelectionHandler implements IMessageHandler<PacketSyncSelection, IMessage> {
    @Override
    public IMessage onMessage(final PacketSyncSelection message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ExitRegionClientCache.setSelection(message.getPos1(), message.getPos2());
            }
        });
        return null;
    }
}

