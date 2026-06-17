package com.sanderbloem.smpessentials.client;

import com.sanderbloem.smpessentials.data.WalletData;
import com.sanderbloem.smpessentials.menu.AuctionTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AuctionTerminalScreen extends AbstractContainerScreen<AuctionTerminalMenu> {

    private static final int SHELL = 0xFFD9D9D9;
    private static final int SHELL_SHADOW = 0xFF8E8E8E;
    private static final int SHELL_HIGHLIGHT = 0xFFFFFFFF;
    private static final int SCREEN_BG = 0xFF202124;
    private static final int SCREEN_PANEL = 0xFF2B2C30;
    private static final int SCREEN_EDGE = 0xFF0A0A0A;
    private static final int SLOT_BG = 0xFFAFAFAF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int SOFT = 0xFFC8CED8;
    private static final int MUTED = 0xFF9EA4AE;
    private static final int HEADER = 0xFF34363B;
    private static final int GOLD = 0xFFFFE860;
    private static final int GREEN = 0xFF6DDA64;
    private static final int ORANGE = 0xFFFFB347;
    private static final int COPPER = 0xFFC77E49;
    private static final int SILVER = 0xFFD5DCE6;
    private static final int CYAN = 0xFF53C7E8;
    private static final int RESET = 0xFFB5BDC7;

    private static final int SLOT_STEP = 24;
    private static final int LISTING_SLOT_X = 28;
    private static final int LISTING_SLOT_Y = 38;
    private static final int LISTING_BUTTON_Y = 64;
    private static final int NAV_BUTTON_Y = 118;
    private static final int SIDEBAR_X = 190;
    private static final int SIDEBAR_W = 42;
    private static final int SIDEBAR_H = 18;
    private static final int SIDEBAR_GAP = 6;

    private int lastSignature = Integer.MIN_VALUE;

    public AuctionTerminalScreen(AuctionTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 248, 272);
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        this.clearWidgets();
        int x = this.leftPos;
        int y = this.topPos;

        for (int i = 0; i < AuctionTerminalMenu.DISPLAY_COUNT; i++) {
            int ownership = this.menu.slotOwnership(i);
            if (ownership < 0) continue;
            final int btnId = AuctionTerminalMenu.BTN_ACTION_BASE + i;
            String label = ownership == 1 ? "X" : "KOOP";
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> send(btnId))
                    .bounds(x + LISTING_SLOT_X + i * SLOT_STEP, y + LISTING_BUTTON_Y, 18, 14).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> send(AuctionTerminalMenu.BTN_PREV))
                .bounds(x + 28, y + NAV_BUTTON_Y, 18, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> send(AuctionTerminalMenu.BTN_NEXT))
                .bounds(x + 126, y + NAV_BUTTON_Y, 18, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal(this.menu.isFilterMine() ? "MIJN" : "ALLES"),
                        b -> send(AuctionTerminalMenu.BTN_FILTER))
                .bounds(x + SIDEBAR_X, y + 34, SIDEBAR_W, SIDEBAR_H).build());
        this.addRenderableWidget(Button.builder(Component.literal("PLAATS"), b -> send(AuctionTerminalMenu.BTN_POST))
                .bounds(x + SIDEBAR_X, y + 34 + (SIDEBAR_H + SIDEBAR_GAP), SIDEBAR_W, SIDEBAR_H).build());
        this.addRenderableWidget(Button.builder(Component.literal("+K"), b -> send(AuctionTerminalMenu.BTN_ADD_COPPER))
                .bounds(x + SIDEBAR_X, y + 34 + 2 * (SIDEBAR_H + SIDEBAR_GAP), SIDEBAR_W, SIDEBAR_H).build());
        this.addRenderableWidget(Button.builder(Component.literal("+Z"), b -> send(AuctionTerminalMenu.BTN_ADD_SILVER))
                .bounds(x + SIDEBAR_X, y + 34 + 3 * (SIDEBAR_H + SIDEBAR_GAP), SIDEBAR_W, SIDEBAR_H).build());
        this.addRenderableWidget(Button.builder(Component.literal("+G"), b -> send(AuctionTerminalMenu.BTN_ADD_GOLD))
                .bounds(x + SIDEBAR_X, y + 34 + 4 * (SIDEBAR_H + SIDEBAR_GAP), SIDEBAR_W, SIDEBAR_H).build());
        this.addRenderableWidget(Button.builder(Component.literal("RST"), b -> send(AuctionTerminalMenu.BTN_RESET))
                .bounds(x + SIDEBAR_X, y + 34 + 5 * (SIDEBAR_H + SIDEBAR_GAP), SIDEBAR_W, SIDEBAR_H).build());

        lastSignature = signature();
    }

    private int signature() {
        int sig = this.menu.getPage() * 31 + (this.menu.isFilterMine() ? 1 : 0) * 17 + this.menu.getPendingPrice() * 7;
        for (int i = 0; i < AuctionTerminalMenu.DISPLAY_COUNT; i++) {
            sig = sig * 31 + this.menu.slotOwnership(i);
            sig = sig * 31 + (this.menu.getSlot(i).hasItem() ? 1 : 0);
        }
        return sig;
    }

    private void send(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (signature() != lastSignature) rebuild();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ex, int mouseX, int mouseY, float partial) {
        super.extractBackground(ex, mouseX, mouseY, partial);
        int x = this.leftPos;
        int y = this.topPos;

        ex.fill(x - 3, y - 3, x + this.imageWidth + 3, y + this.imageHeight + 3, SHELL_SHADOW);
        ex.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, SHELL_HIGHLIGHT);
        ex.fill(x, y, x + this.imageWidth, y + this.imageHeight, SHELL);

        ex.fill(x + 10, y + 8, x + this.imageWidth - 10, y + 22, HEADER);
        ex.text(this.font, "AUCTION TERMINAL", x + 72, y + 12, WHITE, false);

        int screenX1 = x + 18;
        int screenY1 = y + 30;
        int screenX2 = x + 174;
        int screenY2 = y + 140;
        ex.fill(screenX1 - 2, screenY1 - 2, screenX2 + 2, screenY2 + 2, SHELL_HIGHLIGHT);
        ex.fill(screenX1 - 1, screenY1 - 1, screenX2 + 1, screenY2 + 1, SCREEN_EDGE);
        ex.fill(screenX1, screenY1, screenX2, screenY2, SCREEN_BG);

        ex.text(this.font, "live listings", x + 24, y + 32, SOFT, false);
        ex.text(this.font, "plaats item", x + 146, y + 32, SOFT, false);

        for (int i = 0; i < AuctionTerminalMenu.DISPLAY_COUNT; i++) {
            int slotX = x + LISTING_SLOT_X + i * SLOT_STEP;
            int slotY = y + LISTING_SLOT_Y;
            ex.fill(slotX, slotY, slotX + 18, slotY + 18, SCREEN_PANEL);
        }

        ex.fill(x + 140, y + 52, x + 158, y + 70, SCREEN_PANEL);
        ex.text(this.font, "prijs", x + 140, y + 78, MUTED, false);
        ex.text(this.font, WalletData.formatBalance(this.menu.getPendingPrice()), x + 140, y + 90, GOLD, false);
        ex.text(this.font, "K=1  Z=9  G=81", x + 140, y + 104, MUTED, false);

        ex.text(this.font, "filter: " + (this.menu.isFilterMine() ? "mijn aanbod" : "alles"), x + 24, y + 92, WHITE, false);
        ex.text(this.font, "pagina " + (this.menu.getPage() + 1) + "/" + this.menu.getTotalPages(), x + 58, y + 122, WHITE, false);

        drawSidebarButton(ex, x + SIDEBAR_X, y + 34, SIDEBAR_W, SIDEBAR_H, GREEN);
        drawSidebarButton(ex, x + SIDEBAR_X, y + 34 + (SIDEBAR_H + SIDEBAR_GAP), SIDEBAR_W, SIDEBAR_H, ORANGE);
        drawSidebarButton(ex, x + SIDEBAR_X, y + 34 + 2 * (SIDEBAR_H + SIDEBAR_GAP), SIDEBAR_W, SIDEBAR_H, COPPER);
        drawSidebarButton(ex, x + SIDEBAR_X, y + 34 + 3 * (SIDEBAR_H + SIDEBAR_GAP), SIDEBAR_W, SIDEBAR_H, SILVER);
        drawSidebarButton(ex, x + SIDEBAR_X, y + 34 + 4 * (SIDEBAR_H + SIDEBAR_GAP), SIDEBAR_W, SIDEBAR_H, CYAN);
        drawSidebarButton(ex, x + SIDEBAR_X, y + 34 + 5 * (SIDEBAR_H + SIDEBAR_GAP), SIDEBAR_W, SIDEBAR_H, RESET);

        for (int i = 0; i < AuctionTerminalMenu.INV_START + 36; i++) {
            var s = this.menu.getSlot(i);
            ex.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, SLOT_BG);
        }

        ex.text(this.font, "inventaris", x + 16, y + 176, HEADER, false);
    }

    private void drawSidebarButton(GuiGraphicsExtractor ex, int x1, int y1, int width, int height, int color) {
        ex.fill(x1 - 1, y1 - 1, x1 + width + 1, y1 + height + 1, SCREEN_EDGE);
        ex.fill(x1, y1, x1 + width, y1 + height, color);
        ex.fill(x1 + 2, y1 + 2, x1 + width - 2, y1 + height - 2, 0x33000000 | (color & 0x00FFFFFF));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ex, int mouseX, int mouseY) {
    }
}
