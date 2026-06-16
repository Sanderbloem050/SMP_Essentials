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

    private static final int PANEL = 0xFF2E2E33;   // donker paneel
    private static final int PANEL_EDGE = 0xFF1A1A1E;
    private static final int SLOT_BG = 0xFF8B8B8B;  // lichte vakjes zodat items opvallen
    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFE860;

    private int lastTradeCount = -1;

    public AdminShopScreen(AdminShopMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 224);
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // Verwijder-knoppen (✖) onder gevulde artikel-slots
        for (int i = 0; i < AdminShopMenu.DISPLAY_COUNT; i++) {
            if (this.menu.getSlot(i).hasItem()) {
                final int btnId = AdminShopMenu.BTN_REMOVE_BASE + i;
                this.addRenderableWidget(Button.builder(Component.literal("✖"),
                                b -> sendButton(btnId))
                        .bounds(x + 8 + i * 18, y + 47, 16, 13).build());
            }
        }

        // Toevoegen-knop naast het sjabloon-slot
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

        lastTradeCount = countTrades();
    }

    private int countTrades() {
        int n = 0;
        for (int i = 0; i < AdminShopMenu.DISPLAY_COUNT; i++) {
            if (this.menu.getSlot(i).hasItem()) n++;
        }
        return n;
    }

    private void sendButton(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (countTrades() != lastTradeCount) {
            this.rebuildWidgets();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ex, int mouseX, int mouseY, float partial) {
        super.extractBackground(ex, mouseX, mouseY, partial);
        int x = this.leftPos;
        int y = this.topPos;

        // Donker paneel met rand
        ex.fill(x - 2, y - 2, x + this.imageWidth + 2, y + this.imageHeight + 2, PANEL_EDGE);
        ex.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL);

        for (int i = 0; i < AdminShopMenu.TOTAL_SLOTS; i++) {
            var s = this.menu.getSlot(i);
            ex.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, SLOT_BG);
        }

        // Witte tekst met schaduw, gele koppen
        ex.text(this.font, "Shop beheer", x + 8, y + 6, YELLOW, true);
        ex.text(this.font, "Artikelen (✖ = verwijder)", x + 8, y + 18, WHITE, true);
        ex.text(this.font, "Nieuw — sjabloon komt terug", x + 8, y + 63, WHITE, true);
        ex.text(this.font, "Prijs: " + WalletData.formatBalance(this.menu.getPendingPrice()),
                x + 8, y + 95, YELLOW, true);
        ex.text(this.font, "munt:  K=1   Z=9   G=81", x + 8, y + 106, WHITE, true);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ex, int mouseX, int mouseY) {
        // eigen labels in extractBackground
    }
}
