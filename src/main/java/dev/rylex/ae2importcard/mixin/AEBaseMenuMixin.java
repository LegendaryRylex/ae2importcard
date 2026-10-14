package dev.rylex.ae2importcard.mixin;

import appeng.menu.AEBaseMenu;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.rylex.ae2importcard.provider.ImportCardSlot;
import dev.rylex.ae2importcard.provider.ImportCardSlotSemantics;
import dev.rylex.ae2importcard.registry.ModItems;
import java.util.List;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AEBaseMenu.class, priority = 3500)
public abstract class AEBaseMenuMixin {

    @ModifyReturnValue(method = "getQuickMoveDestinationSlots", at = @At("RETURN"))
    private List<Slot> ae2importcard$preferImportCardUpgradeSlot(
            List<Slot> destinations, ItemStack stackToMove, boolean fromPlayerSide) {
        if (!fromPlayerSide || !stackToMove.is(ModItems.IMPORT_CARD.get())) {
            return destinations;
        }

        var menu = (AEBaseMenu) (Object) this;
        if (menu.getSlots(ImportCardSlotSemantics.IMPORT_UPGRADE).isEmpty()) {
            return destinations;
        }

        return destinations.stream().filter(ImportCardSlot::isEmptyImportSlot).toList();
    }
}
