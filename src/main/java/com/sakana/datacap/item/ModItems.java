package com.sakana.datacap.item;

import com.sakana.datacap.DataCollectionMod;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public final class ModItems {
    public static final Item EXIT_TRIGGER_TOOL = new ItemExitTriggerTool()
            .setRegistryName(DataCollectionMod.MODID, "exit_trigger_tool")
            .setUnlocalizedName(DataCollectionMod.MODID + ".exit_trigger_tool")
            .setCreativeTab(CreativeTabs.TOOLS);

    private ModItems() {
    }
}
