package com.roxiun.mellow.feature.stats.tab;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.seraph.SeraphClientType;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import com.roxiun.mellow.util.render.SeraphClientIconRenderer;
import com.roxiun.mellow.util.render.SeraphTagIconRenderer;
import com.roxiun.mellow.util.render.UrchinTagIconRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldSettings;

public class ExtendedStatsTabOverlay extends GuiPlayerTabOverlay {

    private static final Ordering<NetworkPlayerInfo> PLAYER_ORDERING =
        Ordering.from(new PlayerComparator());
    private static final int MAX_TAB_PLAYERS = 80;
    private static final int TOP_Y = 20;
    private static final int BORDER = 4;
    private static final int HEADER_HEIGHT = 11;
    private static final int ENTRY_HEIGHT = 11;
    private static final int ROW_GAP = 1;
    private static final int CELL_PADDING_X = 3;
    private static final int TEAM_MODE_OWN_COLUMN = 0;
    private static final int TEAM_MODE_HIDE_HEADER = 1;
    private static final int TEAM_MODE_COMBINE_NAME = 2;
    private static final int TEAM_MODE_COMBINE_STARS = 3;
    private static final int HEAD_ICON_SIZE = 8;
    private static final int HEAD_TEXT_GAP = 2;
    private static final int TEAM_COLLAPSED_GAP = 1;
    private static final int CLIENT_ICON_SIZE = ENTRY_HEIGHT - 1;
    private static final int TAG_ICON_SIZE = 8;
    private static final int TAG_ICON_GAP = 1;
    private static final int TAG_TEXT_GAP = 2;

    private final Minecraft mc;
    private final MellowOneConfig config;

    private int scrollIndex;
    private int maxVisiblePlayers = 1;
    private boolean combineTeamEnabled;
    private int combineTeamTargetIndex = -1;

    public ExtendedStatsTabOverlay(
        Minecraft mcIn,
        GuiIngame guiIngameIn,
        MellowOneConfig config
    ) {
        super(mcIn, guiIngameIn);
        this.mc = mcIn;
        this.config = config;
    }

    public void renderExtendedPlayerList(StatScope scope) {
        if (
            mc == null ||
            mc.thePlayer == null ||
            mc.getNetHandler() == null ||
            mc.getNetHandler().getPlayerInfoMap() == null
        ) {
            return;
        }

        List<NetworkPlayerInfo> players = collectPlayers(mc.getNetHandler());
        if (players.isEmpty()) {
            return;
        }

        List<Integer> columns = ExtendedTabStatsColumns.getConfiguredColumns(
            scope,
            config
        );
        if (columns.isEmpty()) {
            columns = new ArrayList<>(2);
            columns.add(0); // TEAM
            columns.add(2); // NAME
        }
        resetTeamModeState();
        columns = withAppliedTeamColumnMode(columns);

        List<Integer> columnWidths = computeColumnWidths(columns, players, scope);
        int totalWidth = getTotalWidth(columns, columnWidths);

        ScaledResolution scaled = new ScaledResolution(mc);
        int scaledWidth = scaled.getScaledWidth();
        int scaledHeight = scaled.getScaledHeight();
        int availableWidth = Math.max(100, scaledWidth - BORDER * 2);
        float fitScale = totalWidth > availableWidth
            ? (float) availableWidth / (float) totalWidth
            : 1.0F;

        int scaledPanelWidth = MathHelper.ceiling_float_int(totalWidth * fitScale);
        int startX = Math.max(BORDER, (scaledWidth - scaledPanelWidth) / 2);
        int maxRight = scaledWidth - BORDER;
        if (startX + scaledPanelWidth > maxRight) {
            startX = Math.max(BORDER, maxRight - scaledPanelWidth);
        }

        int scaledHeader = Math.max(
            1,
            MathHelper.ceiling_float_int((HEADER_HEIGHT + ROW_GAP) * fitScale)
        );
        int scaledRowStep = Math.max(
            1,
            MathHelper.ceiling_float_int((ENTRY_HEIGHT + ROW_GAP) * fitScale)
        );
        maxVisiblePlayers = Math.max(
            1,
            (scaledHeight - (TOP_Y + scaledHeader + BORDER * 2)) / scaledRowStep
        );
        int maxScroll = Math.max(0, players.size() - maxVisiblePlayers);
        scrollIndex = MathHelper.clamp_int(scrollIndex, 0, maxScroll);

        int endIndex = Math.min(players.size(), scrollIndex + maxVisiblePlayers);
        List<NetworkPlayerInfo> visible = players.subList(scrollIndex, endIndex);

        int visibleHeight = visible.size() * (ENTRY_HEIGHT + ROW_GAP);
        int panelContentHeight = HEADER_HEIGHT + ROW_GAP + visibleHeight;

        GlStateManager.pushMatrix();
        GlStateManager.translate(startX, TOP_Y, 0.0F);
        GlStateManager.scale(fitScale, fitScale, 1.0F);

        drawRect(-BORDER, -BORDER, totalWidth + BORDER, panelContentHeight + BORDER, Integer.MIN_VALUE);
        drawRect(0, 0, totalWidth, HEADER_HEIGHT, 553648127);

        drawHeaders(columns, columnWidths, scope, 0, (HEADER_HEIGHT - mc.fontRendererObj.FONT_HEIGHT) / 2);

        int rowY = HEADER_HEIGHT + ROW_GAP;
        for (NetworkPlayerInfo info : visible) {
            drawRect(0, rowY, totalWidth, rowY + ENTRY_HEIGHT, getRowBackground(info));
            drawValues(
                columns,
                columnWidths,
                scope,
                info,
                0,
                rowY + (ENTRY_HEIGHT - mc.fontRendererObj.FONT_HEIGHT) / 2
            );
            rowY += ENTRY_HEIGHT + ROW_GAP;
        }

        if (maxScroll > 0) {
            int indicatorX = totalWidth - 8;
            if (scrollIndex > 0) {
                mc.fontRendererObj.drawStringWithShadow("§f▲", indicatorX, HEADER_HEIGHT + 1, -1);
            }
            if (endIndex < players.size()) {
                mc.fontRendererObj.drawStringWithShadow(
                    "§f▼",
                    indicatorX,
                    panelContentHeight - mc.fontRendererObj.FONT_HEIGHT - 1,
                    -1
                );
            }
        }

        GlStateManager.popMatrix();
    }

