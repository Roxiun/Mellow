package com.roxiun.mellow.config;

import java.util.function.Consumer;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Keyboard;

/** Keeps API keys masked; v1's stock Text annotation has no password option. */
public final class ApiKeyScreen extends GuiScreen {
    private final GuiScreen parent;
    private final String title;
    private final Consumer<String> save;
    private final String initial;
    private GuiTextField input;
    private boolean reveal;

    public ApiKeyScreen(GuiScreen parent, String title, String value, Consumer<String> save) {
        this.parent = parent;
        this.title = title;
        this.initial = value;
        this.save = save;
    }

    @Override public void initGui() {
        String value = input == null ? initial : input.getText();
        input = new GuiTextField(0, fontRendererObj, width / 2 - 150, height / 2 - 20, 300, 20);
        input.setMaxStringLength(1024);
        input.setText(value == null ? "" : value);
        input.setFocused(true);
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 150, height / 2 + 12, 96, 20, "Save"));
        buttonList.add(new GuiButton(1, width / 2 - 48, height / 2 + 12, 96, 20, "Cancel"));
        buttonList.add(new GuiButton(2, width / 2 + 54, height / 2 + 12, 96, 20, reveal ? "Hide" : "Show"));
        Keyboard.enableRepeatEvents(true);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, title, width / 2, height / 2 - 45, 0xffffff);
        if (reveal) input.drawTextBox();
        else {
            String value = input.getText();
            int cursor = input.getCursorPosition();
            int selection = input.getSelectionEnd();
            input.setText("*".repeat(value.length()));
            input.setCursorPosition(cursor);
            input.setSelectionPos(selection);
            input.drawTextBox();
            input.setText(value);
            input.setCursorPosition(cursor);
            input.setSelectionPos(selection);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override public void updateScreen() { input.updateCursorCounter(); }
    @Override protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button);
        input.mouseClicked(x, y, button);
    }
    @Override protected void keyTyped(char typed, int key) {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { submit(); return; }
        input.textboxKeyTyped(typed, key);
    }
    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 0) submit();
        else if (button.id == 1) mc.displayGuiScreen(parent);
        else { reveal = !reveal; button.displayString = reveal ? "Hide" : "Show"; }
    }
    private void submit() { save.accept(input.getText().trim()); mc.displayGuiScreen(parent); }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
