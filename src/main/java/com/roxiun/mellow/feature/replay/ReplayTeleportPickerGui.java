package com.roxiun.mellow.feature.replay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ReplayTeleportPickerGui extends GuiScreen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 180;
    private static final int ROW_HEIGHT = 12;

    private final ReplayPlaybackSession session;
    private List<Row> rows = Collections.emptyList();
    private int panelX;
    private int panelY;
    private int listX;
    private int listY;
    private int listWidth;
    private int visibleRowCount;
    private int scrollOffset;

    public ReplayTeleportPickerGui(ReplayPlaybackSession session) {
        this.session = session;
    }

    @Override
    public void initGui() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        listX = panelX + 12;
        listY = panelY + 28;
        listWidth = PANEL_WIDTH - 24;
        visibleRowCount = Math.max(1, (PANEL_HEIGHT - 48) / ROW_HEIGHT);
        refreshRows();
    }

    @Override
    public void updateScreen() {
        refreshRows();
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
            "§dReplay Players",
            panelX + (PANEL_WIDTH / 2),
            panelY + 10,
            0xFFFFFF
        );
        drawCenteredString(
            fontRendererObj,
            "§7Click a player to teleport",
            panelX + (PANEL_WIDTH / 2),
            panelY + PANEL_HEIGHT - 14,
            0xFFFFFF
        );

        if (rows.isEmpty()) {
            drawCenteredString(
                fontRendererObj,
                "§7No replay players available.",
                panelX + (PANEL_WIDTH / 2),
                panelY + (PANEL_HEIGHT / 2) - 4,
                0xFFFFFF
            );
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        int endIndex = Math.min(rows.size(), scrollOffset + visibleRowCount);
        for (int i = scrollOffset; i < endIndex; i++) {
            Row row = rows.get(i);
            int rowY = listY + ((i - scrollOffset) * ROW_HEIGHT);
            boolean hovered = isInside(
                mouseX,
                mouseY,
                listX - 2,
                rowY - 1,
                listWidth + 4,
                ROW_HEIGHT
            );

            if (row.targetName != null && hovered) {
                drawRect(
                    listX - 2,
                    rowY - 1,
                    listX + listWidth + 2,
                    rowY + ROW_HEIGHT - 1,
                    0x40FFFFFF
                );
            }

            if (row.header) {
                fontRendererObj.drawStringWithShadow(row.label, listX, rowY + 2, 0xFFFFFF);
            } else {
                fontRendererObj.drawStringWithShadow(
                    row.label,
                    listX + 6,
                    rowY + 2,
                    0xFFFFFF
                );
            }
        }

        if (rows.size() > visibleRowCount) {
            drawCenteredString(
                fontRendererObj,
                "§8Mouse wheel to scroll",
                panelX + (PANEL_WIDTH / 2),
                panelY + PANEL_HEIGHT - 26,
                0xFFFFFF
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    //? if ornithe {
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    //?} else {
    /*protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    *///?}
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }

        String selectedPlayer = getClickedPlayer(mouseX, mouseY);
        if (selectedPlayer == null) {
            return;
        }

        session.teleportToPlayer(selectedPlayer);
        mc.displayGuiScreen(null);
    }

    @Override
    //? if ornithe {
    public void handleMouseInput() {
    //?} else {
    /*public void handleMouseInput() throws IOException {
    *///?}
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || rows.size() <= visibleRowCount) {
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
    //? if ornithe {
    protected void keyTyped(char typedChar, int keyCode) {
    //?} else {
    /*protected void keyTyped(char typedChar, int keyCode) throws IOException {
    *///?}
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void refreshRows() {
        rows = buildRows(session.getTeleportTargets());
        clampScrollOffset();
    }

    private List<Row> buildRows(List<ReplayPlaybackSession.TeleportTarget> targets) {
        if (targets.isEmpty()) {
            return Collections.emptyList();
        }

        List<Row> nextRows = new ArrayList<>();
        String lastTeamSortKey = "";
        for (ReplayPlaybackSession.TeleportTarget target : targets) {
            if (!target.getTeamSortKey().equals(lastTeamSortKey)) {
                nextRows.add(Row.header(target.getTeamLabel()));
                lastTeamSortKey = target.getTeamSortKey();
            }
            nextRows.add(Row.player(target.getDisplayName(), target.getName()));
        }
        return nextRows;
    }

    private String getClickedPlayer(int mouseX, int mouseY) {
        int endIndex = Math.min(rows.size(), scrollOffset + visibleRowCount);
        for (int i = scrollOffset; i < endIndex; i++) {
            Row row = rows.get(i);
            if (row.targetName == null) {
                continue;
            }

            int rowY = listY + ((i - scrollOffset) * ROW_HEIGHT);
            if (isInside(mouseX, mouseY, listX - 2, rowY - 1, listWidth + 4, ROW_HEIGHT)) {
                return row.targetName;
            }
        }
        return null;
    }

    private void clampScrollOffset() {
        int maxOffset = Math.max(0, rows.size() - visibleRowCount);
        if (scrollOffset < 0) {
            scrollOffset = 0;
        } else if (scrollOffset > maxOffset) {
            scrollOffset = maxOffset;
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x &&
        mouseY >= y &&
        mouseX < x + width &&
        mouseY < y + height;
    }

    private static final class Row {

        private final boolean header;
        private final String label;
        private final String targetName;

        private Row(boolean header, String label, String targetName) {
            this.header = header;
            this.label = label;
            this.targetName = targetName;
        }

        private static Row header(String label) {
            return new Row(true, label, null);
        }

        private static Row player(String label, String targetName) {
            return new Row(false, label, targetName);
        }
    }
}
