package com.roxiun.mellow.feature.stats.tab;

import com.roxiun.mellow.feature.tags.TagPolicy;
import com.roxiun.mellow.api.tags.PlayerTag;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.stats.StatScope;
import com.roxiun.mellow.stats.StatDefinition;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.util.IChatComponent;
import com.roxiun.mellow.mixin.PlayerTabOverlayAccessor;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldSettings;

public class ExtendedStatsTabOverlay extends Gui {

    private static final Ordering<NetworkPlayerInfo> PLAYER_ORDERING =
        Ordering.from(new PlayerComparator());
    private static final int MAX_TAB_PLAYERS = 80;
    private static final int TOP_Y = 10;
    private static final int BORDER = 4;
    private static final int HEADER_HEIGHT = 11;
    private static final int ENTRY_HEIGHT = 9;
    private static final int CELL_PADDING_X = 3;
    private static final int OUTER_PADDING_X = 6;
    private static final int TEAM_MODE_OWN_COLUMN = 0;
    private static final int TEAM_MODE_HIDE_HEADER = 1;
    private static final int TEAM_MODE_COMBINE_NAME = 2;
    private static final int TEAM_MODE_COMBINE_STARS = 3;
    private static final int HEAD_ICON_SIZE = 8;
    private static final int HEAD_TEXT_GAP = 2;
    private static final int TEAM_COLLAPSED_GAP = 1;

    private final Minecraft mc;
    private final MellowOneConfig config;

    private int scrollIndex;
    private int maxVisiblePlayers = 1;
    private boolean combineTeamEnabled;
    private int combineTeamTargetIndex = -1;

    public ExtendedStatsTabOverlay(
        Minecraft mcIn,
        MellowOneConfig config
    ) {
        this.mc = mcIn;
        this.config = config;
    }

    /** One measurement is shared by the vanilla shell and the custom row pass. */
    //? if ornithe {
    public record Layout(StatScope scope, List<NetworkPlayerInfo> players,
                         List<Integer> columns, List<Integer> widths, int tableWidth,
                         int width, int bodyHeight, int totalHeight, int headerHeight, float scale,
                         int visibleCount, int objectiveWidth, ScoreObjective objective) {}
    //?} else {
    /*public static final class Layout {
        private final StatScope scope;
        private final List<NetworkPlayerInfo> players;
        private final List<Integer> columns;
        private final List<Integer> widths;
        private final int tableWidth;
        private final int width;
        private final int bodyHeight;
        private final int totalHeight;
        private final int headerHeight;
        private final float scale;
        private final int visibleCount;
        private final int objectiveWidth;
        private final ScoreObjective objective;
        public Layout(StatScope scope, List<NetworkPlayerInfo> players, List<Integer> columns, List<Integer> widths, int tableWidth, int width, int bodyHeight, int totalHeight, int headerHeight, float scale, int visibleCount, int objectiveWidth, ScoreObjective objective) {
            this.scope = scope;
            this.players = players;
            this.columns = columns;
            this.widths = widths;
            this.tableWidth = tableWidth;
            this.width = width;
            this.bodyHeight = bodyHeight;
            this.totalHeight = totalHeight;
            this.headerHeight = headerHeight;
            this.scale = scale;
            this.visibleCount = visibleCount;
            this.objectiveWidth = objectiveWidth;
            this.objective = objective;
        }
        public StatScope scope() { return scope; }
        public List<NetworkPlayerInfo> players() { return players; }
        public List<Integer> columns() { return columns; }
        public List<Integer> widths() { return widths; }
        public int tableWidth() { return tableWidth; }
        public int width() { return width; }
        public int bodyHeight() { return bodyHeight; }
        public int totalHeight() { return totalHeight; }
        public int headerHeight() { return headerHeight; }
        public float scale() { return scale; }
        public int visibleCount() { return visibleCount; }
        public int objectiveWidth() { return objectiveWidth; }
        public ScoreObjective objective() { return objective; }
    }
    *///?}

