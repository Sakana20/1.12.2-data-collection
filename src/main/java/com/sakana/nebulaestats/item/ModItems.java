package com.sakana.nebulaestats.item;

import com.sakana.nebulaestats.DataCollectionMod;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public final class ModItems {
    public static final Item EXIT_TRIGGER_TOOL = new ItemExitTriggerTool()
            .setRegistryName(DataCollectionMod.MODID, "exit_settings_tool")
            .setUnlocalizedName(DataCollectionMod.MODID + ".exit_settings_tool")
            .setCreativeTab(CreativeTabs.TOOLS);

    private ModItems() {
    }
}

