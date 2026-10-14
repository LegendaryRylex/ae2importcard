package dev.rylex.ae2importcard.compat.advancedae.mixin;

import dev.rylex.ae2importcard.NepPresence;
import dev.rylex.ae2importcard.mixin.ImportCardMixinPlugin;
import java.util.List;

public class AdvancedAeMixinPlugin extends ImportCardMixinPlugin {

    @Override
    public List<String> getMixins() {
        if (!loaded("advanced_ae") || NepPresence.isLoaded()) {
            return List.of();
        }
        return List.of(
                "AdvPatternProviderLogicMixin",
                "AdvPatternProviderLogicHostMixin",
                "AdvPatternProviderMenuMixin",
                "AdvPatternProviderScreenMixin",
                "SmallAdvPatternProviderScreenMixin");
    }
}
