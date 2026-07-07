package com.sakana.datacap.client;

import com.sakana.datacap.DataCollectionMod;
import com.sakana.datacap.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(modid = DataCollectionMod.MODID, value = Side.CLIENT)
public final class ClientEvents {
    private static final KeyBinding TOGGLE_RECORDING = new KeyBinding(
            "key.datacap.toggle_recording",
            Keyboard.KEY_F9,
            "key.categories.datacap"
    );

    private static boolean keyBindingsRegistered;

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerKeyBindings();
        registerItemModel(ModItems.EXIT_TRIGGER_TOOL);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (TOGGLE_RECORDING.isPressed()) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.player != null) {
                minecraft.player.sendChatMessage("/datacap toggle");
            }
        }
    }

    private static void registerItemModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(
                item,
                0,
                new ModelResourceLocation(item.getRegistryName(), "inventory")
        );
    }

    private static void registerKeyBindings() {
        if (keyBindingsRegistered) {
            return;
        }

        ClientRegistry.registerKeyBinding(TOGGLE_RECORDING);
        keyBindingsRegistered = true;
    }
}
