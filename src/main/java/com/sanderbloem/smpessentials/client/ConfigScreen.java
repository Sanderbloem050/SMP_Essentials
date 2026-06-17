package com.sanderbloem.smpessentials.client;

import com.sanderbloem.smpessentials.menu.ConfigMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.function.BooleanSupplier;

public class ConfigScreen extends AbstractContainerScreen<ConfigMenu> {

    private static final int PANEL = 0xFF2E2E33;
    private static final int PANEL_EDGE = 0xFF1A1A1E;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFE860;
    private static final int GREEN = 0xFF6FE36F;
    private static final int RED = 0xFFE36F6F;

    private record Row(String label, int btnId, BooleanSupplier state) {}

    private Row[] rows;

    public ConfigScreen(ConfigMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 150);
    }

    @Override
    protected void init() {
        super.init();
        rows = new Row[] {
                new Row("Jobs",          ConfigMenu.BTN_JOBS,       menu::isJobs),
                new Row("Claims",        ConfigMenu.BTN_CLAIMS,     menu::isClaims),
                new Row("Death Chest",   ConfigMenu.BTN_DEATHCHEST, menu::isDeathChest),
                new Row("Loot Crates",   ConfigMenu.BTN_CRATES,     menu::isCrates),
                new Row("Bounties",      ConfigMenu.BTN_BOUNTIES,   menu::isBounties),
                new Row("Veilinghuis",   ConfigMenu.BTN_AUCTION,    menu::isAuction),
        };
        int x = this.leftPos, y = this.topPos;
        for (int i = 0; i < rows.length; i++) {
            int btnId = rows[i].btnId();
            this.addRenderableWidget(Button.builder(Component.literal("Wissel"),
                            b -> send(btnId))
                    .bounds(x + 120, y + 22 + i * 18, 48, 16).build());
        }
    }

    private void send(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ex, int mouseX, int mouseY, float partial) {
        super.extractBackground(ex, mouseX, mouseY, partial);
        int x = this.leftPos, y = this.topPos;

        ex.fill(x - 2, y - 2, x + this.imageWidth + 2, y + this.imageHeight + 2, PANEL_EDGE);
        ex.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL);

        ex.text(this.font, "Server instellingen", x + 8, y + 6, YELLOW, true);

        if (rows != null) {
            for (int i = 0; i < rows.length; i++) {
                boolean on = rows[i].state().getAsBoolean();
                int ty = y + 24 + i * 18;
                ex.text(this.font, rows[i].label(), x + 8, ty, WHITE, true);
                ex.text(this.font, on ? "AAN" : "UIT", x + 88, ty, on ? GREEN : RED, true);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ex, int mouseX, int mouseY) {
    }
}
