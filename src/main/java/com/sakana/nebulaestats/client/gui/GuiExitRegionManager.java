package com.sakana.nebulaestats.client.gui;

import com.sakana.nebulaestats.client.ExitRegionClientCache;
import com.sakana.nebulaestats.client.ExitRegionClientCache.ClientExitRegion;
import com.sakana.nebulaestats.exit.ExitRegionIds;
import com.sakana.nebulaestats.network.NetworkHandler;
import com.sakana.nebulaestats.network.PacketCreateExitRegion;
import com.sakana.nebulaestats.network.PacketDeleteExitRegion;
import com.sakana.nebulaestats.network.PacketTeleportToExitRegion;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiExitRegionManager extends GuiScreen {
    private static final int ADD_BUTTON = 1;
    private static final int TELEPORT_BUTTON_START = 100;
    private static final int DELETE_BUTTON_START = 200;
    private static final int MIN_VISIBLE_ROWS = 3;
    private static final int MAX_VISIBLE_ROWS = 5;
    private static final int ROW_HEIGHT = 30;
    private static final int TELEPORT_BUTTON_WIDTH = 40;
    private static final int DELETE_BUTTON_WIDTH = 40;
    private static final int DELETE_BUTTON_HEIGHT = 20;
    private static final int ROW_BUTTON_GAP = 4;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final float REGION_NAME_SCALE = 1.35F;
    private static final int ROW_CONTENT_Y_BIAS = 1;
    private static final int SELECTION_Y = 58;
    private static final int COMPACT_SELECTION_Y = 54;
    private static final int SELECTION_LINE_GAP = 11;

    private GuiTextField idField;
    private GuiButton addButton;
    private final List<GuiButton> teleportButtons = new ArrayList<GuiButton>();
    private final List<GuiButton> deleteButtons = new ArrayList<GuiButton>();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listTop;
    private int listBottom;
    private int visibleRows;
    private int scrollOffset;
    private boolean draggingScrollbar;

    @Override
    public void initGui() {
        panelWidth = Math.min(Math.max(300, width - 64), 420);
        panelHeight = Math.min(Math.max(214, height - 32), 286);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int selectionY = getSelectionY();
        int selectionBottom = panelY + selectionY + SELECTION_LINE_GAP * 2 + fontRenderer.FONT_HEIGHT;
        listTop = Math.max(panelY + panelHeight - 162, selectionBottom + 26);
        visibleRows = Math.min(MAX_VISIBLE_ROWS, Math.max(MIN_VISIBLE_ROWS, (panelY + panelHeight - 12 - listTop) / ROW_HEIGHT));
        listBottom = listTop + visibleRows * ROW_HEIGHT;

        buttonList.clear();
        teleportButtons.clear();
        deleteButtons.clear();

        idField = new GuiTextField(0, fontRenderer, getInputX(), panelY + 30, getInputWidth(), 18);
        idField.setMaxStringLength(64);
        idField.setFocused(true);

        addButton = new GuiButton(ADD_BUTTON, panelX + panelWidth - 62, panelY + 29, 48, 20, "\u6dfb\u52a0");
        buttonList.add(addButton);
        refreshDeleteButtons();
    }

    @Override
    public void updateScreen() {
        idField.updateCursorCounter();
        addButton.enabled = canAdd();
        clampScroll();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawPanel();
        drawHeader();
        drawSelection();
        drawRegionRows();
        idField.drawTextBox();
        refreshDeleteButtons();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ADD_BUTTON) {
            if (canAdd()) {
                NetworkHandler.CHANNEL.sendToServer(new PacketCreateExitRegion(idField.getText().trim()));
                idField.setText("");
            }
            return;
        }

        if (button.id >= TELEPORT_BUTTON_START && button.id < DELETE_BUTTON_START) {
            int index = button.id - TELEPORT_BUTTON_START;
            List<ClientExitRegion> regions = ExitRegionClientCache.getRegions();
            if (index >= 0 && index < regions.size()) {
                NetworkHandler.CHANNEL.sendToServer(new PacketTeleportToExitRegion(regions.get(index).getId()));
            }
            return;
        }

        if (button.id >= DELETE_BUTTON_START) {
            int index = button.id - DELETE_BUTTON_START;
            List<ClientExitRegion> regions = ExitRegionClientCache.getRegions();
            if (index >= 0 && index < regions.size()) {
                NetworkHandler.CHANNEL.sendToServer(new PacketDeleteExitRegion(regions.get(index).getId()));
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (idField.textboxKeyTyped(typedChar, keyCode)) {
            addButton.enabled = canAdd();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        idField.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && isScrollbarVisible() && isMouseOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            refreshDeleteButtons();
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingScrollbar) {
            updateScrollFromMouse(mouseY);
            refreshDeleteButtons();
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingScrollbar = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scrollOffset += wheel < 0 ? 1 : -1;
            clampScroll();
            refreshDeleteButtons();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void drawPanel() {
        drawRect(panelX - 4, panelY - 4, panelX + panelWidth + 4, panelY + panelHeight + 4, 0xFF000000);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFC6C6C6);
        drawRect(panelX + 4, panelY + 4, panelX + panelWidth - 4, panelY + panelHeight - 4, 0xFF4D4D4D);
        drawRect(panelX + 8, panelY + 24, panelX + panelWidth - 8, panelY + panelHeight - 8, 0xFF333333);
    }

    private void drawHeader() {
        drawCenteredString(fontRenderer, "\u51fa\u53e3\u533a\u57df\u7ba1\u7406", width / 2, panelY + 8, 0xFFFFFFFF);
        fontRenderer.drawString("\u51fa\u53e3\u540d", panelX + 14, panelY + 35, 0xFFFFFFFF);
        drawRect(getInputX() - 2, panelY + 28, getInputX() + getInputWidth() + 2, panelY + 50, 0xFF000000);
    }

    private void drawSelection() {
        BlockPos pos1 = ExitRegionClientCache.getPos1();
        BlockPos pos2 = ExitRegionClientCache.getPos2();
        int y = panelY + getSelectionY();
        fontRenderer.drawString("\u5f53\u524d\u9009\u533a", panelX + 14, y, 0xFFFFFFFF);
        if (pos1 == null || pos2 == null) {
            fontRenderer.drawString("\u8bf7\u5148\u7528\u51fa\u53e3\u89e6\u53d1\u5de5\u5177\u9009\u62e9\u4e24\u4e2a\u70b9",
                    panelX + 14, y + SELECTION_LINE_GAP, 0xFFE0E0E0);
            return;
        }

        BlockPos min = min(pos1, pos2);
        BlockPos max = max(pos1, pos2);
        fontRenderer.drawString("pos1: " + format(pos1), panelX + 14, y + SELECTION_LINE_GAP, 0xFFE0E0E0);
        fontRenderer.drawString("pos2: " + format(pos2), panelX + 14, y + SELECTION_LINE_GAP * 2, 0xFFE0E0E0);
        fontRenderer.drawString(
                "size: " + (max.getX() - min.getX() + 1) + " x "
                        + (max.getY() - min.getY() + 1) + " x "
                        + (max.getZ() - min.getZ() + 1),
                panelX + 170,
                y + SELECTION_LINE_GAP * 2,
                0xFFE0E0E0
        );
    }

    private void drawRegionRows() {
        List<ClientExitRegion> regions = ExitRegionClientCache.getRegions();
        int visibleRows = getVisibleRows();
        fontRenderer.drawString("\u5df2\u5b9a\u4e49\u51fa\u53e3", panelX + 14, listTop - 17, 0xFFFFFFFF);

        if (regions.isEmpty()) {
            fontRenderer.drawString("\u6682\u65e0\u51fa\u53e3\u533a\u57df", panelX + 14, listTop + 12, 0xFFBDBDBD);
            drawScrollbar(0, visibleRows);
            return;
        }

        int rowRight = getListContentRight();
        int deleteLeft = rowRight - DELETE_BUTTON_WIDTH - 8;
        int teleportLeft = deleteLeft - ROW_BUTTON_GAP - TELEPORT_BUTTON_WIDTH;
        int nameX = panelX + 20;
        int coordX = panelX + Math.max(118, panelWidth / 3 + 22);
        int nameWidth = coordX - nameX - 14;
        int coordWidth = teleportLeft - coordX - 10;
        enableListClip();
        for (int row = 0; row < visibleRows; row++) {
            int index = scrollOffset + row;
            if (index >= regions.size()) {
                break;
            }

            ClientExitRegion region = regions.get(index);
            int top = listTop + row * ROW_HEIGHT;
            int rowColor = index % 2 == 0 ? 0xFF454545 : 0xFF3A3A3A;
            drawRect(panelX + 12, top, rowRight, top + ROW_HEIGHT - 3, rowColor);
            int rowContentHeight = ROW_HEIGHT - 3;
            int nameHeight = Math.round(fontRenderer.FONT_HEIGHT * REGION_NAME_SCALE);
            int nameY = top + (rowContentHeight - nameHeight) / 2 + ROW_CONTENT_Y_BIAS;
            int coordBlockHeight = fontRenderer.FONT_HEIGHT * 2 + 3;
            int coordY = top + (rowContentHeight - coordBlockHeight) / 2 + ROW_CONTENT_Y_BIAS;
            drawScaledString(trimToWidth(region.getId(), (int) (nameWidth / REGION_NAME_SCALE)),
                    nameX, nameY, REGION_NAME_SCALE, 0xFFFFFFFF);
            fontRenderer.drawString(trimToWidth("min: " + format(region.getMin()), coordWidth),
                    coordX, coordY, 0xFFCFCFCF);
            fontRenderer.drawString(trimToWidth("max: " + format(region.getMax()), coordWidth),
                    coordX, coordY + fontRenderer.FONT_HEIGHT + 3, 0xFFCFCFCF);
        }
        disableListClip();

        drawScrollbar(regions.size(), visibleRows);

        if (regions.size() > visibleRows) {
            String page = (scrollOffset + 1) + "-" + Math.min(scrollOffset + visibleRows, regions.size())
                    + "/" + regions.size();
            fontRenderer.drawString(page, panelX + panelWidth - 58, panelY + 102, 0xFFCFCFCF);
        }
    }

    private void refreshDeleteButtons() {
        buttonList.removeAll(teleportButtons);
        buttonList.removeAll(deleteButtons);
        teleportButtons.clear();
        deleteButtons.clear();

        List<ClientExitRegion> regions = ExitRegionClientCache.getRegions();
        int visibleRows = getVisibleRows();
        for (int row = 0; row < visibleRows; row++) {
            int index = scrollOffset + row;
            if (index >= regions.size()) {
                break;
            }

            int y = listTop + row * ROW_HEIGHT + (ROW_HEIGHT - 3 - DELETE_BUTTON_HEIGHT) / 2 + ROW_CONTENT_Y_BIAS;
            int deleteX = getListContentRight() - DELETE_BUTTON_WIDTH - 8;
            GuiButton teleportButton = new GuiButton(
                    TELEPORT_BUTTON_START + index,
                    deleteX - ROW_BUTTON_GAP - TELEPORT_BUTTON_WIDTH,
                    y,
                    TELEPORT_BUTTON_WIDTH,
                    DELETE_BUTTON_HEIGHT,
                    "\u4f20\u9001"
            );
            GuiButton button = new GuiButton(
                    DELETE_BUTTON_START + index,
                    deleteX,
                    y,
                    DELETE_BUTTON_WIDTH,
                    DELETE_BUTTON_HEIGHT,
                    "\u5220\u9664"
            );
            teleportButtons.add(teleportButton);
            deleteButtons.add(button);
            buttonList.add(teleportButton);
            buttonList.add(button);
        }
    }

    private boolean canAdd() {
        String id = idField == null ? "" : idField.getText().trim();
        return ExitRegionClientCache.hasCompleteSelection() && ExitRegionIds.isValid(id);
    }

    private int getVisibleRows() {
        return visibleRows;
    }

    private int getSelectionY() {
        return panelHeight < 236 ? COMPACT_SELECTION_Y : SELECTION_Y;
    }

    private int getInputX() {
        return panelX + 72;
    }

    private int getInputWidth() {
        return Math.max(80, panelWidth - 154);
    }

    private int getListContentRight() {
        return panelX + panelWidth - 24;
    }

    private void drawScrollbar(int totalRows, int visibleRows) {
        int trackLeft = panelX + panelWidth - 18;
        int trackTop = listTop;
        int trackBottom = listBottom - 3;
        int trackHeight = Math.max(1, trackBottom - trackTop);
        int maxScroll = Math.max(1, totalRows - visibleRows);
        int thumbHeight = totalRows <= visibleRows ? trackHeight : Math.max(18, trackHeight * visibleRows / totalRows);
        int thumbTop = totalRows <= visibleRows ? trackTop : trackTop + (trackHeight - thumbHeight) * scrollOffset / maxScroll;

        drawRect(trackLeft, trackTop, trackLeft + SCROLLBAR_WIDTH, trackBottom, 0xFF1A1A1A);
        drawRect(trackLeft + 1, thumbTop, trackLeft + SCROLLBAR_WIDTH - 1, thumbTop + thumbHeight, 0xFF9A9A9A);
        drawRect(trackLeft + 1, thumbTop, trackLeft + SCROLLBAR_WIDTH - 2, thumbTop + thumbHeight - 1, 0xFFCFCFCF);
    }

    private boolean isScrollbarVisible() {
        return true;
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY) {
        int trackLeft = panelX + panelWidth - 18;
        return mouseX >= trackLeft && mouseX <= trackLeft + SCROLLBAR_WIDTH
                && mouseY >= listTop && mouseY <= listBottom - 3;
    }

    private void updateScrollFromMouse(int mouseY) {
        int totalRows = ExitRegionClientCache.getRegions().size();
        int visibleRows = getVisibleRows();
        int maxScroll = Math.max(0, totalRows - visibleRows);
        if (maxScroll == 0) {
            scrollOffset = 0;
            return;
        }

        int trackTop = listTop;
        int trackBottom = listBottom - 3;
        int trackHeight = Math.max(1, trackBottom - trackTop);
        int thumbHeight = Math.max(18, trackHeight * visibleRows / totalRows);
        int travel = Math.max(1, trackHeight - thumbHeight);
        int relative = mouseY - trackTop - thumbHeight / 2;
        scrollOffset = relative * maxScroll / travel;
        clampScroll();
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, ExitRegionClientCache.getRegions().size() - getVisibleRows());
        if (scrollOffset < 0) {
            scrollOffset = 0;
        }
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        return fontRenderer.trimStringToWidth(text, Math.max(12, maxWidth - fontRenderer.getStringWidth("..."))) + "...";
    }

    private void enableListClip() {
        ScaledResolution resolution = new ScaledResolution(mc);
        int scale = resolution.getScaleFactor();
        int x = (panelX + 12) * scale;
        int y = (height - listBottom) * scale;
        int clipWidth = (getListContentRight() - panelX - 12) * scale;
        int clipHeight = (listBottom - listTop) * scale;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, clipWidth, clipHeight);
    }

    private void disableListClip() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawScaledString(String text, int x, int y, float scale, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        fontRenderer.drawString(text, (int) (x / scale), (int) (y / scale), color);
        GlStateManager.popMatrix();
    }

    private String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    private BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }
}

