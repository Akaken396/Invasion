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

public class NexusMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 26;
    public static final int INPUT_Y = 24;
    public static final int OUTPUT_X = 26;
    public static final int OUTPUT_Y = 66;
    public static final int INVENTORY_Y = 104;

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

        addSlot(new SlotItemHandler(items, NexusBlockEntity.SLOT_INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return NexusBlockEntity.isInputItem(stack);
            }
        });
        addSlot(new SlotItemHandler(items, NexusBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    public int get(int index) {
        return data.get(index);
    }

    public boolean isActive() {
        return data.get(0) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int playerStart = 2;
        int playerEnd = slots.size();
        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (NexusBlockEntity.isInputItem(stack)) {
            if (!moveItemStackTo(stack, NexusBlockEntity.SLOT_INPUT, NexusBlockEntity.SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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