    public void handleMouseWheel(int wheelDelta, int playerCount) {
        if (wheelDelta == 0) {
            return;
        }

        int effectiveCount = Math.min(playerCount, MAX_TAB_PLAYERS);
        if (effectiveCount <= maxVisiblePlayers) {
            return;
        }

        int maxScroll = Math.max(0, effectiveCount - maxVisiblePlayers);
        if (wheelDelta > 0) {
            scrollIndex--;
        } else {
            scrollIndex++;
        }
        scrollIndex = MathHelper.clamp_int(scrollIndex, 0, maxScroll);
    }

    public void resetScroll() {
        scrollIndex = 0;
        maxVisiblePlayers = 1;
    }

    private List<NetworkPlayerInfo> collectPlayers(NetHandlerPlayClient netHandler) {
        List<NetworkPlayerInfo> sorted = PLAYER_ORDERING.sortedCopy(
            netHandler.getPlayerInfoMap()
        );
        List<NetworkPlayerInfo> filtered = sorted;
        if (shouldFilterObfuscatedPregameEntries()) {
            filtered = new ArrayList<>(sorted.size());
            for (NetworkPlayerInfo info : sorted) {
                if (!isObfuscatedTabEntry(info)) {
                    filtered.add(info);
                }
            }
        }

        if (filtered.size() <= MAX_TAB_PLAYERS) {
            return filtered;
        }
        return new ArrayList<>(filtered.subList(0, MAX_TAB_PLAYERS));
    }

    private boolean shouldFilterObfuscatedPregameEntries() {
        return (
            HypixelFeatures.getInstance().getGameSnapshot() != null &&
            HypixelFeatures.getInstance().getGameSnapshot().isPregame()
        );
    }

    private boolean isObfuscatedTabEntry(NetworkPlayerInfo info) {
        return PlayerUtils.isObfuscatedTabEntry(info);
    }

    private void resetTeamModeState() {
        combineTeamEnabled = false;
        combineTeamTargetIndex = -1;
    }

    private List<Integer> withAppliedTeamColumnMode(List<Integer> baseColumns) {
        List<Integer> result = new ArrayList<>(baseColumns);
        int mode = getTeamColumnMode();
        if (
            mode == TEAM_MODE_OWN_COLUMN || mode == TEAM_MODE_HIDE_HEADER
        ) {
            return result;
        }

        int teamIndex = result.indexOf(0);
        if (teamIndex < 0) {
            return result;
        }

        int preferredTargetColumn = mode == TEAM_MODE_COMBINE_STARS ? 1 : 2;
        int targetIndex = findTeamCombineTargetIndex(
            result,
            teamIndex,
            preferredTargetColumn
        );
        if (targetIndex < 0) {
            return result;
        }

        result.remove(teamIndex);
        if (targetIndex > teamIndex) {
            targetIndex--;
        }

        combineTeamEnabled = true;
        combineTeamTargetIndex = targetIndex;
        return result;
    }

    private int findTeamCombineTargetIndex(
        List<Integer> columns,
        int teamIndex,
        int preferredTargetColumn
    ) {
        int preferredIndex = columns.indexOf(preferredTargetColumn);
        if (preferredIndex >= 0 && preferredIndex != teamIndex) {
            return preferredIndex;
        }

        for (int i = teamIndex + 1; i < columns.size(); i++) {
            if (columns.get(i) != 0) {
                return i;
            }
        }

        for (int i = 0; i < columns.size(); i++) {
            if (i != teamIndex && columns.get(i) != 0) {
                return i;
            }
        }

        return -1;
    }

    private List<Integer> computeColumnWidths(
        List<Integer> columns,
        List<NetworkPlayerInfo> players,
        StatScope scope
    ) {
        List<Integer> widths = new ArrayList<>(columns.size());

        for (int i = 0; i < columns.size(); i++) {
            int column = columns.get(i);
            String headerText = getHeaderLabel(scope, column);
            String headerLabel = headerText.isEmpty()
                ? ""
                : "§l" + headerText + "§r";
            int width = Math.max(
                getMinimumColumnWidth(scope, column),
                headerLabel.isEmpty()
                    ? 0
                    : mc.fontRendererObj.getStringWidth(headerLabel) + CELL_PADDING_X * 2
            );

            for (NetworkPlayerInfo info : players) {
                int contentWidth = getCellContentWidth(info, column, scope, i);
                int extra =
                    column == 2 && shouldShowHeadsInExtendedView()
                        ? HEAD_ICON_SIZE + HEAD_TEXT_GAP
                        : 0;
                width = Math.max(
                    width,
                    contentWidth + CELL_PADDING_X * 2 + extra
                );
            }

            width = Math.min(width, getMaximumColumnWidth(scope, column));
            widths.add(width);
        }

        return widths;
    }

    private int getTotalWidth(List<Integer> columns, List<Integer> columnWidths) {
        int total = 0;
        for (int i = 0; i < columnWidths.size(); i++) {
            if (i > 0) {
                total += getGapAfterColumn(columns, i - 1);
            }
            total += columnWidths.get(i);
        }
        return total;
    }

    private void drawHeaders(
        List<Integer> columns,
        List<Integer> columnWidths,
        StatScope scope,
        int startX,
        int y
    ) {
        int x = startX;
        for (int i = 0; i < columns.size(); i++) {
            int column = columns.get(i);
            String headerText = getHeaderLabel(scope, column);
            String header = headerText.isEmpty() ? "" : "§l" + headerText + "§r";
            int width = columnWidths.get(i);
            if (!header.isEmpty()) {
                int headerWidth = mc.fontRendererObj.getStringWidth(header);
                int drawX;
                if (isCenterAlignedColumn(scope, column)) {
                    drawX = x + (width - headerWidth) / 2;
                } else if (isRightAlignedColumn(scope, column, i)) {
                    drawX = x + width - CELL_PADDING_X - headerWidth;
                } else {
                    drawX =
                        x +
                        CELL_PADDING_X +
                        (column == 2 && shouldShowHeadsInExtendedView()
                                ? HEAD_ICON_SIZE + HEAD_TEXT_GAP
                                : 0);
                }
                mc.fontRendererObj.drawStringWithShadow(header, drawX, y, -1);
            }
            x += columnWidths.get(i);
            if (i < columns.size() - 1) {
                x += getGapAfterColumn(columns, i);
            }
        }
    }

