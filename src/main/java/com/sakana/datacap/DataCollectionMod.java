package com.sakana.datacap;

import com.sakana.datacap.capture.CaptureRecorder;
import com.sakana.datacap.command.CommandDataCap;
import com.sakana.datacap.exit.ExitRegionManager;
import com.sakana.datacap.item.ItemExitTriggerTool;
import com.sakana.datacap.item.ModItems;
import com.sakana.datacap.selection.SelectionManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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

    public static final ExitRegionManager EXIT_REGIONS = new ExitRegionManager();
    public static final SelectionManager SELECTIONS = new SelectionManager();
    public static final CaptureRecorder RECORDER = new CaptureRecorder(EXIT_REGIONS);

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

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(ModItems.EXIT_TRIGGER_TOOL);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().isRemote || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }

        if (!ItemExitTriggerTool.isSelectionTool(event.getItemStack())) {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        SELECTIONS.setPos1(player.getUniqueID(), event.getPos());
        sendMessage(player, "Exit selection pos1 set to " + ItemExitTriggerTool.format(event.getPos()) + ".");
        event.setCanceled(true);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void sendMessage(EntityPlayerMP player, String message) {
        player.sendMessage(new TextComponentString(message));
    }
}
