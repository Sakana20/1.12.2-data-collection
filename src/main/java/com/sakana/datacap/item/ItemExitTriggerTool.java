package com.sakana.datacap.item;

import com.sakana.datacap.DataCollectionMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemExitTriggerTool extends Item {
    @Override
    public EnumActionResult onItemUse(
            EntityPlayer player,
            World world,
            BlockPos pos,
            EnumHand hand,
            EnumFacing facing,
            float hitX,
            float hitY,
            float hitZ
    ) {
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMp = (EntityPlayerMP) player;
            DataCollectionMod.SELECTIONS.setPos2(playerMp.getUniqueID(), pos);
            DataCollectionMod.sendMessage(playerMp, "Exit selection pos2 set to " + format(pos) + ".");
        }

        return EnumActionResult.SUCCESS;
    }

    public static boolean isSelectionTool(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModItems.EXIT_TRIGGER_TOOL;
    }

    public static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
