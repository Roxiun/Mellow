package com.roxiun.mellow.feature.replay;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ReplayBrowserGui extends GuiScreen {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 232;
    private static final int LIST_WIDTH = 166;
    private static final int ROW_HEIGHT = 34;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
        "yyyy-MM-dd HH:mm",
        Locale.ENGLISH
    );

    private final ReplayManager replayManager;

    private List<ReplayCatalogEntry> entries = Collections.emptyList();
    private int panelX;
    private int panelY;
    private int listX;
    private int listY;
    private int listHeight;
    private int visibleRowCount;
    private int scrollOffset;
    private int selectedIndex = -1;
    private String pendingDeleteReplayId;
    private String statusMessage;
    private long statusExpiresAt;
    private long lastSelectionClickAt;

    private GuiButton openButton;
    private GuiButton deleteButton;
    private GuiButton refreshButton;

    public ReplayBrowserGui(ReplayManager replayManager) {
        this.replayManager = replayManager;
    }

    @Override
    public void initGui() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        listX = panelX + 12;
        listY = panelY + 28;
        listHeight = PANEL_HEIGHT - 64;
        visibleRowCount = Math.max(1, listHeight / ROW_HEIGHT);

        buttonList.clear();
        openButton = new GuiButton(0, panelX + PANEL_WIDTH - 176, panelY + PANEL_HEIGHT - 26, 48, 20, "Open");
        deleteButton = new GuiButton(1, panelX + PANEL_WIDTH - 124, panelY + PANEL_HEIGHT - 26, 56, 20, "Delete");
        refreshButton = new GuiButton(2, panelX + PANEL_WIDTH - 64, panelY + PANEL_HEIGHT - 26, 52, 20, "Refresh");
        buttonList.add(openButton);
        buttonList.add(deleteButton);
        buttonList.add(refreshButton);

        refreshEntries(null);
    }

    @Override
    public void updateScreen() {
        if (statusMessage != null && System.currentTimeMillis() >= statusExpiresAt) {
            statusMessage = null;
            clearDeleteConfirmation();
        }
        updateButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE0101010);
        drawRect(
            panelX + 1,
            panelY + 1,
            panelX + PANEL_WIDTH - 1,
            panelY + PANEL_HEIGHT - 1,
            0xE0252530
        );

        drawCenteredString(
            fontRendererObj,
            "§dMellow Replays & Clips",
            panelX + (PANEL_WIDTH / 2),
            panelY + 10,
            0xFFFFFF
        );

        drawSectionChrome();
        drawReplayList(mouseX, mouseY);
        drawReplayDetails();

        if (statusMessage != null) {
            fontRendererObj.drawStringWithShadow(statusMessage, panelX + 12, panelY + PANEL_HEIGHT - 20, 0xFFFFFF);
        } else if (!entries.isEmpty()) {
            fontRendererObj.drawStringWithShadow(
                "§8Enter to open, Delete twice to remove",
                panelX + 12,
                panelY + PANEL_HEIGHT - 20,
                0xFFFFFF
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null || !button.enabled) {
            return;
        }

        if (button.id == 0) {
            openSelectedReplay();
            return;
        }
        if (button.id == 1) {
            deleteSelectedReplay();
            return;
        }
        if (button.id == 2) {
            refreshEntries(getSelectedReplayId());
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0 || entries.isEmpty()) {
            return;
        }

        int clickedIndex = getClickedIndex(mouseX, mouseY);
        if (clickedIndex < 0) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean doubleClick = clickedIndex == selectedIndex && (now - lastSelectionClickAt) <= 250L;
        selectedIndex = clickedIndex;
        lastSelectionClickAt = now;
        clearDeleteConfirmation();
        updateButtons();

        if (doubleClick) {
            openSelectedReplay();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || entries.size() <= visibleRowCount) {
            return;
        }

        if (wheel > 0) {
            scrollOffset--;
        } else {
            scrollOffset++;
        }
        clampScrollOffset();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            openSelectedReplay();
            return;
        }
        if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
            deleteSelectedReplay();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void drawSectionChrome() {
        drawRect(listX, listY - 12, listX + LIST_WIDTH, listY + listHeight, 0x28000000);
        drawRect(
            listX + LIST_WIDTH + 10,
            listY - 12,
            panelX + PANEL_WIDTH - 12,
            listY + listHeight,
            0x28000000
        );
        fontRendererObj.drawStringWithShadow("§fSaved Replays & Clips", listX, listY - 10, 0xFFFFFF);
        fontRendererObj.drawStringWithShadow("§fReplay Details", listX + LIST_WIDTH + 16, listY - 10, 0xFFFFFF);
    }

    private void drawReplayList(int mouseX, int mouseY) {
        if (entries.isEmpty()) {
            drawCenteredString(
                fontRendererObj,
                "§7No saved replays or clips yet.",
                listX + (LIST_WIDTH / 2),
                listY + (listHeight / 2) - 4,
                0xFFFFFF
            );
            return;
        }

        int endIndex = Math.min(entries.size(), scrollOffset + visibleRowCount);
        for (int i = scrollOffset; i < endIndex; i++) {
            ReplayCatalogEntry entry = entries.get(i);
            ReplayMetadata metadata = entry.getMetadata();
            int rowY = listY + ((i - scrollOffset) * ROW_HEIGHT);
            boolean selected = i == selectedIndex;
            boolean hovered = isInside(mouseX, mouseY, listX, rowY, LIST_WIDTH, ROW_HEIGHT - 2);

            int backgroundColor = selected
                ? 0x50D94FD2
                : hovered
                    ? 0x32FFFFFF
                    : 0x18000000;
            drawRect(listX, rowY, listX + LIST_WIDTH, rowY + ROW_HEIGHT - 2, backgroundColor);

            fontRendererObj.drawStringWithShadow(
                trim(
                    fontRendererObj,
                    (metadata.isClip() ? "§d[Clip] §f" : "") + safe(metadata.getReplayId()),
                    LIST_WIDTH - 10
                ),
                listX + 6,
                rowY + 5,
                0xFFFFFF
            );
            fontRendererObj.drawStringWithShadow(
                "§7" + trim(fontRendererObj, primaryLocation(metadata), LIST_WIDTH - 16),
                listX + 6,
                rowY + 16,
                0xFFFFFF
            );
            fontRendererObj.drawStringWithShadow(
                "§8" + formatDuration(metadata.getVisibleDurationMs()) + "  " + trim(fontRendererObj, safe(metadata.getMode()), LIST_WIDTH - 48),
                listX + 6,
                rowY + 26,
                0xFFFFFF
            );
        }
    }

    private void drawReplayDetails() {
        int detailX = listX + LIST_WIDTH + 16;
        int detailY = listY + 4;
        int lineHeight = 13;

        ReplayCatalogEntry selected = getSelectedEntry();
        if (selected == null) {
            fontRendererObj.drawStringWithShadow(
                "§7Select a replay or clip to inspect it.",
                detailX,
                detailY,
                0xFFFFFF
            );
            return;
        }

        ReplayMetadata metadata = selected.getMetadata();
        drawDetailLine(detailX, detailY, "Type", metadata.isClip() ? "Clip" : "Replay");
        drawDetailLine(detailX, detailY + (lineHeight * 1), "Id", safe(metadata.getReplayId()));
        drawDetailLine(detailX, detailY + (lineHeight * 2), "Map", safe(metadata.getMap()));
        drawDetailLine(detailX, detailY + (lineHeight * 3), "Mode", safe(metadata.getMode()));
        drawDetailLine(detailX, detailY + (lineHeight * 4), "Viewer", safe(metadata.getViewerName()));
        drawDetailLine(detailX, detailY + (lineHeight * 5), "Server", safe(metadata.getServerName()));
        drawDetailLine(detailX, detailY + (lineHeight * 6), metadata.isClip() ? "Saved" : "Started", formatDate(metadata.getCatalogTimestamp()));
        drawDetailLine(detailX, detailY + (lineHeight * 7), "Duration", formatDuration(metadata.getVisibleDurationMs()));
        drawDetailLine(detailX, detailY + (lineHeight * 8), "Packets", String.valueOf(metadata.getPacketCount()));

        fontRendererObj.drawSplitString(
            "§7Double-click an entry or press §fOpen§7 to start playback.",
            detailX,
            detailY + (lineHeight * 10),
            PANEL_WIDTH - LIST_WIDTH - 34,
            0xFFFFFF
        );
    }

    private void drawDetailLine(int x, int y, String label, String value) {
        fontRendererObj.drawStringWithShadow("§8" + label, x, y, 0xFFFFFF);
        fontRendererObj.drawStringWithShadow(
            "§f" + trim(fontRendererObj, value, PANEL_WIDTH - LIST_WIDTH - 86),
            x + 52,
            y,
            0xFFFFFF
        );
    }

    private String primaryLocation(ReplayMetadata metadata) {
        String map = metadata == null ? "" : metadata.getMap();
        if (map != null && !map.trim().isEmpty()) {
            return map;
        }
        return safe(metadata == null ? "" : metadata.getServerName());
    }

    private void refreshEntries(String selectedReplayId) {
        entries = replayManager.listReplays();
        if (entries.isEmpty()) {
            selectedIndex = -1;
            scrollOffset = 0;
            clearDeleteConfirmation();
            updateButtons();
            return;
        }

        selectedIndex = findIndexByReplayId(selectedReplayId);
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        clampScrollOffset();
        clearDeleteConfirmation();
        updateButtons();
    }

    private void openSelectedReplay() {
        ReplayCatalogEntry selected = getSelectedEntry();
        if (selected == null) {
            return;
        }

        if (replayManager.openReplay(selected.getMetadata().getReplayId())) {
            mc.displayGuiScreen(null);
        } else {
            setStatus("§cFailed to open replay.");
        }
    }

    private void deleteSelectedReplay() {
        ReplayCatalogEntry selected = getSelectedEntry();
        if (selected == null) {
            return;
        }

        String replayId = safe(selected.getMetadata().getReplayId());
        if (!replayId.equals(pendingDeleteReplayId)) {
            pendingDeleteReplayId = replayId;
            deleteButton.displayString = "Confirm";
            setStatus("§cClick delete again to remove §f" + replayId + "§c.");
            return;
        }

        if (replayManager.deleteReplay(replayId)) {
            setStatus("§7Deleted replay §f" + replayId + "§7.");
            refreshEntries(null);
            return;
        }

        clearDeleteConfirmation();
        setStatus("§cReplay no longer exists.");
    }

    private void clearDeleteConfirmation() {
        pendingDeleteReplayId = null;
        if (deleteButton != null) {
            deleteButton.displayString = "Delete";
        }
    }

    private void updateButtons() {
        boolean hasSelection = getSelectedEntry() != null;
        if (openButton != null) {
            openButton.enabled = hasSelection;
        }
        if (deleteButton != null) {
            deleteButton.enabled = hasSelection;
        }
        if (refreshButton != null) {
            refreshButton.enabled = true;
        }
    }

    private ReplayCatalogEntry getSelectedEntry() {
        return selectedIndex >= 0 && selectedIndex < entries.size()
            ? entries.get(selectedIndex)
            : null;
    }

    private String getSelectedReplayId() {
        ReplayCatalogEntry selected = getSelectedEntry();
        return selected == null ? null : safe(selected.getMetadata().getReplayId());
    }

    private int getClickedIndex(int mouseX, int mouseY) {
        int endIndex = Math.min(entries.size(), scrollOffset + visibleRowCount);
        for (int i = scrollOffset; i < endIndex; i++) {
            int rowY = listY + ((i - scrollOffset) * ROW_HEIGHT);
            if (isInside(mouseX, mouseY, listX, rowY, LIST_WIDTH, ROW_HEIGHT - 2)) {
                return i;
            }
        }
        return -1;
    }

    private int findIndexByReplayId(String replayId) {
        if (replayId == null || replayId.trim().isEmpty()) {
            return -1;
        }
        for (int i = 0; i < entries.size(); i++) {
            if (replayId.equalsIgnoreCase(safe(entries.get(i).getMetadata().getReplayId()))) {
                return i;
            }
        }
        return -1;
    }

    private void clampScrollOffset() {
        int maxOffset = Math.max(0, entries.size() - visibleRowCount);
        if (selectedIndex >= 0 && selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= 0 && selectedIndex >= scrollOffset + visibleRowCount) {
            scrollOffset = selectedIndex - visibleRowCount + 1;
        }
        if (scrollOffset < 0) {
            scrollOffset = 0;
        } else if (scrollOffset > maxOffset) {
            scrollOffset = maxOffset;
        }
    }

    private void setStatus(String message) {
        statusMessage = message;
        statusExpiresAt = System.currentTimeMillis() + 2500L;
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0L) {
            return "Unknown";
        }
        return DATE_FORMAT.format(new Date(timestamp));
    }

    private String formatDuration(int durationMs) {
        int totalSeconds = Math.max(0, durationMs / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes >= 60) {
            int hours = minutes / 60;
            minutes %= 60;
            return String.format(Locale.ENGLISH, "%dh %02dm %02ds", hours, minutes, seconds);
        }
        return String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown" : value;
    }

    private String trim(net.minecraft.client.gui.FontRenderer fontRenderer, String text, int width) {
        return fontRenderer.trimStringToWidth(text, Math.max(0, width));
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x &&
        mouseY >= y &&
        mouseX < x + width &&
        mouseY < y + height;
    }
}