    private void drawValues(
        List<Integer> columns,
        List<Integer> columnWidths,
        StatScope scope,
        NetworkPlayerInfo info,
        int startX,
        int baselineY
    ) {
        int x = startX;
        for (int i = 0; i < columns.size(); i++) {
            int column = columns.get(i);
            int width = columnWidths.get(i);
            int textStartX = x + CELL_PADDING_X;
            int reservedLeft = CELL_PADDING_X * 2;
            TabStats stats = getStatsForInfo(info, scope);

            if (column == 2 && shouldShowHeadsInExtendedView()) {
                int headX = x + CELL_PADDING_X;
                int headY = baselineY + (mc.fontRendererObj.FONT_HEIGHT - HEAD_ICON_SIZE) / 2;
                drawPlayerHead(info, headX, headY, HEAD_ICON_SIZE);
                textStartX += HEAD_ICON_SIZE + HEAD_TEXT_GAP;
                reservedLeft += HEAD_ICON_SIZE + HEAD_TEXT_GAP;
            }

            if (ExtendedTabStatsColumns.isClientColumn(scope, column)) {
                drawClientIcon(info, x, width, baselineY);
                x += width;
                if (i < columns.size() - 1) {
                    x += getGapAfterColumn(columns, i);
                }
                continue;
            }

            if (ExtendedTabStatsColumns.isTagsColumn(scope, column)) {
                int urchinIconWidth = getUrchinTagIconWidth(stats);
                int seraphIconWidth = getSeraphTagIconWidth(stats);
                int totalIconWidth = urchinIconWidth + seraphIconWidth;
                if (urchinIconWidth > 0 && seraphIconWidth > 0) {
                    totalIconWidth += TAG_ICON_GAP;
                }

                String value = fitToWidth(
                    getDisplayValue(info, column, scope, i),
                    Math.max(
                        1,
                        width -
                        reservedLeft -
                        totalIconWidth -
                        (totalIconWidth > 0 && !getDisplayValue(info, column, scope, i).isEmpty()
                            ? TAG_TEXT_GAP
                            : 0)
                    )
                );
                int totalWidth = mc.fontRendererObj.getStringWidth(value);
                if (totalIconWidth > 0) {
                    totalWidth += totalIconWidth;
                    if (!value.isEmpty()) {
                        totalWidth += TAG_TEXT_GAP;
                    }
                }

                int drawX = isCenterAlignedColumn(scope, column)
                    ? x + (width - totalWidth) / 2
                    : textStartX;

                if (!value.isEmpty()) {
                    mc.fontRendererObj.drawStringWithShadow(value, drawX, baselineY, -1);
                }
                if (totalIconWidth > 0) {
                    int iconX = drawX + mc.fontRendererObj.getStringWidth(value);
                    if (!value.isEmpty()) {
                        iconX += TAG_TEXT_GAP;
                    }
                    int centerY = baselineY + (mc.fontRendererObj.FONT_HEIGHT - TAG_ICON_SIZE) / 2;

                    if (urchinIconWidth > 0) {
                        drawUrchinTagIcons(stats, iconX, centerY);
                        iconX += urchinIconWidth;
                    }
                    if (seraphIconWidth > 0) {
                        if (urchinIconWidth > 0) {
                            iconX += TAG_ICON_GAP;
                        }
                        drawSeraphTagIcons(stats, iconX, centerY);
                    }
                }

                x += width;
                if (i < columns.size() - 1) {
                    x += getGapAfterColumn(columns, i);
                }
                continue;
            }

            int maxTextWidth = Math.max(1, width - reservedLeft);
            if (column == 2 && shouldKeepTagsInName(scope)) {
                int urchinIconWidth = getUrchinTagIconWidth(stats);
                int seraphIconWidth = getSeraphTagIconWidth(stats);
                int totalIconWidth = urchinIconWidth + seraphIconWidth;
                if (urchinIconWidth > 0 && seraphIconWidth > 0) {
                    totalIconWidth += TAG_ICON_GAP;
                }
                maxTextWidth = Math.max(
                    1,
                    maxTextWidth - totalIconWidth - (totalIconWidth > 0 ? TAG_TEXT_GAP : 0)
                );
            }

            String value = fitToWidth(
                getDisplayValue(info, column, scope, i),
                maxTextWidth
            );
            if (!value.isEmpty()) {
                int drawX;
                if (isCenterAlignedColumn(scope, column)) {
                    drawX = x + (width - mc.fontRendererObj.getStringWidth(value)) / 2;
                } else if (isRightAlignedColumn(scope, column, i)) {
                    drawX =
                        x +
                        width -
                        CELL_PADDING_X -
                        mc.fontRendererObj.getStringWidth(value);
                } else {
                    drawX = textStartX;
                }
                mc.fontRendererObj.drawStringWithShadow(value, drawX, baselineY, -1);

                if (column == 2 && shouldKeepTagsInName(scope)) {
                    int urchinIconWidth = getUrchinTagIconWidth(stats);
                    int seraphIconWidth = getSeraphTagIconWidth(stats);
                    int totalIconWidth = urchinIconWidth + seraphIconWidth;
                    if (urchinIconWidth > 0 && seraphIconWidth > 0) {
                        totalIconWidth += TAG_ICON_GAP;
                    }
                    if (totalIconWidth > 0) {
                        int iconX = drawX + mc.fontRendererObj.getStringWidth(value);
                        if (!value.isEmpty()) {
                            iconX += TAG_TEXT_GAP;
                        }
                        int centerY = baselineY + (mc.fontRendererObj.FONT_HEIGHT - TAG_ICON_SIZE) / 2;

                        if (urchinIconWidth > 0) {
                            drawUrchinTagIcons(stats, iconX, centerY);
                            iconX += urchinIconWidth;
                        }
                        if (seraphIconWidth > 0) {
                            if (urchinIconWidth > 0) {
                                iconX += TAG_ICON_GAP;
                            }
                            drawSeraphTagIcons(stats, iconX, centerY);
                        }
                    }
                }
            }
            x += width;
            if (i < columns.size() - 1) {
                x += getGapAfterColumn(columns, i);
            }
        }
    }

    private int getCellContentWidth(
        NetworkPlayerInfo info,
        int column,
        StatScope scope,
        int columnIndex
    ) {
        if (ExtendedTabStatsColumns.isClientColumn(scope, column)) {
            return getCachedClientType(info) == null ? 0 : CLIENT_ICON_SIZE;
        }

        TabStats stats = getStatsForInfo(info, scope);
        String value = getDisplayValue(info, column, scope, columnIndex);
        int width = mc.fontRendererObj.getStringWidth(value);
        int urchinIconWidth = getUrchinTagIconWidth(stats);
        int seraphIconWidth = getSeraphTagIconWidth(stats);
        int totalIconWidth = urchinIconWidth + seraphIconWidth;
        if (urchinIconWidth > 0 && seraphIconWidth > 0) {
            totalIconWidth += TAG_ICON_GAP;
        }

        if (ExtendedTabStatsColumns.isTagsColumn(scope, column)) {
            return width + totalIconWidth + (totalIconWidth > 0 && !value.isEmpty() ? TAG_TEXT_GAP : 0);
        }

        if (column == 2 && shouldKeepTagsInName(scope)) {
            return width + totalIconWidth + (totalIconWidth > 0 && !value.isEmpty() ? TAG_TEXT_GAP : 0);
        }

        return width;
    }

