package com.sanderbloem.currencymod.client;

import com.sanderbloem.currencymod.menu.AtmMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AtmScreen extends AbstractContainerScreen<AtmMenu> {

    private static final int PANEL = 0xFF2E2E33;
    private static final int PANEL_EDGE = 0xFF1A1A1E;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFE860;
    private static final int C_GOLD = 0xFFFFD24A;
    private static final int C_SILVER = 0xFFE2E5EC;
    private static final int C_COPPER = 0xFFE08A4A;

    public AtmScreen(AtmMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 192);
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        this.addRenderableWidget(Button.builder(Component.literal("Stort alle munten"),
                        b -> send(AtmMenu.BTN_DEPOSIT))
                .bounds(x + 8, y + 48, 160, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("1 Koper"),
                        b -> send(AtmMenu.BTN_WD_COPPER))
                .bounds(x + 8, y + 82, 50, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("1 Zilver"),
                        b -> send(AtmMenu.BTN_WD_SILVER))
                .bounds(x + 62, y + 82, 50, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("1 Goud"),
                        b -> send(AtmMenu.BTN_WD_GOLD))
                .bounds(x + 116, y + 82, 50, 18).build());
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

        for (int i = 0; i < 36; i++) {
            var s = this.menu.getSlot(i);
            ex.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, SLOT_BG);
        }

        ex.text(this.font, "Bank", x + 8, y + 6, YELLOW, true);
        ex.text(this.font, "Saldo:", x + 8, y + 20, WHITE, true);

        // Saldo per munt, met volledige woorden en kleur
        String g = this.menu.getGold() + " Goud";
        String s = this.menu.getSilver() + " Zilver";
        String c = this.menu.getCopper() + " Koper";
        int gx = x + 8;
        int gy = y + 32;
        ex.text(this.font, g, gx, gy, C_GOLD, true);
        gx += this.font.width(g) + 10;
        ex.text(this.font, s, gx, gy, C_SILVER, true);
        gx += this.font.width(s) + 10;
        ex.text(this.font, c, gx, gy, C_COPPER, true);

        ex.text(this.font, "Opnemen:", x + 8, y + 72, WHITE, true);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ex, int mouseX, int mouseY) {
    }
}
