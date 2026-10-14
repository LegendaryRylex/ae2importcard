package dev.rylex.ae2importcard.compat.advancedae.mixin;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import dev.rylex.ae2importcard.provider.ImportUpgradeHost;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AdvPatternProviderLogicHost.class)
public interface AdvPatternProviderLogicHostMixin extends ImportUpgradeHost {

    @Override
    default IUpgradeInventory ae2importcardUpgrades() {
        return ((AdvPatternProviderLogicHost) this).getLogic() instanceof ImportUpgradeHost upgradeHost
                ? upgradeHost.ae2importcardUpgrades()
                : UpgradeInventories.empty();
    }
}
