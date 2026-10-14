package dev.rylex.ae2importcard.provider;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.RestrictedInputSlot;
import dev.rylex.ae2importcard.registry.ModItems;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ImportCardSlot extends RestrictedInputSlot {

    public ImportCardSlot(InternalInventory inv, int invSlot) {
        super(PlacableItemType.UPGRADES, inv, invSlot);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(ModItems.IMPORT_CARD.get()) && super.mayPlace(stack);
    }

    public static boolean isEmptyImportSlot(Slot slot) {
        return slot instanceof ImportCardSlot && !slot.hasItem();
    }
}
