package com.sakana.datacap.item;

import com.sakana.datacap.DataCollectionMod;
import com.sakana.datacap.network.NetworkHandler;
import com.sakana.datacap.selection.Selection;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
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

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMp = (EntityPlayerMP) player;
            Selection selection = DataCollectionMod.SELECTIONS.getSelection(playerMp.getUniqueID());
            if (selection == null || !selection.isComplete()) {
                DataCollectionMod.sendMessage(playerMp, "Select pos1 and pos2 before opening exit region manager.");
                return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
            }

            NetworkHandler.openExitRegionGui(playerMp);
        }

        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    public static boolean isSelectionTool(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModItems.EXIT_TRIGGER_TOOL;
    }

    public static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
