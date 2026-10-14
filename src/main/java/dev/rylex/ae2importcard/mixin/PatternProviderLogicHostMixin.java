package dev.rylex.ae2importcard.mixin;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import dev.rylex.ae2importcard.provider.ImportUpgradeHost;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PatternProviderLogicHost.class)
public interface PatternProviderLogicHostMixin extends ImportUpgradeHost {

    @Override
    default IUpgradeInventory ae2importcardUpgrades() {
        return ((PatternProviderLogicHost) this).getLogic() instanceof ImportUpgradeHost upgradeHost
                ? upgradeHost.ae2importcardUpgrades()
                : UpgradeInventories.empty();
    }
}
