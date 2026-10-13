package com.invasion.client;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.menu.NexusMenu;
import com.invasion.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Interface of the Nexus. Drawn entirely with flat rectangles, so it needs no texture. */
public class NexusScreen extends AbstractContainerScreen<NexusMenu> {
    private static final int BG = 0xFFC6C6C6;
    private static final int BORDER_LIGHT = 0xFFFFFFFF;
    private static final int BORDER_DARK = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int PANEL = 0xFF2B1B3D;
    private static final int TEXT = 0x404040;

    public NexusScreen(NexusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // Panel with bevelled border.
        g.fill(x, y, x + imageWidth, y + imageHeight, BORDER_DARK);
        g.fill(x, y, x + imageWidth - 1, y + imageHeight - 1, BORDER_LIGHT);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, BG);

        // Status panel.
        g.fill(x + 50, y + 16, x + imageWidth - 7, y + 90, PANEL);

        for (Slot slot : menu.slots) {
            drawSlot(g, x + slot.x, y + slot.y);
        }

        // Generation progress (towards the next Rift Flux) between input and output slot.
        int needed = Math.max(1, Config.KILLS_PER_RIFT_FLUX.get());
        int progress = Math.min(menu.get(8), needed);
        int barX = x + NexusMenu.INPUT_X + 6;
        int barTop = y + NexusMenu.INPUT_Y + 20;
        int barBottom = y + NexusMenu.OUTPUT_Y - 4;
        g.fill(barX, barTop, barX + 4, barBottom, 0xFF373737);
        int filled = (barBottom - barTop) * progress / needed;
        g.fill(barX, barTop, barX + 4, barTop + filled, 0xFFB04CFF);

        // Hit point bar.
        int maxHp = Math.max(1, menu.get(4));
        int hp = Math.max(0, menu.get(3));
        int hpLeft = x + 54;
        int hpRight = x + imageWidth - 11;
        int hpY = y + 78;
        g.fill(hpLeft, hpY, hpRight, hpY + 7, 0xFF000000);
        g.fill(hpLeft + 1, hpY + 1, hpLeft + 1 + (hpRight - hpLeft - 2) * hp / maxHp, hpY + 6, hpColor(hp, maxHp));
    }

    private static int hpColor(int hp, int maxHp) {
        float ratio = hp / (float) maxHp;
        if (ratio > 0.5F) {
            return 0xFF3FD23F;
        }
        return ratio > 0.25F ? 0xFFE0C030 : 0xFFE03030;
    }

    private static void drawSlot(GuiGraphics g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, BORDER_DARK);
        g.fill(sx, sy, sx + 17, sy + 17, BORDER_LIGHT);
        g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);

        int x = 54;
        int y = 19;
        int white = 0xFFFFFF;
        int purple = 0xD9A6FF;
        boolean active = menu.isActive();
        int phase = menu.get(11);

        Component status;
        if (!active) {
            status = Component.translatable("gui.invasion.nexus.inactive");
        } else if (phase == NexusBlockEntity.Phase.RESTING.ordinal()) {
            status = Component.translatable("gui.invasion.nexus.next_wave", menu.get(10));
        } else {
            status = Component.translatable("gui.invasion.nexus.under_attack");
        }
        g.drawString(font, status, x, y, active ? 0xFF7070 : purple, false);

        Component mode = Component.translatable(menu.get(1) == NexusBlockEntity.Mode.CONTINUOUS.ordinal()
                ? "gui.invasion.nexus.mode.continuous" : "gui.invasion.nexus.mode.wave");
        g.drawString(font, active ? mode : Component.translatable("gui.invasion.nexus.insert"), x, y + 11, purple, false);
        g.drawString(font, Component.translatable("gui.invasion.nexus.level_kills", menu.get(2), menu.get(5)), x, y + 22, white, false);
        if (active) {
            g.drawString(font, Component.translatable("gui.invasion.nexus.wave_remaining", menu.get(6), menu.get(7)), x, y + 33, white, false);
        } else {
            g.drawString(font, Component.translatable("gui.invasion.nexus.radius", menu.get(9)), x, y + 33, white, false);
        }
        g.drawString(font, Component.translatable("gui.invasion.nexus.hp", menu.get(3), menu.get(4)), x, y + 46, white, false);
    }
}
