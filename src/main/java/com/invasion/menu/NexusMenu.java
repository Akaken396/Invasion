package com.invasion.menu;

import com.invasion.blockentity.NexusBlockEntity;
import com.invasion.registry.ModBlocks;
import com.invasion.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/** Nexus container with the slot layout of the original GUI. */
public class NexusMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side constructor. */
    public NexusMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, new ItemStackHandler(2), new SimpleContainerData(NexusBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL);
    }

    /** Server side constructor. */
    public NexusMenu(int id, Inventory inventory, NexusBlockEntity nexus, ContainerData data) {
        this(id, inventory, nexus.getItems(), data, ContainerLevelAccess.create(nexus.getLevel(), nexus.getBlockPos()));
    }

    private NexusMenu(int id, Inventory inventory, IItemHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.NEXUS.get(), id);
        this.data = data;
        this.access = access;
        checkContainerDataCount(data, NexusBlockEntity.DATA_COUNT);

        addSlot(new SlotItemHandler(items, NexusBlockEntity.SLOT_INPUT, 32, 33));
        addSlot(new SlotItemHandler(items, NexusBlockEntity.SLOT_OUTPUT, 102, 33) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public int getMode() {
        return data.get(0);
    }

    public int getActivationTimer() {
        return data.get(1);
    }

    public int getNexusLevel() {
        return data.get(2);
    }

    public int getKills() {
        return data.get(3);
    }

    public int getSpawnRadius() {
        return data.get(4);
    }

    public int getCurrentWave() {
        return data.get(5);
    }

    public int getPowerLevel() {
        return data.get(6);
    }

    public int getGeneration() {
        return data.get(7);
    }

    public int getCookTime() {
        return data.get(8);
    }

    public boolean isActivating() {
        int timer = getActivationTimer();
        return timer > 0 && timer < NexusBlockEntity.ACTIVATION_TIME;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, NexusBlockEntity.SLOT_INPUT, NexusBlockEntity.SLOT_INPUT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.NEXUS.get());
    }
}