    public Layout measure(StatScope scope, int screenWidth, int screenHeight,
                          IChatComponent header,
                          IChatComponent footer,
                          ScoreObjective objective) {
        List<NetworkPlayerInfo> players = collectPlayers(mc.getNetHandler());
        List<Integer> columns = ExtendedTabStatsColumns.getConfiguredColumns(scope, config);
        if (columns.isEmpty()) columns = new ArrayList<>(java.util.Arrays.asList(0, 2));
        resetTeamModeState();
        columns = withAppliedTeamColumnMode(columns);
        List<Integer> widths = computeColumnWidths(columns, players, scope);
        int objectiveWidth = 0;
        if (objective != null) {
            boolean hearts = objective.getRenderType() == IScoreObjectiveCriteria.EnumRenderType.HEARTS;
            boolean healthColumn = columns.stream().anyMatch(c -> ExtendedTabStatsColumns.isHealthColumn(scope, c));
            if (!TabHealthValueResolver.isHealthObjective(objective) || !healthColumn) {
                objectiveWidth = hearts ? 90 : mc.fontRendererObj.getStringWidth(formatHeader(objective.getDisplayName())) + 6;
                if (!hearts) for (NetworkPlayerInfo info : players) {
                    String value = Integer.toString(objective.getScoreboard().getValueFromObjective(
                        info.getGameProfile().getName(), objective).getScorePoints());
                    objectiveWidth = Math.max(objectiveWidth, mc.fontRendererObj.getStringWidth(value) + 6);
                }
            }
        }
        int tableWidth = getTotalWidth(columns, widths) + objectiveWidth;
        int available = Math.max(1, screenWidth - 50);
        float scale = Math.min(1F, (float) available / Math.max(1, tableWidth));
        int width = MathHelper.ceiling_float_int(tableWidth * scale);
        int textHeight = 0;
        for (IChatComponent text : new IChatComponent[]{header, footer}) {
            if (text == null) continue;
            List<String> lines = mc.fontRendererObj.listFormattedStringToWidth(text.getFormattedText(), available);
            for (String line : lines) width = Math.max(width, mc.fontRendererObj.getStringWidth(line));
            textHeight += lines.size() * mc.fontRendererObj.FONT_HEIGHT + 1;
        }
        // Expand the native shell equally on both sides, including wide headers/footers.
        // Content stays centered and retains its existing cell spacing and scale.
        width += OUTER_PADDING_X * 2;
        int headerHeight = config.extendedTabStatsHeaders == 2 ? 0 : HEADER_HEIGHT;
        int availableBody = Math.max(9, screenHeight - TOP_Y - textHeight - BORDER);
        int capacity = Math.max(1, (int) ((availableBody - 8) / scale - headerHeight) / ENTRY_HEIGHT);
        boolean scrolls = players.size() > capacity;
        if (scrolls) capacity = Math.max(1, capacity - 1); // Dedicated scroll status line.
        maxVisiblePlayers = capacity;
        int count = Math.min(players.size(), capacity);
        scrollIndex = MathHelper.clamp_int(scrollIndex, 0, Math.max(0, players.size() - count));
        int pixels = MathHelper.ceiling_float_int((headerHeight + count * ENTRY_HEIGHT + (scrolls ? 9 : 0)) * scale);
        // Vanilla reserves its body in nine-pixel rows. Keep footer positioning native.
        int bodyHeight = ((pixels + 8) / 9) * 9;
        return new Layout(scope, players, columns, widths, tableWidth, width, bodyHeight,
            bodyHeight + textHeight, headerHeight, scale, count, objectiveWidth, objective);
    }

