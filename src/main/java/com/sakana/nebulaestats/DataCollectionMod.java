package com.sakana.nebulaestats;

import com.sakana.nebulaestats.capture.CaptureRecorder;
import com.sakana.nebulaestats.command.CommandDataCap;
import com.sakana.nebulaestats.exit.ExitRegionManager;
import com.sakana.nebulaestats.exit.ExitRegionStorage;
import com.sakana.nebulaestats.item.ItemExitTriggerTool;
import com.sakana.nebulaestats.item.ModItems;
import com.sakana.nebulaestats.network.NetworkHandler;
import com.sakana.nebulaestats.selection.SelectionManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = DataCollectionMod.MODID, name = DataCollectionMod.NAME, version = DataCollectionMod.VERSION)
@Mod.EventBusSubscriber(modid = DataCollectionMod.MODID)
public class DataCollectionMod {
    public static final String MODID = "nebulaestats";
    public static final String NAME = "Nebulaecraft Statistics";
    public static final String VERSION = "1.0.0";

    public static final ExitRegionManager EXIT_REGIONS = new ExitRegionManager();
    public static final ExitRegionStorage EXIT_REGION_STORAGE = new ExitRegionStorage(EXIT_REGIONS);
    public static final SelectionManager SELECTIONS = new SelectionManager();
    public static final CaptureRecorder RECORDER = new CaptureRecorder(EXIT_REGIONS);

    private static Logger logger;
    private static MinecraftServer currentServer;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        NetworkHandler.init(event.getSide());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        currentServer = event.getServer();

        try {
            EXIT_REGION_STORAGE.load(currentServer);
            logger.info("Loaded {} exit regions from {}", EXIT_REGIONS.getAll().size(),
                    EXIT_REGION_STORAGE.getStorageFile(currentServer).getPath());
        } catch (Exception exception) {
            logger.error("Failed to load exit regions", exception);
        }

        event.registerServerCommand(new CommandDataCap(RECORDER));
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        if (currentServer == null) {
            logger.warn("Skipping exit region save because the server reference is missing.");
            return;
        }

        try {
            EXIT_REGION_STORAGE.save(currentServer);
            logger.info("Saved {} exit regions to {}", EXIT_REGIONS.getAll().size(),
                    EXIT_REGION_STORAGE.getStorageFile(currentServer).getPath());
        } catch (Exception exception) {
            logger.error("Failed to save exit regions", exception);
        } finally {
            currentServer = null;
        }
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
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            NetworkHandler.syncExitRegions(player);
            NetworkHandler.syncSelection(player);
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
        sendMessage(player, "\u5df2\u8bbe\u7f6e\u51fa\u53e3\u9009\u533a pos1\uff1a" + ItemExitTriggerTool.format(event.getPos()));
        NetworkHandler.syncSelection(player);
        event.setCanceled(true);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void sendMessage(EntityPlayerMP player, String message) {
        player.sendMessage(new TextComponentString(message));
    }
}

