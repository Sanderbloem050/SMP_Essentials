package com.sanderbloem.currencymod.client;

import com.sanderbloem.currencymod.data.WalletData;
import com.sanderbloem.currencymod.menu.BountyBoardMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BountyBoardScreen extends AbstractContainerScreen<BountyBoardMenu> {

    private static final int PANEL = 0xFF2C241F;
    private static final int PANEL_EDGE = 0xFF171210;
    private static final int SLOT = 0x805F4B3A;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFFFD662;
    private static final int RED = 0xFFFF7A7A;
    private static final int SOFT = 0xFFD5C3AA;
    private static final int GREEN = 0xFF83E28A;

    public BountyBoardScreen(BountyBoardMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 212, 182);
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        this.addRenderableWidget(Button.builder(Component.literal("<"),
                        b -> send(BountyBoardMenu.BTN_PREV_TARGET))
                .bounds(x + 8, y + 138, 20, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"),
                        b -> send(BountyBoardMenu.BTN_NEXT_TARGET))
                .bounds(x + 184, y + 138, 20, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("+1c"),
                        b -> send(BountyBoardMenu.BTN_ADD_COPPER))
                .bounds(x + 8, y + 159, 46, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("+1s"),
                        b -> send(BountyBoardMenu.BTN_ADD_SILVER))
                .bounds(x + 58, y + 159, 46, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("+1g"),
                        b -> send(BountyBoardMenu.BTN_ADD_GOLD))
                .bounds(x + 108, y + 159, 46, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Ververs"),
                        b -> send(BountyBoardMenu.BTN_REFRESH))
                .bounds(x + 158, y + 159, 46, 18).build());
    }

    private void send(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ex, int mouseX, int mouseY, float partial) {
        super.extractBackground(ex, mouseX, mouseY, partial);
        int x = this.leftPos;
        int y = this.topPos;

        ex.fill(x - 2, y - 2, x + this.imageWidth + 2, y + this.imageHeight + 2, PANEL_EDGE);
        ex.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL);

        ex.fill(x + 8, y + 20, x + 204, y + 114, SLOT);
        ex.fill(x + 8, y + 120, x + 204, y + 178, SLOT);

        ex.text(this.font, "Bounty Board", x + 8, y + 6, GOLD, true);
        ex.text(this.font, "Jouw saldo: " + balanceString(), x + 8, y + 118, SOFT, true);
        ex.text(this.font, "Open bounties", x + 8, y + 22, WHITE, true);

        for (int i = 0; i < 5; i++) {
            int rowY = y + 36 + i * 14;
            String name = this.menu.getEntryName(i);
            String amount = this.menu.hasEntry(i) ? WalletData.formatBalance(this.menu.getEntryAmount(i)) : "-";
            ex.text(this.font, (i + 1) + ". " + (name.isBlank() ? "Geen bounty" : name), x + 12, rowY, RED, false);
            ex.text(this.font, amount, x + 144, rowY, GOLD, false);
        }

        ex.text(this.font, "Nieuw doelwit", x + 8, y + 134, WHITE, true);
        if (this.menu.hasTarget()) {
            ex.text(this.font, this.menu.getTargetName(), x + 36, y + 142, GREEN, true);
            ex.text(this.font, "Huidige bounty: " + WalletData.formatBalance(this.menu.getTargetBounty()),
                    x + 36, y + 152, SOFT, false);
        } else {
            ex.text(this.font, "Geen andere spelers online", x + 36, y + 146, SOFT, false);
        }
    }

    private String balanceString() {
        return this.menu.getGold() + "g " + this.menu.getSilver() + "s " + this.menu.getCopper() + "c";
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ex, int mouseX, int mouseY) {
    }
}
