package com.roxiun.mellow.feature.profileviewer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.provider.StatsProvider;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.feature.profileviewer.model.PvComputedStats;
import com.roxiun.mellow.feature.profileviewer.model.PvExperience;
import com.roxiun.mellow.feature.profileviewer.model.PvMode;
import com.roxiun.mellow.feature.profileviewer.model.PvSourceData;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.formatting.BedwarsStarFormatter;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class PVGui extends GuiScreen {

    private static final int WIDTH = 430;
    private static final int HEIGHT = 224;
    private static final long SEARCH_COOLDOWN_MS = 5000L;
    private static final int SOCIAL_ICON_SIZE = 15;
    private static final int SOCIAL_GAP = 0;
    private static final int TOP_CARD_TEXT_WIDTH = 170;
    private static final int PROVIDER_LABEL_WIDTH = 120;
    private static final int CORNER_CARD_WIDTH = 97;
    private static final int STATS_TITLE_WIDTH = 220;
    private static final int CATEGORY_BUTTON_TEXT_WIDTH = 136;
    private static final int MODE_BUTTON_TEXT_WIDTH = 136;
    private static final int LEFT_STATS_COLUMN_WIDTH = 92;
    private static final int MIDDLE_STATS_COLUMN_WIDTH = 92;
    private static final int RIGHT_STATS_COLUMN_WIDTH = 88;
    private static final int STATUS_TEXT_WIDTH = WIDTH - 20;
    private static final int SOCIAL_TOOLTIP_WIDTH = 180;
    private static final int STATS_COLUMN_LINE_HEIGHT = 9;
    private static final float MIN_CENTERED_TEXT_SCALE = 0.65F;
    private static final float MIN_LEFT_TEXT_SCALE = 0.72F;
    private static final byte ALL_PLAYER_MODEL_PARTS_MASK = allPlayerModelPartsMask();

    private static final ResourceLocation TEXTURE_BACKGROUND = new ResourceLocation(
        "mellow:textures/gui/background.png"
    );
    private static final ResourceLocation TEXTURE_LEFT_BUTTON = new ResourceLocation(
        "mellow:textures/gui/left_button.png"
    );
    private static final ResourceLocation TEXTURE_LEFT_BUTTON_PRESSED = new ResourceLocation(
        "mellow:textures/gui/left_button_pressed.png"
    );
    private static final ResourceLocation TEXTURE_RIGHT_BUTTON = new ResourceLocation(
        "mellow:textures/gui/right_button.png"
    );
    private static final ResourceLocation TEXTURE_RIGHT_BUTTON_PRESSED = new ResourceLocation(
        "mellow:textures/gui/right_button_pressed.png"
    );
    private static final ResourceLocation TEXTURE_SEARCH = new ResourceLocation(
        "mellow:textures/gui/search_bar.png"
    );
    private static final ResourceLocation TEXTURE_SEARCH_ACTIVE = new ResourceLocation(
        "mellow:textures/gui/search_bar_active.png"
    );

    private final PlayerCache playerCache;
    private final MellowOneConfig config;
    private final ProviderId providerId;
    private final PlayerProfile profile;
    private final PvSourceData sourceData;

    private PvMode selectedMode;
    private final List<String> categories = PvMode.categories();
    private int categoryIndex;

    private int panelX;
    private int panelY;

    private int leftButtonX;
    private int leftButtonY;
    private int leftButtonW;
    private int leftButtonH;

    private int rightButtonX;
    private int rightButtonY;
    private int rightButtonW;
    private int rightButtonH;

    private int searchX;
    private int searchY;
    private int searchW;
    private int searchH;

    private String searchText;
    private boolean searchActive;
    private long lastCursorBlinkAt;
    private boolean cursorVisible = true;

    private long lastSearchAt;
    private String statusText = "";
    private int statusColor = 0xFFBBBBBB;
    private long statusUntil;

    private List<SocialButton> socialButtons = new ArrayList<>();
    private EntityOtherPlayerMP previewPlayer;
    private ResourceLocation loadedSkin;
    private String loadedSkinType;
    private byte previewModelPartsMask = ALL_PLAYER_MODEL_PARTS_MASK;

    public PVGui(
        PlayerProfile profile,
        String rawProviderData,
        ProviderId providerId,
        PlayerCache playerCache,
        MellowOneConfig config
    ) {
        this(profile, rawProviderData, providerId, playerCache, config, PvMode.OVERALL);
    }

    public PVGui(
        PlayerProfile profile,
        String rawProviderData,
        ProviderId providerId,
        PlayerCache playerCache,
        MellowOneConfig config,
        PvMode preferredMode
    ) {
        this.profile = profile;
        this.providerId = providerId;
        this.playerCache = playerCache;
        this.config = config;
        this.sourceData = PvDataParser.parse(rawProviderData, providerId);
        this.selectedMode = preferredMode == null ? PvMode.OVERALL : preferredMode;
        this.categoryIndex = Math.max(0, categories.indexOf(this.selectedMode.getCategory()));
        this.searchText = profile == null ? "" : profile.getName();
    }

    @Override
    public void initGui() {
        panelX = (width - WIDTH) / 2;
        panelY = (height - HEIGHT) / 2;

        leftButtonW = 146;
        leftButtonH = 20;
        rightButtonW = 146;
        rightButtonH = 20;
        leftButtonX = panelX + 123;
        rightButtonX = panelX + 276;
        leftButtonY = panelY + 197;
        rightButtonY = panelY + 197;

        searchW = 103;
        searchH = 20;
        searchX = panelX + 10;
        searchY = panelY + 197;

        socialButtons = buildSocialButtons();
        lastCursorBlinkAt = System.currentTimeMillis();
        initPreviewPlayer();
    }

    @Override
    public void updateScreen() {
        long now = System.currentTimeMillis();
        if (searchActive && now - lastCursorBlinkAt >= 500) {
            cursorVisible = !cursorVisible;
            lastCursorBlinkAt = now;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        drawPanelTexture(TEXTURE_BACKGROUND);
        drawPanelTexture(
            isInside(mouseX, mouseY, leftButtonX, leftButtonY, leftButtonW, leftButtonH)
                ? TEXTURE_LEFT_BUTTON_PRESSED
                : TEXTURE_LEFT_BUTTON
        );
        drawPanelTexture(
            isInside(mouseX, mouseY, rightButtonX, rightButtonY, rightButtonW, rightButtonH)
                ? TEXTURE_RIGHT_BUTTON_PRESSED
                : TEXTURE_RIGHT_BUTTON
        );
        drawPanelTexture(
            searchActive || isInside(mouseX, mouseY, searchX, searchY, searchW, searchH)
                ? TEXTURE_SEARCH_ACTIVE
                : TEXTURE_SEARCH
        );

        BedwarsPlayer player = profile.getBedwarsPlayer();
        PvComputedStats stats = PvComputedStats.from(sourceData, player, selectedMode);

        renderPreviewEntity(mouseX, mouseY);
        drawTopCard(player, stats);
        drawProviderLabel();
        drawCornerCard();
        drawSocialButtons(mouseX, mouseY);
        drawStatsCard(stats);
        drawModeTexts();
        drawSearchFieldText();

        if (!statusText.isEmpty() && System.currentTimeMillis() < statusUntil) {
            drawCenteredBoundedString(
                statusText,
                panelX + WIDTH / 2,
                panelY + HEIGHT - 11,
                STATUS_TEXT_WIDTH,
                statusColor,
                1.0F,
                MIN_CENTERED_TEXT_SCALE
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPanelTexture(ResourceLocation texture) {
        drawTexturedQuad(texture, panelX, panelY, WIDTH, HEIGHT, GL11.GL_NEAREST);
    }

    private void drawTexturedQuad(
        ResourceLocation texture,
        float x,
        float y,
        float width,
        float height,
        int filter
    ) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE,
            GL11.GL_ONE_MINUS_SRC_ALPHA
        );
        GlStateManager.color(1F, 1F, 1F, 1F);

        mc.getTextureManager().bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(x, y + height, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldRenderer.pos(x + width, y + height, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldRenderer.pos(x + width, y, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldRenderer.pos(x, y, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager.disableBlend();
    }

    private void drawTopCard(BedwarsPlayer player, PvComputedStats stats) {
        int topCenterX = panelX + 216;
        int topStartY = panelY + 15;

        String rankedName = player.getFormattedNameWithRank();
        drawCenteredBoundedString(
            rankedName,
            topCenterX,
            topStartY,
            TOP_CARD_TEXT_WIDTH,
            0xFFFFFF,
            1.0F,
            MIN_CENTERED_TEXT_SCALE
        );

        String currentStar = BedwarsStarFormatter.format(Math.max(0, stats.level));
        String nextStar = BedwarsStarFormatter.format(Math.max(0, stats.level + 1));
        int bedwarsExp = sourceData.getBedwarsExperience();
        int currentExp = PvExperience.getCurrentExperienceInLevel(bedwarsExp);
        int requiredExp = PvExperience.getExperienceRequiredForCurrentLevel(bedwarsExp);

        drawCenteredBoundedString(
            "§7Level: " + currentStar,
            topCenterX,
            topStartY + 12,
            TOP_CARD_TEXT_WIDTH,
            0xFFFFFF,
            1.0F,
            MIN_CENTERED_TEXT_SCALE
        );
        drawCenteredBoundedString(
            "§7EXP Progress: §b" + commas(currentExp) + "§7/§a" + commas(requiredExp),
            topCenterX,
            topStartY + 24,
            TOP_CARD_TEXT_WIDTH,
            0xFFFFFF,
            1.0F,
            MIN_CENTERED_TEXT_SCALE
        );
        drawCenteredBoundedString(
            currentStar + PvExperience.getProgressBar(bedwarsExp) + nextStar,
            topCenterX,
            topStartY + 36,
            TOP_CARD_TEXT_WIDTH,
            0xFFFFFF,
            1.0F,
            0.55F
        );
    }

    private void drawProviderLabel() {
        drawRightBoundedString(
            "§7Provider: §f" + providerName(),
            panelX + WIDTH - 8,
            panelY + 6,
            PROVIDER_LABEL_WIDTH,
            0xFFFFFF,
            1.0F,
            0.75F
        );
    }

    private void drawCornerCard() {
        int x = panelX + 323;
        int y = panelY + 20;
        drawLeftBoundedString(
            "§6NW Level: §f" + commas(sourceData.getNetworkLevel()),
            x,
            y,
            CORNER_CARD_WIDTH,
            0xFFFFFF,
            1.0F,
            MIN_LEFT_TEXT_SCALE
        );
        drawLeftBoundedString(
            "§dKarma: §f" + commas(sourceData.getKarma()),
            x,
            y + 12,
            CORNER_CARD_WIDTH,
            0xFFFFFF,
            1.0F,
            MIN_LEFT_TEXT_SCALE
        );
        drawLeftBoundedString(
            "§9Gifted: §f" + commas(sourceData.getGifted()),
            x,
            y + 24,
            CORNER_CARD_WIDTH,
            0xFFFFFF,
            1.0F,
            MIN_LEFT_TEXT_SCALE
        );
        drawLeftBoundedString(
            "§eAP: §f" + commas(sourceData.getAchievementPoints()),
            x,
            y + 36,
            CORNER_CARD_WIDTH,
            0xFFFFFF,
            1.0F,
            MIN_LEFT_TEXT_SCALE
        );
    }

    private void drawStatsCard(PvComputedStats s) {
        int centerX = panelX + 205;
        int topY = panelY + 65;

        drawCenteredBoundedString(
            "§cBedWars §fStats §7(" + selectedMode.getFullName() + ")",
            centerX,
            topY + 5,
            STATS_TITLE_WIDTH,
            0xFFFFFF,
            1.0F,
            MIN_CENTERED_TEXT_SCALE
        );

        int leftX = panelX + 130;
        int middleX = panelX + 229;
        int rightX = panelX + 327;
        int baseY = topY + 19;

        List<String> left = new ArrayList<>();
        left.add("§9" + commas(s.tokens) + " §fTokens");
        left.add("§9" + commas(s.slumberTickets) + " §fTickets");
        left.add("");
        left.add("§a" + commas(s.wins) + " §fWins");
        left.add("§a" + commas(s.kills) + " §fKills");
        left.add("§a" + commas(s.finalKills) + " §fFinal Kills");
        left.add("§a" + commas(s.beds) + " §fBeds Broken");
        left.add("");
        left.add("§e" + d2(s.killsPerGame) + " §fKills/Game");
        left.add("§b" + d2(s.killsPerStar) + " §fKills/Star");

        List<String> middle = new ArrayList<>();
        middle.add("§9" + d1(s.clutchRatePercent) + "% §fClutch Rate");
        middle.add("§9" + commas(s.gamesPlayed) + " §fGames Played");
        middle.add("");
        middle.add("§c" + commas(s.losses) + " §fLosses");
        middle.add("§c" + commas(s.deaths) + " §fDeaths");
        middle.add("§c" + commas(s.finalDeaths) + " §fFinal Deaths");
        middle.add("§c" + commas(s.bedsLost) + " §fBeds Lost");
        middle.add("");
        middle.add("§e" + d2(s.finalsPerGame) + " §fFinals/Game");
        middle.add("§b" + d2(s.finalsPerStar) + " §fFinals/Star");

        List<String> right = new ArrayList<>();
        right.add("§9" + d1(s.winRatePercent) + "% §fWin Rate");
        right.add("§9" + commas(s.skillIndex) + " §fSkill Index");
        right.add("");
        right.add("§d" + d2(s.wlr) + " §fWLR");
        right.add("§d" + d2(s.kdr) + " §fKDR");
        right.add("§d" + d2(s.fkdr) + " §fFKDR");
        right.add("§d" + d2(s.bblr) + " §fBBLR");
        right.add("");
        right.add("§e" + d2(s.bedsPerGame) + " §fBeds/Game");
        right.add("§b" + d2(s.bedsPerStar) + " §fBeds/Star");

        drawColumnFitted(leftX, baseY, left, LEFT_STATS_COLUMN_WIDTH, 0.825F, MIN_LEFT_TEXT_SCALE);
        drawColumnFitted(
            middleX,
            baseY,
            middle,
            MIDDLE_STATS_COLUMN_WIDTH,
            0.825F,
            MIN_LEFT_TEXT_SCALE
        );
        drawColumnFitted(
            rightX,
            baseY,
            right,
            RIGHT_STATS_COLUMN_WIDTH,
            0.825F,
            MIN_LEFT_TEXT_SCALE
        );
    }

    private void drawColumnFitted(
        int x,
        int y,
        List<String> lines,
        int maxWidth,
        float preferredScale,
        float minScale
    ) {
        int lineY = y;
        for (String line : lines) {
            if (!line.isEmpty()) {
                drawLeftBoundedString(
                    line,
                    x,
                    lineY,
                    maxWidth,
                    0xFFFFFF,
                    preferredScale,
                    minScale
                );
            }
            lineY += STATS_COLUMN_LINE_HEIGHT;
        }
    }

    private void drawModeTexts() {
        drawCenteredBoundedString(
            "§f" + currentCategory(),
            leftButtonX + leftButtonW / 2,
            leftButtonY + 6,
            CATEGORY_BUTTON_TEXT_WIDTH,
            0xFFFFFF,
            1.0F,
            MIN_CENTERED_TEXT_SCALE
        );
        drawCenteredBoundedString(
            "§f" + selectedMode.getFullName(),
            rightButtonX + rightButtonW / 2,
            rightButtonY + 6,
            MODE_BUTTON_TEXT_WIDTH,
            0xFFFFFF,
            1.0F,
            MIN_CENTERED_TEXT_SCALE
        );
    }

    private void drawSearchFieldText() {
        String visibleSearchText = fontRendererObj.trimStringToWidth(searchText, searchW - 10, true);
        String display = visibleSearchText.isEmpty() && !searchActive ? "§7Search..." : "§f" + visibleSearchText;
        fontRendererObj.drawStringWithShadow(display, searchX + 4, searchY + 6, 0xFFFFFF);

        if (searchActive && cursorVisible) {
            int cursorX = searchX + 4 + fontRendererObj.getStringWidth(visibleSearchText);
            drawRect(cursorX, searchY + 4, cursorX + 1, searchY + searchH - 4, 0xFFFFFFFF);
        }
    }

    private void drawSocialButtons(int mouseX, int mouseY) {
        if (socialButtons.isEmpty()) {
            drawCenteredString(fontRendererObj, "§cNo Socials", panelX + 62, panelY + 183, 0xFFFFFF);
            return;
        }

        int totalWidth = (SOCIAL_ICON_SIZE * socialButtons.size()) + (SOCIAL_GAP * (socialButtons.size() - 1));
        int startX = panelX + 62 - (totalWidth / 2);
        int y = panelY + 182;
        int x = startX;

        for (SocialButton button : socialButtons) {
            button.x = x;
            button.y = y;
            button.w = SOCIAL_ICON_SIZE;
            button.h = SOCIAL_ICON_SIZE;

            boolean hovered = isInside(mouseX, mouseY, button.x, button.y, button.w, button.h);
            ResourceLocation texture = hovered && Mouse.isButtonDown(0)
                ? button.type.pressedTexture
                : button.type.normalTexture;

            drawSocialTexture(texture, button.x, button.y);

            if (hovered) {
                drawSocialTooltip(button, mouseX, mouseY);
            }

            x += SOCIAL_ICON_SIZE + SOCIAL_GAP;
        }
    }

    private void drawSocialTexture(ResourceLocation texture, int x, int y) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(texture);
        drawModalRectWithCustomSizedTexture(
            x,
            y,
            0F,
            0F,
            SOCIAL_ICON_SIZE,
            SOCIAL_ICON_SIZE,
            SOCIAL_ICON_SIZE,
            SOCIAL_ICON_SIZE
        );
    }

    private void drawSocialTooltip(SocialButton button, int mouseX, int mouseY) {
        String actorName = profile.getName();
        String action = button.type.copyOnly ? "copy" : "visit";
        String line1 = "§7Click to " + action + " §a" + actorName + "§7's " + button.type.prettyName + ".";
        String line2 = "§b" + (button.type.copyOnly ? button.value : ensureUrl(button.value));
        List<String> tooltipLines = new ArrayList<>();
        tooltipLines.addAll(fontRendererObj.listFormattedStringToWidth(line1, SOCIAL_TOOLTIP_WIDTH));
        tooltipLines.addAll(fontRendererObj.listFormattedStringToWidth(line2, SOCIAL_TOOLTIP_WIDTH));
        drawHoveringText(tooltipLines, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            return;
        }

        if (isInside(mouseX, mouseY, leftButtonX, leftButtonY, leftButtonW, leftButtonH)) {
            cycleCategory();
            if (mc.thePlayer != null) {
                mc.thePlayer.playSound("random.click", 0.5F, 2F);
            }
            return;
        }

        if (isInside(mouseX, mouseY, rightButtonX, rightButtonY, rightButtonW, rightButtonH)) {
            cycleMode();
            if (mc.thePlayer != null) {
                mc.thePlayer.playSound("random.click", 0.5F, 2F);
            }
            return;
        }

        if (isInside(mouseX, mouseY, searchX, searchY, searchW, searchH)) {
            searchActive = true;
            return;
        }
        searchActive = false;

        for (SocialButton button : socialButtons) {
            if (isInside(mouseX, mouseY, button.x, button.y, button.w, button.h)) {
                onSocialClick(button);
                break;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (!searchActive) {
            super.keyTyped(typedChar, keyCode);
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            searchActive = false;
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            performSearch();
            return;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (!searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
            }
            return;
        }

        if (isValidSearchChar(typedChar) && searchText.length() < 16) {
            searchText += typedChar;
        }
    }

    private void performSearch() {
        String query = searchText == null ? "" : searchText.trim();
        if (query.isEmpty()) {
            setStatus("§cEnter a username.", 0xFFFF6666, 2000L);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSearchAt < SEARCH_COOLDOWN_MS) {
            setStatus("§cPlease wait before searching again.", 0xFFFF6666, 2000L);
            return;
        }
        lastSearchAt = now;

        setStatus("§aFetching profile for " + query + "...", 0xFFA8FFB0, 3000L);

        final PvMode modeToKeep = selectedMode;
        AsyncExecutor.getInstance().command(() -> {
            PlayerProfile newProfile = playerCache.getProfile(query);
            String rawData = playerCache.fetchRawPlayerData(query);
            StatsProvider provider = playerCache.getSelectedProvider();
            ProviderId newProviderId = provider == null ? providerId : provider.getProviderId();

            if (newProfile == null || newProfile.getBedwarsPlayer() == null) {
                MainThreadDispatcher.run(() ->
                    setStatus("§cFailed to fetch profile for " + query, 0xFFFF6666, 3000L)
                );
                return;
            }

            MainThreadDispatcher.run(() ->
                mc.displayGuiScreen(
                    new PVGui(newProfile, rawData, newProviderId, playerCache, config, modeToKeep)
                )
            );
        });
    }

    private void cycleCategory() {
        categoryIndex++;
        if (categoryIndex >= categories.size()) {
            categoryIndex = 0;
        }
        List<PvMode> modes = modesInCurrentCategory();
        if (!modes.isEmpty()) {
            selectedMode = modes.get(0);
        }
    }

    private void cycleMode() {
        List<PvMode> modes = modesInCurrentCategory();
        if (modes.isEmpty()) {
            return;
        }

        int modeIndex = modes.indexOf(selectedMode);
        modeIndex = (modeIndex + 1) % modes.size();
        selectedMode = modes.get(modeIndex);
    }

    private List<PvMode> modesInCurrentCategory() {
        return PvMode.modesForCategory(currentCategory());
    }

    private String currentCategory() {
        if (categoryIndex < 0 || categoryIndex >= categories.size()) {
            return "Core Modes";
        }
        return categories.get(categoryIndex);
    }

    private boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean isValidSearchChar(char typedChar) {
        return Character.isLetterOrDigit(typedChar) || typedChar == '_';
    }

    private void onSocialClick(SocialButton button) {
        if (button == null || button.value == null || button.value.isEmpty()) {
            return;
        }

        if (button.type.copyOnly) {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(button.value), null);
                ChatUtils.sendMessage("§aCopied " + button.type.prettyName + " to clipboard.");
            } catch (Exception e) {
                ChatUtils.sendMessage("§cFailed to copy " + button.type.prettyName + ".");
            }
            return;
        }

        try {
            String url = ensureUrl(button.value);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                ChatUtils.sendMessage("§eDesktop browsing is not supported on this system.");
            }
        } catch (Exception e) {
            ChatUtils.sendMessage("§cFailed to open " + button.type.prettyName + ".");
        }
    }

    private String ensureUrl(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.startsWith("http://") || value.startsWith("https://") ? value : "https://" + value;
    }

    private List<SocialButton> buildSocialButtons() {
        List<SocialButton> buttons = new ArrayList<>();
        for (SocialType socialType : SocialType.values()) {
            String value = sourceData.social(socialType.key);
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            buttons.add(new SocialButton(socialType, value.trim()));
        }
        return buttons;
    }

    private void setStatus(String text, int color, long durationMs) {
        this.statusText = text == null ? "" : text;
        this.statusColor = color;
        this.statusUntil = System.currentTimeMillis() + Math.max(500L, durationMs);
    }

    private String providerName() {
        if (providerId == null) {
            return "Unknown";
        }
        switch (providerId) {
            case HYPIXEL_PUBLIC:
                return "Hypixel Public API";
            case NADESHIKO:
                return "Nadeshiko";
            case ABYSS:
                return "Abyss";
            case BORDIC:
                return "Bordic";
            default:
                return providerId.name();
        }
    }

    private String commas(Number value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    private String d2(double value) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(value);
    }

    private String d1(double value) {
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(value);
    }

    private void initPreviewPlayer() {
        UUID profileUuid = resolveProfileUuid();
        UUID previewUuid = profileUuid == null ? new UUID(0L, 0L) : profileUuid;
        GameProfile previewProfile = new GameProfile(previewUuid, profile.getName());
        previewModelPartsMask = resolvePreviewModelPartsMask(profileUuid);

        if (mc.theWorld != null) {
            previewPlayer = new EntityOtherPlayerMP(mc.theWorld, previewProfile) {
                @Override
                public ResourceLocation getLocationSkin() {
                    return loadedSkin == null ? super.getLocationSkin() : loadedSkin;
                }

                @Override
                public String getSkinType() {
                    return loadedSkinType == null ? super.getSkinType() : loadedSkinType;
                }
            };
            previewPlayer.getDataWatcher().updateObject(10, Byte.valueOf(previewModelPartsMask));
        }

        requestSkinLoad(new GameProfile(profileUuid, profile.getName()));
    }

    private void renderPreviewEntity(int mouseX, int mouseY) {
        if (previewPlayer == null) {
            return;
        }

        int entityX = panelX + 62;
        int entityY = panelY + 146;
        int scale = 64;
        float lookX = (float) (entityX - mouseX);
        float lookY = (float) (entityY - 40 - mouseY);

        drawEntityOnScreen(entityX, entityY, scale, lookX, lookY, previewPlayer);
    }

    private void drawEntityOnScreen(
        int posX,
        int posY,
        int scale,
        float mouseX,
        float mouseY,
        EntityLivingBase entity
    ) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posX, (float) posY, 60.0F);
        GlStateManager.scale((float) (-scale), (float) scale, (float) scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        float renderYawOffset = entity.renderYawOffset;
        float rotationYaw = entity.rotationYaw;
        float rotationPitch = entity.rotationPitch;
        float prevRotationYawHead = entity.prevRotationYawHead;
        float rotationYawHead = entity.rotationYawHead;
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(25.0F, 1.0F, 0.0F, 0.0F);
        entity.renderYawOffset = (float) Math.atan(mouseX / 40.0F) * 20.0F;
        entity.rotationYaw = (float) Math.atan(mouseX / 40.0F) * 40.0F;
        entity.rotationPitch = -((float) Math.atan(mouseY / 40.0F)) * 20.0F;
        entity.rotationYawHead = entity.rotationYaw;
        entity.prevRotationYawHead = entity.rotationYaw;
        RenderManager renderManager = mc.getRenderManager();
        renderManager.setPlayerViewY(180.0F);
        renderManager.setRenderShadow(false);
        renderManager.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        renderManager.setRenderShadow(true);
        entity.renderYawOffset = renderYawOffset;
        entity.rotationYaw = rotationYaw;
        entity.rotationPitch = rotationPitch;
        entity.prevRotationYawHead = prevRotationYawHead;
        entity.rotationYawHead = rotationYawHead;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void requestSkinLoad(GameProfile baseProfile) {
        if (baseProfile == null || mc == null) {
            return;
        }

        AsyncExecutor.getInstance().command(() -> {
            GameProfile hydratedProfile = hydrateProfile(baseProfile);
            MainThreadDispatcher.run(() -> loadHydratedSkin(hydratedProfile));
        });
    }

    private GameProfile hydrateProfile(GameProfile baseProfile) {
        if (baseProfile == null || mc == null || mc.getSessionService() == null) {
            return baseProfile;
        }

        if (baseProfile.isComplete() && baseProfile.getProperties().containsKey("textures")) {
            return baseProfile;
        }

        try {
            GameProfile hydrated = mc.getSessionService().fillProfileProperties(baseProfile, false);
            return hydrated == null ? baseProfile : hydrated;
        } catch (Exception ignored) {
            return baseProfile;
        }
    }

    private void loadHydratedSkin(GameProfile hydratedProfile) {
        if (hydratedProfile == null || mc == null || mc.getSkinManager() == null) {
            return;
        }

        try {
            mc.getSkinManager().loadProfileTextures(
                hydratedProfile,
                (type, location, profileTexture) -> {
                    if (type == MinecraftProfileTexture.Type.SKIN) {
                        loadedSkin = location;
                        loadedSkinType = resolveSkinType(profileTexture);
                    }
                },
                false
            );
        } catch (Exception ignored) {}
    }

    private String resolveSkinType(MinecraftProfileTexture profileTexture) {
        if (profileTexture == null) {
            return null;
        }

        String model = profileTexture.getMetadata("model");
        return "slim".equalsIgnoreCase(model) ? "slim" : "default";
    }

    private UUID resolveProfileUuid() {
        try {
            return UUIDUtils.fromString(profile.getUuid());
        } catch (Exception ignored) {
            return null;
        }
    }

    private EntityPlayer resolveLivePlayer(UUID uuid) {
        if (uuid == null || mc == null || mc.theWorld == null) {
            return null;
        }
        return mc.theWorld.getPlayerEntityByUUID(uuid);
    }

    private byte resolvePreviewModelPartsMask(UUID uuid) {
        EntityPlayer livePlayer = resolveLivePlayer(uuid);
        if (livePlayer != null) {
            return livePlayer.getDataWatcher().getWatchableObjectByte(10);
        }
        return ALL_PLAYER_MODEL_PARTS_MASK;
    }

    private boolean isPreviewModelPartEnabled(EnumPlayerModelParts part) {
        return (previewModelPartsMask & part.getPartMask()) == part.getPartMask();
    }

    private void drawCenteredBoundedString(
        String text,
        int centerX,
        int y,
        int maxWidth,
        int color,
        float preferredScale,
        float minScale
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }

        FittedText fitted = fitText(text, maxWidth, preferredScale, minScale);
        int textWidth = fontRendererObj.getStringWidth(fitted.text);
        int drawX = centerX - Math.round((textWidth * fitted.scale) / 2.0F);
        drawScaledString(fitted.text, drawX, y, fitted.scale, color);
    }

    private void drawLeftBoundedString(
        String text,
        int x,
        int y,
        int maxWidth,
        int color,
        float preferredScale,
        float minScale
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }

        FittedText fitted = fitText(text, maxWidth, preferredScale, minScale);
        drawScaledString(fitted.text, x, y, fitted.scale, color);
    }

    private void drawRightBoundedString(
        String text,
        int rightX,
        int y,
        int maxWidth,
        int color,
        float preferredScale,
        float minScale
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }

        FittedText fitted = fitText(text, maxWidth, preferredScale, minScale);
        int drawX = rightX - Math.round(fontRendererObj.getStringWidth(fitted.text) * fitted.scale);
        drawScaledString(fitted.text, drawX, y, fitted.scale, color);
    }

    private void drawScaledString(String text, int x, int y, float scale, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (scale >= 0.999F) {
            fontRendererObj.drawStringWithShadow(text, x, y, color);
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        fontRendererObj.drawStringWithShadow(text, Math.round(x / scale), Math.round(y / scale), color);
        GlStateManager.popMatrix();
    }

    private FittedText fitText(
        String text,
        int maxWidth,
        float preferredScale,
        float minScale
    ) {
        float scale = preferredScale <= 0.0F ? 1.0F : preferredScale;
        int textWidth = fontRendererObj.getStringWidth(text);

        if (maxWidth <= 0 || textWidth <= 0 || (textWidth * scale) <= maxWidth) {
            return new FittedText(text, scale);
        }

        float fitScale = (float) maxWidth / (float) textWidth;
        if (fitScale >= minScale) {
            return new FittedText(text, fitScale);
        }

        float boundedScale = Math.max(0.1F, minScale);
        String clamped = clampTextWithEllipsis(text, Math.max(0, Math.round(maxWidth / boundedScale)));
        return new FittedText(clamped, boundedScale);
    }

    private String clampTextWithEllipsis(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }

        if (fontRendererObj.getStringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "§7...";
        int ellipsisWidth = fontRendererObj.getStringWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return ellipsisWidth == maxWidth ? ellipsis : "";
        }

        String trimmed = fontRendererObj.trimStringToWidth(text, Math.max(0, maxWidth - ellipsisWidth));
        while (trimmed.endsWith("§")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        if (trimmed.isEmpty()) {
            return ellipsis;
        }

        return trimmed + ellipsis;
    }

    private static byte allPlayerModelPartsMask() {
        int mask = 0;
        for (EnumPlayerModelParts part : EnumPlayerModelParts.values()) {
            if (part == EnumPlayerModelParts.CAPE) {
                continue;
            }
            mask |= part.getPartMask();
        }
        return (byte) mask;
    }

    private enum SocialType {
        TIKTOK("TIKTOK", "TikTok", false, "tiktok_logo"),
        TWITCH("TWITCH", "Twitch", false, "twitch_logo"),
        DISCORD("DISCORD", "Discord", true, "discord_logo"),
        HYPIXEL("HYPIXEL", "Hypixel", false, "hypixel_logo"),
        TWITTER("TWITTER", "Twitter", false, "twitter_logo"),
        YOUTUBE("YOUTUBE", "YouTube", false, "youtube_logo"),
        INSTAGRAM("INSTAGRAM", "Instagram", false, "instagram_logo");

        private final String key;
        private final String prettyName;
        private final boolean copyOnly;
        private final ResourceLocation normalTexture;
        private final ResourceLocation pressedTexture;

        SocialType(String key, String prettyName, boolean copyOnly, String textureName) {
            this.key = key;
            this.prettyName = prettyName;
            this.copyOnly = copyOnly;
            this.normalTexture = new ResourceLocation("mellow:textures/socials/" + textureName + ".png");
            this.pressedTexture = new ResourceLocation("mellow:textures/socials/" + textureName + "_pressed.png");
        }
    }

    private static class SocialButton {

        private final SocialType type;
        private final String value;

        private int x;
        private int y;
        private int w;
        private int h;

        private SocialButton(SocialType type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    private static class FittedText {

        private final String text;
        private final float scale;

        private FittedText(String text, float scale) {
            this.text = text;
            this.scale = scale;
        }
    }
}
