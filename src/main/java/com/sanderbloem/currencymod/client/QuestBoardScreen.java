package com.sanderbloem.currencymod.client;

import com.sanderbloem.currencymod.menu.QuestBoardMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class QuestBoardScreen extends AbstractContainerScreen<QuestBoardMenu> {

    private static final int PANEL = 0xFF2C241F;
    private static final int PANEL_EDGE = 0xFF171210;
    private static final int SLOT = 0x805F4B3A;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFFFD662;
    private static final int SOFT = 0xFFD5C3AA;

    public QuestBoardScreen(QuestBoardMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 232, 150);
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        this.addRenderableWidget(Button.builder(Component.literal("Inleveren"),
                        b -> send(QuestBoardMenu.BTN_FETCH_TURNIN))
                .bounds(x + 8, y + 40, 90, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("Claim"),
                        b -> send(QuestBoardMenu.BTN_TRAVEL_CLAIM))
                .bounds(x + 8, y + 78, 90, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("Bijdragen"),
                        b -> send(QuestBoardMenu.BTN_COMMUNITY_CONTRIBUTE))
                .bounds(x + 8, y + 116, 90, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("Claim"),
                        b -> send(QuestBoardMenu.BTN_COMMUNITY_CLAIM))
                .bounds(x + 102, y + 116, 60, 16).build());
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

        ex.fill(x + 8, y + 20, x + 224, y + 38, SLOT);
        ex.fill(x + 8, y + 58, x + 224, y + 76, SLOT);
        ex.fill(x + 8, y + 96, x + 224, y + 114, SLOT);

        ex.text(this.font, "Quest Board", x + 8, y + 6, GOLD, true);

        ex.text(this.font, this.menu.getFetchLine1(), x + 12, y + 23, WHITE, false);
        ex.text(this.font, this.menu.getFetchLine2(), x + 12, y + 31, SOFT, false);

        ex.text(this.font, this.menu.getTravelLine1(), x + 12, y + 61, WHITE, false);
        ex.text(this.font, this.menu.getTravelLine2(), x + 12, y + 69, SOFT, false);

        ex.text(this.font, this.menu.getCommunityLine1(), x + 12, y + 99, WHITE, false);
        ex.text(this.font, this.menu.getCommunityLine2(), x + 12, y + 107, SOFT, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ex, int mouseX, int mouseY) {
    }
}
