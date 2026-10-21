package com.invasion.client;

import com.invasion.InvasionMod;
import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.menu.NexusMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Port of the original Nexus GUI (nexusgui.png). */
public class NexusScreen extends AbstractContainerScreen<NexusMenu> {
    private static final ResourceLocation BACKGROUND = InvasionMod.id("textures/gui/nexus.png");
    private static final int TEXT = 0x404040;

    public NexusScreen(NexusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    private final Button[] upgradeButtons = new Button[3];

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < upgradeButtons.length; i++) {
            int id = i;
            upgradeButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                }
            }).bounds(leftPos + imageWidth + 4, topPos + 18 + i * 24, 110, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.invasion.role.button"), b -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, NexusMenu.ROLE_BUTTON);
            }
        }).bounds(leftPos + imageWidth + 4, topPos + 18 + 3 * 24, 110, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.invasion.role.button.desc"))).build());
        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        for (int i = 0; i < upgradeButtons.length; i++) {
            Button button = upgradeButtons[i];
            if (button == null) {
                continue;
            }
            int level = menu.getUpgradeLevel(i);
            boolean max = level >= NexusBlockEntity.UPGRADE_MAX[i];
            button.setMessage(Component.translatable("gui.invasion.upgrade.button", Component.translatable("gui.invasion.upgrade." + i),
                    level, max ? Component.translatable("gui.invasion.upgrade.max") : Component.literal(String.valueOf(
                            NexusBlockEntity.upgradeCost(i, level)))));
            button.active = !max;
            button.setTooltip(Tooltip.create(Component.translatable("gui.invasion.upgrade." + i + ".desc")));
        }
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
        g.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight);

        int l = menu.getGeneration() * 26 / NexusBlockEntity.GENERATION_PER_FLUX;
        g.blit(BACKGROUND, x + 126, y + 28 + 26 - l, 185, 26 - l, 9, l);
        l = menu.getCookTime() * 18 / NexusBlockEntity.COOK_TIME;
        g.blit(BACKGROUND, x + 31, y + 51, 204, 0, l, 2);

        int mode = menu.getMode();
        if (mode == 1 || mode == 3 || mode == 5) {
            g.blit(BACKGROUND, x + 19, y + 29, 176, 0, 9, 31);
            g.blit(BACKGROUND, x + 19, y + 19, 194, 0, 9, 9);
        } else if (mode == 2) {
            g.blit(BACKGROUND, x + 19, y + 29, 176, 31, 9, 31);
        }
        if ((mode == 0 || mode == 2) && menu.isActivating()) {
            l = menu.getActivationTimer() * 31 / NexusBlockEntity.ACTIVATION_TIME;
            g.blit(BACKGROUND, x + 19, y + 29 + 31 - l, 176, 31 - l, 9, l);
        } else if (mode == 4 && menu.isActivating()) {
            l = menu.getActivationTimer() * 31 / NexusBlockEntity.ACTIVATION_TIME;
            g.blit(BACKGROUND, x + 19, y + 29 + 31 - l, 176, 62 - l, 9, l);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, Component.translatable("gui.invasion.nexus.title", menu.getNexusLevel()), 46, 6, TEXT, false);
        g.drawString(font, Component.translatable("gui.invasion.nexus.hp", menu.getHp(), menu.getMaxHp()), imageWidth + 4, 6, 0xFFFFFF, true);
        g.drawString(font, Component.translatable("gui.invasion.nexus.kills", menu.getKills()), 96, 60, TEXT, false);
        g.drawString(font, Component.translatable("gui.invasion.nexus.radius", menu.getSpawnRadius()), 142, 72, TEXT, false);
        int mode = menu.getMode();
        if (mode == 1 || mode == 3) {
            g.drawString(font, Component.translatable("gui.invasion.nexus.activated"), 13, 62, TEXT, false);
            g.drawString(font, Component.translatable("gui.invasion.nexus.wave", menu.getCurrentWave()), 55, 37, TEXT, false);
        } else if (mode == 5) {
            g.drawString(font, Component.translatable("gui.invasion.nexus.boss_rush"), 13, 62, TEXT, false);
            g.drawString(font, Component.translatable("gui.invasion.nexus.round", menu.getCurrentWave(), menu.getTotalRounds()), 50, 37, TEXT, false);
        } else if (mode == 2) {
            g.drawString(font, Component.translatable("gui.invasion.nexus.power"), 56, 31, TEXT, false);
            g.drawString(font, String.valueOf(menu.getPowerLevel()), 61, 44, TEXT, false);
        }
        if (menu.isActivating() && mode == 0) {
            g.drawString(font, Component.translatable("gui.invasion.nexus.activating"), 13, 62, TEXT, false);
            g.drawString(font, Component.translatable("gui.invasion.nexus.are_you_sure"), 8, 72, TEXT, false);
        } else if (menu.isActivating() && mode == 4) {
            g.drawString(font, Component.translatable("gui.invasion.nexus.activating"), 13, 62, TEXT, false);
        }
    }
}
