package com.sakana.nebulaestats.item;

import com.sakana.nebulaestats.DataCollectionMod;
import com.sakana.nebulaestats.network.NetworkHandler;
import com.sakana.nebulaestats.selection.Selection;
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
            DataCollectionMod.sendMessage(playerMp, "\u5df2\u8bbe\u7f6e\u51fa\u53e3\u9009\u533a pos2\uff1a" + format(pos));
            NetworkHandler.syncSelection(playerMp);
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
                DataCollectionMod.sendMessage(playerMp, "\u8bf7\u5148\u9009\u62e9 pos1 \u548c pos2\uff0c\u518d\u6253\u5f00\u51fa\u53e3\u533a\u57df\u7ba1\u7406\u7a97\u53e3\u3002");
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

