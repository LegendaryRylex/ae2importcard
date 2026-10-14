package dev.rylex.ae2importcard.mixin;

import appeng.api.upgrades.Upgrades;
import dev.rylex.ae2importcard.provider.ImportCardUpgrades;
import dev.rylex.ae2importcard.registry.ModItems;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Upgrades.class)
public abstract class UpgradesMixin {

    @Inject(method = "getTooltipLinesForCard", at = @At("HEAD"), cancellable = true)
    private static void ae2importcard$importCardSupportedBy(
            ItemLike card, CallbackInfoReturnable<List<Component>> cir) {
        if (card.asItem() == ModItems.IMPORT_CARD.get()) {
            cir.setReturnValue(ImportCardUpgrades.supportedBy());
        }
    }
}
