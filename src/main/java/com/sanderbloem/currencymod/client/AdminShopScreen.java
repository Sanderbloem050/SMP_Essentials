package com.sanderbloem.currencymod.client;

import com.sanderbloem.currencymod.data.WalletData;
import com.sanderbloem.currencymod.menu.AdminShopMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AdminShopScreen extends AbstractContainerScreen<AdminShopMenu> {

    private static final int PANEL      = 0xFF2E2E33;
    private static final int PANEL_EDGE = 0xFF1A1A1E;
    private static final int SLOT_BG    = 0xFF8B8B8B;
    private static final int WHITE      = 0xFFFFFFFF;
    private static final int YELLOW     = 0xFFFFE860;
    private static final int GREEN      = 0xFF6FE36F;
    private static final int CYAN       = 0xFF6FE3E3;

    private int lastSignature = Integer.MIN_VALUE;

    public AdminShopScreen(AdminShopMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 224);
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // Verwijder-knoppen onder gevulde display-slots
        for (int i = 0; i < AdminShopMenu.DISPLAY_COUNT; i++) {
            if (this.menu.getSlot(i).hasItem()) {
                final int btnId = AdminShopMenu.BTN_REMOVE_BASE + i;
                this.addRenderableWidget(Button.builder(Component.literal("✖"),
                                b -> sendButton(btnId))
                        .bounds(x + 8 + i * 18, y + 47, 16, 13).build());
            }
        }

        // Paginanavigatie
        this.addRenderableWidget(Button.builder(Component.literal("←"),
                        b -> sendButton(AdminShopMenu.BTN_PREV))
                .bounds(x + 120, y + 4, 16, 12).build());
        this.addRenderableWidget(Button.builder(Component.literal("→"),
                        b -> sendButton(AdminShopMenu.BTN_NEXT))
                .bounds(x + 156, y + 4, 16, 12).build());

        // Toevoegen-knop
        this.addRenderableWidget(Button.builder(Component.literal("Toevoegen →"),
                        b -> sendButton(AdminShopMenu.BTN_ADD))
                .bounds(x + 30, y + 74, 110, 18).build());

        // Munt-knoppen
        this.addRenderableWidget(Button.builder(Component.literal("Koper"),
                        b -> sendButton(AdminShopMenu.BTN_ADD_COPPER))
                .bounds(x + 8, y + 116, 40, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Zilver"),
                        b -> sendButton(AdminShopMenu.BTN_ADD_SILVER))
                .bounds(x + 50, y + 116, 40, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Goud"),
                        b -> sendButton(AdminShopMenu.BTN_ADD_GOLD))
                .bounds(x + 92, y + 116, 40, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Reset"),
                        b -> sendButton(AdminShopMenu.BTN_RESET))
                .bounds(x + 134, y + 116, 34, 18).build());

        // Mode-knop
        this.addRenderableWidget(Button.builder(Component.literal("Wissel mode"),
                        b -> sendButton(AdminShopMenu.BTN_TOGGLE_MODE))
                .bounds(x + 8, y + 136, 160, 16).build());

        lastSignature = signature();
    }

    private int signature() {
        int sig = this.menu.getPage() * 31 + (this.menu.isBuyMode() ? 1000 : 0);
        for (int i = 0; i < AdminShopMenu.DISPLAY_COUNT; i++) {
            sig = sig * 31 + (this.menu.getSlot(i).hasItem() ? 1 : 0);
        }
        return sig;
    }

    private void sendButton(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (signature() != lastSignature) this.rebuildWidgets();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ex, int mouseX, int mouseY, float partial) {
        super.extractBackground(ex, mouseX, mouseY, partial);
        int x = this.leftPos;
        int y = this.topPos;

        ex.fill(x - 2, y - 2, x + this.imageWidth + 2, y + this.imageHeight + 2, PANEL_EDGE);
        ex.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL);

        for (int i = 0; i < AdminShopMenu.TOTAL_SLOTS; i++) {
            var s = this.menu.getSlot(i);
            ex.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, SLOT_BG);
        }

        ex.text(this.font, "Shop beheer", x + 8, y + 6, YELLOW, true);
        ex.text(this.font, (this.menu.getPage() + 1) + "/" + this.menu.getTotalPages(), x + 138, y + 7, WHITE, true);
        ex.text(this.font, "Artikelen (✖ = verwijder)", x + 8, y + 18, WHITE, true);
        ex.text(this.font, "Nieuw — sjabloon komt terug", x + 8, y + 63, WHITE, true);
        ex.text(this.font, "Prijs: " + WalletData.formatBalance(this.menu.getPendingPrice()), x + 8, y + 95, YELLOW, true);
        ex.text(this.font, "munt:  K=1   Z=9   G=81", x + 8, y + 106, WHITE, true);

        boolean buy = this.menu.isBuyMode();
        int modeColor = buy ? CYAN : GREEN;
        String modeLabel = buy ? "Mode: §bWINKEL KOOPT van speler" : "Mode: §aWINKEL VERKOOPT aan speler";
        ex.text(this.font, modeLabel, x + 8, y + 157, modeColor, true);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ex, int mouseX, int mouseY) {}
}