    private String fitToWidth(String value, int width) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        if (mc.fontRendererObj.getStringWidth(value) <= width) {
            return value;
        }

        String suffix = "§7...";
        int suffixWidth = mc.fontRendererObj.getStringWidth(suffix);
        int trimmedWidth = Math.max(0, width - suffixWidth);
        String trimmed = mc.fontRendererObj.trimStringToWidth(value, trimmedWidth);
        if (trimmed == null || trimmed.isEmpty()) {
            return "";
        }
        return trimmed + suffix;
    }

    private void drawPlayerHead(
        NetworkPlayerInfo playerInfo,
        int x,
        int y,
        int size
    ) {
        if (playerInfo == null || playerInfo.getGameProfile() == null) {
            return;
        }

        if (playerInfo.getLocationSkin() == null) {
            return;
        }

        GameProfile gameProfile = playerInfo.getGameProfile();
        EntityPlayer entityPlayer = mc.theWorld == null
            ? null
            : mc.theWorld.getPlayerEntityByUUID(gameProfile.getId());
        boolean upsideDown =
            entityPlayer != null &&
            entityPlayer.isWearing(EnumPlayerModelParts.CAPE) &&
            ("Dinnerbone".equals(gameProfile.getName()) ||
                "Grumm".equals(gameProfile.getName()));

        int vBase = 8 + (upsideDown ? 8 : 0);
        int vSize = 8 * (upsideDown ? -1 : 1);

        mc.getTextureManager().bindTexture(playerInfo.getLocationSkin());
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Gui.drawScaledCustomSizeModalRect(
            x,
            y,
            8.0F,
            (float) vBase,
            8,
            vSize,
            size,
            size,
            64.0F,
            64.0F
        );

        if (entityPlayer != null && entityPlayer.isWearing(EnumPlayerModelParts.HAT)) {
            Gui.drawScaledCustomSizeModalRect(
                x,
                y,
                40.0F,
                (float) vBase,
                8,
                vSize,
                size,
                size,
                64.0F,
                64.0F
            );
        }
    }

    private boolean isRightAlignedColumn(
        StatScope scope,
        int column,
        int columnIndex
    ) {
        if (isTeamCombinedTargetColumn(columnIndex)) {
            return false;
        }
        if (ExtendedTabStatsColumns.isTagsColumn(scope, column)) {
            return false;
        }
        return column != 0 && column != 2;
    }

    private boolean isTeamCombinedTargetColumn(int columnIndex) {
        return (
            combineTeamEnabled &&
            combineTeamTargetIndex >= 0 &&
            columnIndex == combineTeamTargetIndex
        );
    }

    private String getDisplayValue(
        NetworkPlayerInfo info,
        int column,
        StatScope scope,
        int columnIndex
    ) {
        String value = getColumnValue(info, column, scope);
        if (!isTeamCombinedTargetColumn(columnIndex)) {
            return value;
        }

        String team = getColumnValue(info, 0, scope);
        if (team == null || team.trim().isEmpty()) {
            return value == null ? "" : value;
        }
        if (value == null || value.isEmpty()) {
            return shouldStripCombinedTeamPadding()
                ? trimVisibleTrailingWhitespace(team)
                : team;
        }
        return joinCombinedTeamValue(team, value);
    }

    private String joinCombinedTeamValue(String team, String value) {
        if (!shouldStripCombinedTeamPadding()) {
            return team + value;
        }

        String trimmedTeam = trimVisibleTrailingWhitespace(team);
        String trimmedValue = trimVisibleLeadingWhitespace(value);

        if (trimmedTeam.isEmpty()) {
            return trimmedValue;
        }
        if (trimmedValue.isEmpty()) {
            return trimmedTeam;
        }
        return trimmedTeam + " " + trimmedValue;
    }

    private boolean shouldStripCombinedTeamPadding() {
        return config.extendedTabStatsStripCombinedTeamPadding;
    }

    private String trimVisibleLeadingWhitespace(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder formattingPrefix = new StringBuilder();
        int index = 0;
        while (index < value.length()) {
            char current = value.charAt(index);
            if (current == '\u00A7' && index + 1 < value.length()) {
                formattingPrefix.append(current).append(value.charAt(index + 1));
                index += 2;
                continue;
            }
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            break;
        }

        return formattingPrefix.append(value.substring(index)).toString();
    }

    private String trimVisibleTrailingWhitespace(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        int lastVisibleEnd = -1;
        int index = 0;
        while (index < value.length()) {
            char current = value.charAt(index);
            if (current == '\u00A7' && index + 1 < value.length()) {
                index += 2;
                continue;
            }
            if (!Character.isWhitespace(current)) {
                lastVisibleEnd = index + 1;
            }
            index++;
        }

        if (lastVisibleEnd < 0) {
            return "";
        }

        StringBuilder suffixFormatting = new StringBuilder();
        index = lastVisibleEnd;
        while (index < value.length()) {
            char current = value.charAt(index);
            if (current == '\u00A7' && index + 1 < value.length()) {
                suffixFormatting.append(current).append(value.charAt(index + 1));
                index += 2;
                continue;
            }
            index++;
        }

        return value.substring(0, lastVisibleEnd) + suffixFormatting;
    }

    private String getHeaderLabel(StatScope scope, int column) {
        if (column == 0 && shouldHideTeamHeaderInExtendedView()) {
            return "";
        }
        return ExtendedTabStatsColumns.getHeaderLabel(scope, column);
    }

    private int getMinimumColumnWidth(StatScope scope, int column) {
        if (ExtendedTabStatsColumns.isHealthColumn(scope, column)) {
            return 18;
        }
        if (ExtendedTabStatsColumns.isTagsColumn(scope, column)) {
            return 20;
        }

        switch (column) {
            case 0: // TEAM
                return shouldHideTeamHeaderInExtendedView() ? 10 : 18;
            case 1: // STARS / LEVEL
                return 22;
            case 2: // NAME
                return shouldShowHeadsInExtendedView() ? 70 : 58;
            default: // numeric stat columns
                return 18;
        }
    }

    private int getMaximumColumnWidth(StatScope scope, int column) {
        if (ExtendedTabStatsColumns.isHealthColumn(scope, column)) {
            return 34;
        }
        if (ExtendedTabStatsColumns.isTagsColumn(scope, column)) {
            return 110;
        }

        if (scope == StatScope.SKYWARS) {
            switch (column) {
                case 0:
                    return 40;
                case 1:
                    return 70;
                case 2:
                    return shouldShowHeadsInExtendedView() ? 230 : 220;
                default:
                    return 72;
            }
        }

        if (scope == StatScope.DUELS) {
            switch (column) {
                case 0:
                    return 40;
                case 1:
                    return 92;
                case 2:
                    return shouldShowHeadsInExtendedView() ? 230 : 220;
                default:
                    return 72;
            }
        }

        if (scope == StatScope.BUILD_BATTLE) {
            switch (column) {
                case 0:
                    return 40;
                case 1:
                    return 136;
                case 2:
                    return shouldShowHeadsInExtendedView() ? 230 : 220;
                default:
                    return 72;
            }
        }

        if (scope == StatScope.TNT_RUN) {
            switch (column) {
                case 0:
                    return 40;
                case 1:
                    return 72;
                case 2:
                    return shouldShowHeadsInExtendedView() ? 230 : 220;
                default:
                    return 72;
            }
        }

        switch (column) {
            case 0:
                return 40;
            case 1:
                return 70;
            case 2:
                return shouldShowHeadsInExtendedView() ? 230 : 220;
            default:
                return 72;
        }
    }

    private boolean shouldShowHeadsInExtendedView() {
        return config != null && config.extendedTabStatsShowHeads;
    }

    private boolean shouldHideTeamHeaderInExtendedView() {
        return getTeamColumnMode() == TEAM_MODE_HIDE_HEADER;
    }

    private int getTeamColumnMode() {
        if (config == null) {
            return TEAM_MODE_OWN_COLUMN;
        }
        switch (config.extendedTabStatsTeamColumnMode) {
            case 0:
                return TEAM_MODE_COMBINE_STARS;
            case 1:
                return TEAM_MODE_OWN_COLUMN;
            case 2:
                return TEAM_MODE_HIDE_HEADER;
            case 3:
                return TEAM_MODE_COMBINE_NAME;
            default:
                return TEAM_MODE_OWN_COLUMN;
        }
    }

    private int getGapAfterColumn(List<Integer> columns, int index) {
        if (
            shouldHideTeamHeaderInExtendedView() &&
            index >= 0 &&
            index < columns.size() &&
            columns.get(index) == 0
        ) {
            return TEAM_COLLAPSED_GAP;
        }
        return ExtendedTabStatsColumns.COLUMN_GAP;
    }

    private String getColumnValue(
        NetworkPlayerInfo info,
        int column,
        StatScope scope
    ) {
        if (info == null || info.getGameProfile() == null) {
            return "";
        }

        String playerName = info.getGameProfile().getName();
        if (playerName == null || playerName.isEmpty()) {
            return "";
        }

        if (ExtendedTabStatsColumns.isHealthColumn(scope, column)) {
            return TabHealthValueResolver.getFormattedHealth(mc, info);
        }

        TabStats stats = Mellow.tabStats.get(playerName);
        String resolvedRealName = Mellow.nickUtils == null
            ? null
            : Mellow.nickUtils.getResolvedRealNameForNick(playerName);
        boolean isNicked =
            Mellow.nickUtils != null && Mellow.nickUtils.isNicked(playerName);
        if (stats == null && isNicked && Mellow.nickUtils != null) {
            stats = Mellow.nickUtils.getResolvedTabStatsForNick(playerName, scope);
        }

        if (ExtendedTabStatsColumns.isTagsColumn(scope, column)) {
            return buildTagsColumnValue(info, stats);
        }
        if (ExtendedTabStatsColumns.isPingColumn(scope, column)) {
            return buildPingColumnValue(info);
        }
        if (ExtendedTabStatsColumns.isClientColumn(scope, column)) {
            return buildClientColumnValue(playerName);
        }

        String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
        String team = tabData != null && tabData.length > 0 ? tabData[0] : "";
        String name = tabData != null && tabData.length > 1 ? tabData[1] : playerName;
        String suffix = tabData != null && tabData.length > 2 ? tabData[2] : "";
        String teamColor = team.length() >= 2 ? team.substring(0, 2) : "§f";

        if (column == 4 && scope == StatScope.BEDWARS) {
            return buildBedwarsWinstreakValue(stats, isNicked, info);
        }

        String value;
        if (scope == StatScope.SKYWARS) {
            value = getSkywarsColumnValue(
                column,
                team,
                name,
                suffix,
                teamColor,
                stats,
                isNicked,
                resolvedRealName
            );
        } else if (scope == StatScope.DUELS) {
            value = getDuelsColumnValue(
                column,
                team,
                name,
                suffix,
                teamColor,
                stats,
                isNicked,
                resolvedRealName
            );
        } else if (scope == StatScope.BUILD_BATTLE) {
            value = getBuildBattleColumnValue(
                column,
                team,
                name,
                suffix,
                teamColor,
                stats,
                isNicked,
                resolvedRealName
            );
        } else if (scope == StatScope.TNT_RUN) {
            value = getTntRunColumnValue(
                column,
                team,
                name,
                suffix,
                teamColor,
                stats,
                isNicked,
                resolvedRealName
            );
        } else {
            value = getBedwarsColumnValue(
                column,
                team,
                name,
                suffix,
                teamColor,
                stats,
                isNicked,
                resolvedRealName
            );
        }

        if (column == 2 && shouldKeepTagsInName(scope)) {
            value = appendTagSuffixes(value, stats);
            value = appendListTags(value, info.getGameProfile().getId());
        }

        return value == null ? "" : value;
    }

    private String getBedwarsColumnValue(
        int column,
        String team,
        String name,
        String suffix,
        String teamColor,
        TabStats stats,
        boolean isNicked,
        String resolvedRealName
    ) {
        switch (column) {
            case 0:
                return team;
            case 1:
                if (isNicked && (stats == null || stats.getStars() == null || stats.getStars().isEmpty())) {
                    return getNickLabel();
                }
                if (stats != null && stats.getStars() != null && !stats.getStars().isEmpty()) {
                    return formatStarsForTab(stats.getStars(), config.showStarsWithBrackets);
                }
                return "";
            case 2:
                if (hasResolvedRealName(resolvedRealName)) {
                    return buildDenickedName(
                        teamColor,
                        name,
                        suffix,
                        resolvedRealName
                    );
                }
                if (shouldShowRankInTabName()) {
                    if (
                        stats != null &&
                        stats.getFormattedNameWithRank() != null &&
                        !stats.getFormattedNameWithRank().isEmpty()
                    ) {
                        return stats.getFormattedNameWithRank() + "§r";
                    }
                }
                return "§r" + teamColor + name + suffix;
            case 3:
                return getExtendedStatValue(stats, stats != null ? stats.getFkdr() : null, isNicked);
            case 4:
                return getExtendedStatValue(
                    stats,
                    stats != null ? stats.getWinstreak() : null,
                    isNicked
                );
            case 5:
                return getExtendedStatValue(stats, stats != null ? stats.getWlr() : null, isNicked);
            case 6:
                return getExtendedStatValue(stats, stats != null ? stats.getBblr() : null, isNicked);
            case 7:
                return getExtendedStatValue(stats, stats != null ? stats.getWins() : null, isNicked);
            case 8:
                return getExtendedStatValue(stats, stats != null ? stats.getBeds() : null, isNicked);
            case 9:
                return getExtendedStatValue(stats, stats != null ? stats.getFinals() : null, isNicked);
            default:
                return "";
        }
    }

    private String getSkywarsColumnValue(
        int column,
        String team,
        String name,
        String suffix,
        String teamColor,
        TabStats stats,
        boolean isNicked,
        String resolvedRealName
    ) {
        switch (column) {
            case 0:
                return team;
            case 1:
                if (isNicked && (stats == null || stats.getStars() == null || stats.getStars().isEmpty())) {
                    return getNickLabel();
                }
                if (stats != null && stats.getStars() != null && !stats.getStars().isEmpty()) {
                    return stats.getStars() + "§r";
                }
                return "";
            case 2:
                if (hasResolvedRealName(resolvedRealName)) {
                    return buildDenickedName(
                        teamColor,
                        name,
                        suffix,
                        resolvedRealName
                    );
                }
                if (shouldShowRankInTabName()) {
                    if (
                        stats != null &&
                        stats.getFormattedNameWithRank() != null &&
                        !stats.getFormattedNameWithRank().isEmpty()
                    ) {
                        return stats.getFormattedNameWithRank() + "§r";
                    }
                }
                return "§r" + teamColor + name + suffix;
            case 3:
                return getExtendedStatValue(stats, stats != null ? stats.getFkdr() : null, isNicked);
            case 4:
                return getExtendedStatValue(stats, stats != null ? stats.getWlr() : null, isNicked);
            case 5:
                return getExtendedStatValue(stats, stats != null ? stats.getWins() : null, isNicked);
            case 6:
                return getExtendedStatValue(stats, stats != null ? stats.getKills() : null, isNicked);
            default:
                return "";
        }
    }

    private String getDuelsColumnValue(
        int column,
        String team,
        String name,
        String suffix,
        String teamColor,
        TabStats stats,
        boolean isNicked,
        String resolvedRealName
    ) {
        switch (column) {
            case 0:
                return team;
            case 1:
                if (isNicked && (stats == null || stats.getStars() == null || stats.getStars().isEmpty())) {
                    return getNickLabel();
                }
                if (stats != null && stats.getStars() != null && !stats.getStars().isEmpty()) {
                    return stats.getStars() + "§r";
                }
                return "";
            case 2:
                if (hasResolvedRealName(resolvedRealName)) {
                    return buildDenickedName(
                        teamColor,
                        name,
                        suffix,
                        resolvedRealName
                    );
                }
                if (shouldShowRankInTabName()) {
                    if (
                        stats != null &&
                        stats.getFormattedNameWithRank() != null &&
                        !stats.getFormattedNameWithRank().isEmpty()
                    ) {
                        return stats.getFormattedNameWithRank() + "§r";
                    }
                }
                return "§r" + teamColor + name + suffix;
            case 3:
                return getExtendedStatValue(stats, stats != null ? stats.getFkdr() : null, isNicked);
            case 4:
                return getExtendedStatValue(stats, stats != null ? stats.getWlr() : null, isNicked);
            case 5:
                return getExtendedStatValue(stats, stats != null ? stats.getWins() : null, isNicked);
            case 6:
                return getExtendedStatValue(stats, stats != null ? stats.getLosses() : null, isNicked);
            case 7:
                return getExtendedStatValue(stats, stats != null ? stats.getKills() : null, isNicked);
            case 8:
                return getExtendedStatValue(stats, stats != null ? stats.getDeaths() : null, isNicked);
            case 9:
                return getExtendedStatValue(
                    stats,
                    stats != null ? stats.getWinstreak() : null,
                    isNicked
                );
            default:
                return "";
        }
    }

    private String getBuildBattleColumnValue(
        int column,
        String team,
        String name,
        String suffix,
        String teamColor,
        TabStats stats,
        boolean isNicked,
        String resolvedRealName
    ) {
        switch (column) {
            case 0:
                return team;
            case 1:
                if (isNicked && (stats == null || stats.getStars() == null || stats.getStars().isEmpty())) {
                    return getNickLabel();
                }
                return getExtendedStatValue(stats, stats != null ? stats.getStars() : null, isNicked);
            case 2:
                if (hasResolvedRealName(resolvedRealName)) {
                    return buildDenickedName(
                        teamColor,
                        name,
                        suffix,
                        resolvedRealName
                    );
                }
                if (shouldShowRankInTabName()) {
                    if (
                        stats != null &&
                        stats.getFormattedNameWithRank() != null &&
                        !stats.getFormattedNameWithRank().isEmpty()
                    ) {
                        return stats.getFormattedNameWithRank() + "§r";
                    }
                }
                return "§r" + teamColor + name + suffix;
            case 3:
                return getExtendedStatValue(stats, stats != null ? stats.getWins() : null, isNicked);
            default:
                return "";
        }
    }

    private String getTntRunColumnValue(
        int column,
        String team,
        String name,
        String suffix,
        String teamColor,
        TabStats stats,
        boolean isNicked,
        String resolvedRealName
    ) {
        switch (column) {
            case 0:
                return team;
            case 1:
                return getExtendedStatValue(stats, stats != null ? stats.getWins() : null, isNicked);
            case 2:
                if (hasResolvedRealName(resolvedRealName)) {
                    return buildDenickedName(
                        teamColor,
                        name,
                        suffix,
                        resolvedRealName
                    );
                }
                if (shouldShowRankInTabName()) {
                    if (
                        stats != null &&
                        stats.getFormattedNameWithRank() != null &&
                        !stats.getFormattedNameWithRank().isEmpty()
                    ) {
                        return stats.getFormattedNameWithRank() + "§r";
                    }
                }
                return "§r" + teamColor + name + suffix;
            case 3:
                return getExtendedStatValue(stats, stats != null ? stats.getWlr() : null, isNicked);
            default:
                return "";
        }
    }

    private String appendListTags(String value, UUID playerUUID) {
        String safe = value == null ? "" : value;
        if (playerUUID == null) {
            return safe;
        }

        if (
            Mellow.blacklistManager != null &&
            Mellow.blacklistManager.isBlacklisted(playerUUID)
        ) {
            safe += " §8[§4LIST§8]";
        }
        if (
            Mellow.annoylistManager != null &&
            Mellow.annoylistManager.isAnnoylisted(playerUUID)
        ) {
            safe += " §8[§3ANNOY§8]";
        }

        return safe;
    }

    private String appendTagSuffixes(String value, TabStats stats) {
        if (stats == null) {
            return value;
        }

        StringBuilder safe = new StringBuilder(value == null ? "" : value);

        if (Mellow.config.showSeraphTagsInTab && stats.isSeraphTagged()) {
            for (SeraphTag tag : stats.getSeraphTags()) {
                safe.append(' ').append(FormattingUtils.formatSeraphTagIcon(tag));
            }
        }

        return safe.toString();
    }

    private int getRowBackground(NetworkPlayerInfo info) {
        if (
            Mellow.config != null &&
            Mellow.config.highlightTaggedPlayers &&
            isPlayerTagged(info)
        ) {
            return 0x99550000;
        }
        return 553648127;
    }

    private boolean isPlayerTagged(NetworkPlayerInfo info) {
        if (info == null || info.getGameProfile() == null) {
            return false;
        }

        String playerName = info.getGameProfile().getName();
        if (playerName != null) {
            TabStats stats = Mellow.tabStats.get(playerName);
            if (stats != null && (stats.isUrchinTagged() || stats.isSeraphTagged())) {
                return true;
            }
        }

        UUID playerUuid = info.getGameProfile().getId();
        if (playerUuid == null) {
            return false;
        }
        if (
            Mellow.blacklistManager != null &&
            Mellow.blacklistManager.isBlacklisted(playerUuid)
        ) {
            return true;
        }
        return (
            Mellow.annoylistManager != null &&
            Mellow.annoylistManager.isAnnoylisted(playerUuid)
        );
    }

    private String buildTagsColumnValue(NetworkPlayerInfo info, TabStats stats) {
        StringBuilder builder = new StringBuilder();
        UUID playerUuid =
            info == null || info.getGameProfile() == null
                ? null
                : info.getGameProfile().getId();

        if (
            playerUuid != null &&
            Mellow.blacklistManager != null &&
            Mellow.blacklistManager.isBlacklisted(playerUuid)
        ) {
            builder.append("§8[§4BL§8]§r");
        }
        if (
            playerUuid != null &&
            Mellow.annoylistManager != null &&
            Mellow.annoylistManager.isAnnoylisted(playerUuid)
        ) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append("§8[§3AL§8]§r");
        }

        if (stats != null && Mellow.config != null) {
            // Seraph tags are rendered as icons in drawValues(), not as text badges
        }

        return builder.toString();
    }

    private TabStats getStatsForInfo(NetworkPlayerInfo info, StatScope scope) {
        if (info == null || info.getGameProfile() == null) {
            return null;
        }

        String playerName = info.getGameProfile().getName();
        if (playerName == null || playerName.isEmpty()) {
            return null;
        }

        TabStats stats = Mellow.tabStats.get(playerName);
        boolean isNicked =
            Mellow.nickUtils != null && Mellow.nickUtils.isNicked(playerName);
        if (stats == null && isNicked && Mellow.nickUtils != null) {
            stats = Mellow.nickUtils.getResolvedTabStatsForNick(playerName, scope);
        }
        return stats;
    }

    private int getUrchinTagIconWidth(TabStats stats) {
        if (
            stats == null ||
            Mellow.config == null ||
            !Mellow.config.showUrchinTagsInTab ||
            !stats.isUrchinTagged()
        ) {
            return 0;
        }

        return UrchinTagIconRenderer.measureTags(
            stats.getUrchinTags(),
            TAG_ICON_SIZE,
            TAG_ICON_GAP
        );
    }

    private void drawUrchinTagIcons(TabStats stats, int x, int y) {
        if (
            stats == null ||
            Mellow.config == null ||
            !Mellow.config.showUrchinTagsInTab ||
            !stats.isUrchinTagged()
        ) {
            return;
        }

        UrchinTagIconRenderer.drawTags(
            stats.getUrchinTags(),
            x,
            y,
            TAG_ICON_SIZE,
            TAG_ICON_GAP
        );
    }

    private int getSeraphTagIconWidth(TabStats stats) {
        if (
            stats == null ||
            Mellow.config == null ||
            !Mellow.config.showSeraphTagsInTab ||
            !stats.isSeraphTagged()
        ) {
            return 0;
        }

        return SeraphTagIconRenderer.measureTags(
            stats.getSeraphTags(),
            TAG_ICON_SIZE,
            TAG_ICON_GAP
        );
    }

    private void drawSeraphTagIcons(TabStats stats, int x, int y) {
        if (
            stats == null ||
            Mellow.config == null ||
            !Mellow.config.showSeraphTagsInTab ||
            !stats.isSeraphTagged()
        ) {
            return;
        }

        SeraphTagIconRenderer.drawTags(
            stats.getSeraphTags(),
            x,
            y,
            TAG_ICON_SIZE,
            TAG_ICON_GAP
        );
    }

    private String buildPingColumnValue(NetworkPlayerInfo info) {
        if (
            Mellow.config == null ||
            info == null ||
            info.getGameProfile() == null ||
            info.getGameProfile().getId() == null
        ) {
            return "";
        }

        int ping = info.getResponseTime();
        if (ping <= 1 || ping >= 999) {
            return "§70";
        }
        if (ping < 50) {
            return "§a" + ping;
        }
        if (ping < 100) {
            return "§e" + ping;
        }
        if (ping < 200) {
            return "§6" + ping;
        }
        return "§c" + ping;
    }

    private String buildClientColumnValue(String playerName) {
        SeraphClientType clientType = getCachedClientType(playerName);
        return clientType == null ? "" : clientType.getDisplayName();
    }

    private String buildBedwarsWinstreakValue(
        TabStats stats,
        boolean isNicked,
        NetworkPlayerInfo info
    ) {
        String visible = getExtendedStatValue(
            stats,
            stats != null ? stats.getWinstreak() : null,
            isNicked
        );
        if (!shouldUseHiddenWinstreakFallback(visible, info)) {
            return visible;
        }

        int auroraWinstreak = Mellow.auroraWinstreakService.getCachedWinstreak(
            info.getGameProfile().getId().toString().replace("-", "")
        );
        if (auroraWinstreak < 0) {
            return visible;
        }
        return FormattingUtils.formatBedwarsWinstreakWithColor(auroraWinstreak);
    }

    private boolean shouldUseHiddenWinstreakFallback(
        String visible,
        NetworkPlayerInfo info
    ) {
        return (
            Mellow.config != null &&
            Mellow.config.showHiddenWinstreaks &&
            Mellow.auroraWinstreakService != null &&
            info != null &&
            info.getGameProfile() != null &&
            info.getGameProfile().getId() != null &&
            isHiddenOrEmptyWinstreak(visible)
        );
    }

    private boolean isHiddenOrEmptyWinstreak(String value) {
        return FormattingUtils.isHiddenOrEmptyWinstreakDisplay(value);
    }

    private SeraphClientType getCachedClientType(String playerName) {
        if (
            Mellow.config == null ||
            !Mellow.config.seraph ||
            playerName == null ||
            Mellow.seraphClientCacheService == null
        ) {
            return null;
        }

        return Mellow.seraphClientCacheService.getCachedClient(playerName);
    }

    private SeraphClientType getCachedClientType(NetworkPlayerInfo info) {
        if (info == null || info.getGameProfile() == null) {
            return null;
        }

        String playerName = info.getGameProfile().getName();
        SeraphClientType clientType = getCachedClientType(playerName);
        if (clientType != null) {
            return clientType;
        }

        UUID playerUuid = getTrustedPlayerUuid(info);
        if (
            playerUuid != null &&
            Mellow.config != null &&
            Mellow.config.seraph &&
            Mellow.seraphClientCacheService != null
        ) {
            Mellow.seraphClientCacheService.refreshClientAsync(
                playerName,
                playerUuid.toString().replace("-", "")
            );
        }

        return null;
    }

    private UUID getTrustedPlayerUuid(NetworkPlayerInfo info) {
        if (
            info == null ||
            info.getGameProfile() == null ||
            info.getGameProfile().getId() == null ||
            PlayerUtils.isObfuscatedTabEntry(info)
        ) {
            return null;
        }

        UUID playerUuid = info.getGameProfile().getId();
        return playerUuid.version() == 4 ? playerUuid : null;
    }

    private void drawClientIcon(
        NetworkPlayerInfo info,
        int columnX,
        int columnWidth,
        int baselineY
    ) {
        SeraphClientType clientType = getCachedClientType(info);
        if (clientType == null) {
            return;
        }

        int iconX = columnX + (columnWidth - CLIENT_ICON_SIZE) / 2;
        int rowY = baselineY - (ENTRY_HEIGHT - mc.fontRendererObj.FONT_HEIGHT) / 2;
        int iconY = rowY + (ENTRY_HEIGHT - CLIENT_ICON_SIZE) / 2;
        SeraphClientIconRenderer.drawIcon(clientType, iconX, iconY, CLIENT_ICON_SIZE);
    }

    private boolean shouldKeepTagsInName(StatScope scope) {
        if (Mellow.config == null) {
            return true;
        }
        return !ExtendedTabStatsColumns
            .getConfiguredColumns(scope, Mellow.config)
            .contains(ExtendedTabStatsColumns.getTagsColumnIndex(scope));
    }

    private boolean isCenterAlignedColumn(StatScope scope, int column) {
        return (
            ExtendedTabStatsColumns.isTagsColumn(scope, column) ||
            ExtendedTabStatsColumns.isPingColumn(scope, column) ||
            ExtendedTabStatsColumns.isClientColumn(scope, column)
        );
    }

    private String getNickLabel() {
        if (Mellow.config.showNickWithBrackets) {
            return "§5[§lNICK§r§5]§r";
        }
        return "§5§lNICK§r";
    }

    private boolean hasResolvedRealName(String resolvedRealName) {
        return resolvedRealName != null && !resolvedRealName.trim().isEmpty();
    }

    private String buildDenickedName(
        String teamColor,
        String nickName,
        String suffix,
        String resolvedRealName
    ) {
        return (
            "§r" +
            teamColor +
            nickName +
            suffix +
            " §7(" +
            resolvedRealName +
            "§7)"
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String getExtendedStatValue(TabStats stats, String value, boolean isNicked) {
        String safeValue = safe(value);
        if (!safeValue.isEmpty()) {
            return safeValue;
        }

        // Show explicit placeholders only for unresolved nicked players in extended tab columns.
        if (isNicked && stats == null) {
            return "-";
        }

        return "";
    }

    private boolean shouldShowRankInTabName() {
        if (HypixelFeatures.getInstance().getGameSnapshot() == null) {
            return false;
        }
        if (HypixelFeatures.getInstance().getGameSnapshot().isLobby()) {
            return true;
        }
        return Mellow.config.showRanksInGameTabStats;
    }

    private String formatStarsForTab(String stars, boolean withBrackets) {
        if (stars == null || stars.isEmpty()) {
            return "";
        }

        String result;
        if (withBrackets) {
            result = hasOuterBrackets(stars) ? stars : "§7[" + stars + "§7]";
        } else {
            result = stripOuterBrackets(stars);
        }
        return result + "§r";
    }

    private String stripOuterBrackets(String value) {
        if (!hasOuterBrackets(value)) {
            return value;
        }

        int open = value.indexOf('[');
        int close = value.lastIndexOf(']');
        if (open >= 0 && close > open) {
            return (
                value.substring(0, open) +
                value.substring(open + 1, close) +
                value.substring(close + 1)
            );
        }
        return value;
    }

    private boolean hasOuterBrackets(String value) {
        String plain = value.replaceAll("§.", "");
        return plain.startsWith("[") && plain.endsWith("]");
    }

    private static class PlayerComparator implements java.util.Comparator<NetworkPlayerInfo> {

        @Override
        public int compare(NetworkPlayerInfo first, NetworkPlayerInfo second) {
            ScorePlayerTeam firstTeam = first.getPlayerTeam();
            ScorePlayerTeam secondTeam = second.getPlayerTeam();

            return ComparisonChain
                .start()
                .compareTrueFirst(
                    first.getGameType() != WorldSettings.GameType.SPECTATOR,
                    second.getGameType() != WorldSettings.GameType.SPECTATOR
                )
                .compare(
                    firstTeam != null ? firstTeam.getRegisteredName() : "",
                    secondTeam != null ? secondTeam.getRegisteredName() : ""
                )
                .compare(first.getGameProfile().getName(), second.getGameProfile().getName())
                .result();
        }
    }
}