    public void drawBody(Layout layout, int screenWidth, int top, GuiPlayerTabOverlay vanilla) {
        //? if ornithe {
        ArgentumTabBatchCompat.Boundary batch = ArgentumTabBatchCompat.pause(vanilla);
        //?}
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(screenWidth / 2 - layout.tableWidth() * layout.scale() / 2, top, 0);
            GlStateManager.scale(layout.scale(), layout.scale(), 1);
            GlStateManager.color(1, 1, 1, 1);
            drawHeaders(layout.columns(), layout.widths(), layout.scope(), 0, 0);
            int scoreX = layout.tableWidth() - layout.objectiveWidth();
            if (layout.objectiveWidth() > 0 && layout.headerHeight() > 0) {
                String title = layout.objective().getRenderType() == IScoreObjectiveCriteria.EnumRenderType.HEARTS
                    ? "HP" : layout.objective().getDisplayName();
                mc.fontRendererObj.drawStringWithShadow(formatHeader(title), scoreX + 3, 0, -1);
            }
            int end = Math.min(layout.players().size(), scrollIndex + layout.visibleCount());
            int y = layout.headerHeight();
            for (NetworkPlayerInfo info : layout.players().subList(scrollIndex, end)) {
                int background = getRowBackground(info);
                if (background != 0) drawRect(0, y, layout.tableWidth(), y + ENTRY_HEIGHT, background);
                drawValues(layout.columns(), layout.widths(), layout.scope(), info, 0, y);
                if (layout.objectiveWidth() > 0 && info.getGameType() != WorldSettings.GameType.SPECTATOR) {
                    //? if ornithe {
                    ((PlayerTabOverlayAccessor) vanilla).mellow$drawScoreboardValues(
                        layout.objective(), y, info.getGameProfile().getName(), scoreX + 3,
                        layout.tableWidth() - 3, info);
                    //?} else {
                    /*if (layout.objective().getRenderType() == IScoreObjectiveCriteria.EnumRenderType.HEARTS) {
                        ((PlayerTabOverlayAccessor) vanilla).mellow$drawScoreboardValues(
                            layout.objective(), y, info.getGameProfile().getName(), scoreX + 3,
                            layout.tableWidth() - 3, info);
                    } else {
                        // VanillaHUD's native numeric-score redirect assumes vanilla row/ping
                        // coordinates. Draw the same value in our measured score column.
                        String score = "§e" + layout.objective().getScoreboard().getValueFromObjective(
                            info.getGameProfile().getName(), layout.objective()).getScorePoints();
                        mc.fontRendererObj.drawStringWithShadow(score,
                            layout.tableWidth() - 3 - mc.fontRendererObj.getStringWidth(score), y, -1);
                    }
                    *///?}
                }
                y += ENTRY_HEIGHT;
            }
            if (layout.players().size() > layout.visibleCount()) {
                String status = "§7" + (scrollIndex + 1) + "–" + end + " / " + layout.players().size();
                mc.fontRendererObj.drawStringWithShadow(status,
                    (layout.tableWidth() - mc.fontRendererObj.getStringWidth(status)) / 2, y, -1);
            }
        } finally {
            //? if ornithe {
            try {
                batch.flushIcons();
            } finally {
                GlStateManager.popMatrix();
                GlStateManager.color(1, 1, 1, 1);
                batch.resume();
            }
            //?} else {
            /*GlStateManager.popMatrix();
            GlStateManager.color(1, 1, 1, 1);
            *///?}
        }
    }

    public boolean handleMouseWheel(int wheelDelta) {
        if (wheelDelta == 0) {
            return false;
        }

        int effectiveCount = mc.getNetHandler() == null ? 0 : collectPlayers(mc.getNetHandler()).size();
        if (effectiveCount <= maxVisiblePlayers) {
            return false;
        }

        int maxScroll = Math.max(0, effectiveCount - maxVisiblePlayers);
        if (wheelDelta > 0) {
            scrollIndex--;
        } else {
            scrollIndex++;
        }
        scrollIndex = MathHelper.clamp_int(scrollIndex, 0, maxScroll);
        return true;
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

        if (VanillaHudTabIntegration.selfAtTop()) {
            filtered = new ArrayList<>(filtered);
            java.util.UUID self = mc.thePlayer.getUniqueID();
            filtered.sort(java.util.Comparator.comparing(info -> !self.equals(info.getGameProfile().getId())));
        }
        int limit = VanillaHudTabIntegration.playerLimit(MAX_TAB_PLAYERS);
        if (filtered.size() <= limit) {
            return filtered;
        }
        return new ArrayList<>(filtered.subList(0, limit));
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
            String headerLabel = formatHeader(getHeaderLabel(scope, column));
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
            String header = formatHeader(getHeaderLabel(scope, column));
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

            if (column == 2 && shouldShowHeadsInExtendedView()) {
                int headX = x + CELL_PADDING_X;
                int headY = baselineY + (mc.fontRendererObj.FONT_HEIGHT - HEAD_ICON_SIZE) / 2;
                drawPlayerHead(info, headX, headY, HEAD_ICON_SIZE);
                textStartX += HEAD_ICON_SIZE + HEAD_TEXT_GAP;
                reservedLeft += HEAD_ICON_SIZE + HEAD_TEXT_GAP;
            }

            int maxTextWidth = Math.max(1, width - reservedLeft);

            String value = fitToWidth(
                getDisplayValue(info, column, scope, i),
                maxTextWidth
            );
            if (value != null && !value.isEmpty()) {
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

        return mc.fontRendererObj.getStringWidth(
            getDisplayValue(info, column, scope, columnIndex)
        );
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

    private String formatHeader(String label) {
        if (config.extendedTabStatsHeaders == 2 || label == null || label.isEmpty()) return "";
        if (config.extendedTabStatsHeaders == 1) return label.replaceAll("(?i)§l", "") + "§r";
        // Colour and reset codes also clear bold, including in server objective titles.
        return "§l" + label.replaceAll("(?i)(§[0-9a-fr])", "$1§l") + "§r";
    }

    private String getHeaderLabel(StatScope scope, int column) {
        if (column == 0 && shouldHideTeamHeaderInExtendedView()) {
            return "";
        }
        String label = ExtendedTabStatsColumns.getHeaderLabel(scope, column);
        switch (label) {
            case "TEAM": case "STARS": case "NAME": case "LEVEL": case "WINS":
            case "KILLS": case "BEDS": case "FINALS": case "TAGS": case "PING":
                return label.charAt(0) + label.substring(1).toLowerCase(java.util.Locale.ROOT);
            default: return label;
        }
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
        StatDefinition stat = ExtendedTabStatsColumns.definition(scope, column);
        if (stat == null) return 72;
        return stat.maximumWidth() + (stat.style() == StatDefinition.Style.NAME && shouldShowHeadsInExtendedView() ? 10 : 0);
    }

    private boolean shouldShowHeadsInExtendedView() {
        return config != null && config.extendedTabStatsShowHeads && VanillaHudTabIntegration.showHeads();
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
        if (resolvedRealName != null && Mellow.nickUtils != null) {
            stats = Mellow.nickUtils.getResolvedTabStatsForNick(playerName, scope);
        }

        if (ExtendedTabStatsColumns.isTagsColumn(scope, column)) {
            return buildTagsColumnValue(info, stats);
        }
        if (ExtendedTabStatsColumns.isPingColumn(scope, column)) {
            return buildPingColumnValue(info);
        }

        String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
        String team = tabData != null && tabData.length > 0 ? tabData[0] : "";
        String name = tabData != null && tabData.length > 1 ? tabData[1] : playerName;
        String suffix = tabData != null && tabData.length > 2 ? tabData[2] : "";
        String teamColor = PlayerUtils.getTeamColor(team);

        StatDefinition definition = ExtendedTabStatsColumns.definition(scope, column);
        if (definition == null) return "";
        String value = getStatColumnValue(definition, team, name, suffix, teamColor, stats, isNicked, resolvedRealName, info);

        if (column == 2 && shouldKeepTagsInName(scope)) {
            value = appendTagSuffixes(value, stats);
            value = appendListTags(value, info.getGameProfile().getId());
        }

        return value == null ? "" : value;
    }

    private String getStatColumnValue(StatDefinition definition, String team, String name,
        String suffix, String teamColor, TabStats stats, boolean isNicked, String resolvedRealName,
        NetworkPlayerInfo info) {
        String value = definition.value(stats);
        switch (definition.style()) {
            case TEAM:
                return team;
            case NAME:
                if (hasResolvedRealName(resolvedRealName)) return buildDenickedName(teamColor, name, suffix, resolvedRealName);
                return TabNameFormatter.format(info, team, stats == null ? null : stats.getFormattedNameWithRank(),
                    HypixelFeatures.getInstance().getGameSnapshot(), config.showRanksInGameTabStats, isNicked);
            case BEDWARS_STARS:
            case BADGE:
                if (isNicked && (value == null || value.isEmpty())) return getNickLabel();
                if (value == null || value.isEmpty()) return "";
                return definition.style() == StatDefinition.Style.BEDWARS_STARS
                    ? formatStarsForTab(value, config.showStarsWithBrackets) : value + "§r";
            case TITLE:
                if (isNicked && (value == null || value.isEmpty())) return getNickLabel();
                return getExtendedStatValue(stats, value, isNicked);
            case BEDWARS_WINSTREAK:
                return buildBedwarsWinstreakValue(stats, isNicked, info);
            default:
                // Build Battle's title uses the same missing-stat rendering as numeric columns.
                return getExtendedStatValue(stats, value, isNicked);
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

        String safe = value == null ? "" : value;

        for (PlayerTag tag : TagPolicy.visible(stats.getTags(), Mellow.config))
            safe += " " + tag.getIcon();

        return safe;
    }

    private int getRowBackground(NetworkPlayerInfo info) {
        if (
            Mellow.config != null &&
            Mellow.config.highlightTaggedPlayers &&
            isPlayerTagged(info)
        ) {
            return 0x40550000;
        }
        return 0;
    }

    private boolean isPlayerTagged(NetworkPlayerInfo info) {
        if (info == null || info.getGameProfile() == null) {
            return false;
        }

        String playerName = info.getGameProfile().getName();
        if (playerName != null) {
            TabStats stats = Mellow.tabStats.get(playerName);
            if (stats != null && !TagPolicy.visible(stats.getTags(), Mellow.config).isEmpty()) {
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

        if (stats != null) {
            for (PlayerTag tag : TagPolicy.visible(stats.getTags(), Mellow.config)) {
                if (builder.length() > 0) builder.append(" ");
                builder.append(tag.getIcon());
            }
        }

        return builder.toString();
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
            return "§7?";
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

        int auroraWinstreak = Mellow.auroraWinstreakService.getMatchWinstreak(
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
            ExtendedTabStatsColumns.isPingColumn(scope, column)
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
