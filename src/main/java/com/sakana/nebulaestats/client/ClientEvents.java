package com.sakana.nebulaestats.client;

import com.sakana.nebulaestats.DataCollectionMod;
import com.sakana.nebulaestats.client.ExitRegionClientCache.ClientExitRegion;
import com.sakana.nebulaestats.item.ModItems;
import com.sakana.nebulaestats.item.ItemExitTriggerTool;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.List;

@Mod.EventBusSubscriber(modid = DataCollectionMod.MODID, value = Side.CLIENT)
public final class ClientEvents {
    private static final double REGION_RENDER_DISTANCE_SQ = 96.0D * 96.0D;
    private static final double REGION_PICK_DISTANCE = 96.0D;
    private static final double REGION_LABEL_MAX_DISTANCE_SQ = 64.0D * 64.0D;

    private static final KeyBinding TOGGLE_RECORDING = new KeyBinding(
            "key.nebulaestats.toggle_recording",
            Keyboard.KEY_F9,
            "key.categories.nebulaestats"
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
                minecraft.player.sendChatMessage("/ncs toggle");
            }
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || minecraft.world == null || !isHoldingExitTool(player)) {
            return;
        }

        Entity view = minecraft.getRenderViewEntity();
        if (view == null) {
            return;
        }

        double viewX = view.lastTickPosX + (view.posX - view.lastTickPosX) * event.getPartialTicks();
        double viewY = view.lastTickPosY + (view.posY - view.lastTickPosY) * event.getPartialTicks();
        double viewZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * event.getPartialTicks();

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(2.0F);

        List<ClientExitRegion> regions = ExitRegionClientCache.getRegions();
        for (ClientExitRegion region : regions) {
            if (distanceSqToCenter(player, region.getMin()) > REGION_RENDER_DISTANCE_SQ
                    && distanceSqToCenter(player, region.getMax()) > REGION_RENDER_DISTANCE_SQ) {
                continue;
            }

            drawRegionBox(region.getMin(), region.getMax(), viewX, viewY, viewZ, 0.2F, 1.0F, 0.35F, 0.85F);
        }

        BlockPos pos1 = ExitRegionClientCache.getPos1();
        BlockPos pos2 = ExitRegionClientCache.getPos2();
        if (pos1 != null && pos2 != null) {
            drawRegionBox(pos1, pos2, viewX, viewY, viewZ, 0.15F, 0.75F, 1.0F, 1.0F);
        }

        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
        ClientExitRegion pointedRegion = getPointedRegion(player, event.getPartialTicks(), regions);
        if (pointedRegion != null) {
            drawRegionLabel(minecraft, pointedRegion, viewX, viewY, viewZ);
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
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

    private static boolean isHoldingExitTool(EntityPlayerSP player) {
        return ItemExitTriggerTool.isSelectionTool(player.getHeldItemMainhand())
                || ItemExitTriggerTool.isSelectionTool(player.getHeldItemOffhand());
    }

    private static double distanceSqToCenter(EntityPlayerSP player, BlockPos pos) {
        double dx = player.posX - (pos.getX() + 0.5D);
        double dy = player.posY - (pos.getY() + 0.5D);
        double dz = player.posZ - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static void drawRegionBox(
            BlockPos pos1,
            BlockPos pos2,
            double viewX,
            double viewY,
            double viewZ,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        BlockPos min = new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
        AxisAlignedBB box = new AxisAlignedBB(
                min.getX(),
                min.getY(),
                min.getZ(),
                max.getX() + 1.0D,
                max.getY() + 1.0D,
                max.getZ() + 1.0D
        ).offset(-viewX, -viewY, -viewZ);
        RenderGlobal.drawSelectionBoundingBox(box, red, green, blue, alpha);
    }

    private static ClientExitRegion getPointedRegion(
            EntityPlayerSP player,
            float partialTicks,
            List<ClientExitRegion> regions
    ) {
        Vec3d start = player.getPositionEyes(partialTicks);
        Vec3d look = player.getLook(partialTicks);
        Vec3d end = start.addVector(
                look.x * REGION_PICK_DISTANCE,
                look.y * REGION_PICK_DISTANCE,
                look.z * REGION_PICK_DISTANCE
        );

        ClientExitRegion closestRegion = null;
        double closestDistanceSq = REGION_PICK_DISTANCE * REGION_PICK_DISTANCE;
        for (ClientExitRegion region : regions) {
            AxisAlignedBB box = createBox(region.getMin(), region.getMax()).grow(0.1D);
            RayTraceResult result = box.calculateIntercept(start, end);
            if (result == null || result.hitVec == null) {
                continue;
            }

            double distanceSq = start.squareDistanceTo(result.hitVec);
            if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq;
                closestRegion = region;
            }
        }

        return closestRegion;
    }

    private static void drawRegionLabel(
            Minecraft minecraft,
            ClientExitRegion region,
            double viewX,
            double viewY,
            double viewZ
    ) {
        AxisAlignedBB box = createBox(region.getMin(), region.getMax());
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerY = box.maxY + 0.35D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        double dx = centerX - minecraft.player.posX;
        double dy = centerY - minecraft.player.posY;
        double dz = centerZ - minecraft.player.posZ;
        if (dx * dx + dy * dy + dz * dz > REGION_LABEL_MAX_DISTANCE_SQ) {
            return;
        }

        FontRenderer fontRenderer = minecraft.fontRenderer;
        RenderManager renderManager = minecraft.getRenderManager();
        float scale = 0.02666667F;
        int halfWidth = fontRenderer.getStringWidth(region.getId()) / 2;

        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX - viewX, centerY - viewY, centerZ - viewZ);
        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        drawLabelBackground(-halfWidth - 2, -2, halfWidth + 2, fontRenderer.FONT_HEIGHT + 1);
        fontRenderer.drawString(region.getId(), -halfWidth, 0, 0xFFFFFFFF);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
    }

    private static void drawLabelBackground(int left, int top, int right, int bottom) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.disableTexture2D();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(left, bottom, 0.0D).color(0.0F, 0.0F, 0.0F, 0.45F).endVertex();
        buffer.pos(right, bottom, 0.0D).color(0.0F, 0.0F, 0.0F, 0.45F).endVertex();
        buffer.pos(right, top, 0.0D).color(0.0F, 0.0F, 0.0F, 0.45F).endVertex();
        buffer.pos(left, top, 0.0D).color(0.0F, 0.0F, 0.0F, 0.45F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    private static AxisAlignedBB createBox(BlockPos pos1, BlockPos pos2) {
        BlockPos min = new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
        return new AxisAlignedBB(
                min.getX(),
                min.getY(),
                min.getZ(),
                max.getX() + 1.0D,
                max.getY() + 1.0D,
                max.getZ() + 1.0D
        );
    }
}

