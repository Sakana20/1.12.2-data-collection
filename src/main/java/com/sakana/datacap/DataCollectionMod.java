package com.sakana.datacap;

import com.sakana.datacap.capture.CaptureRecorder;
import com.sakana.datacap.command.CommandDataCap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = DataCollectionMod.MODID, name = DataCollectionMod.NAME, version = DataCollectionMod.VERSION)
@Mod.EventBusSubscriber(modid = DataCollectionMod.MODID)
public class DataCollectionMod {
    public static final String MODID = "datacap";
    public static final String NAME = "Data Capture";
    public static final String VERSION = "1.0.0";

    public static final CaptureRecorder RECORDER = new CaptureRecorder();

    private static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandDataCap(RECORDER));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        if (event.player instanceof EntityPlayerMP) {
            RECORDER.sample((EntityPlayerMP) event.player);
        }
    }

    public static Logger getLogger() {
        return logger;
    }
}
