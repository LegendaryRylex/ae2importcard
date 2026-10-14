package dev.rylex.ae2importcard.compat.draconic.mixin;

import dev.rylex.ae2importcard.NepPresence;
import dev.rylex.ae2importcard.mixin.ImportCardMixinPlugin;
import java.util.List;

public class DraconicMixinPlugin extends ImportCardMixinPlugin {

    @Override
    public List<String> getMixins() {
        if (!loaded("ae2_draconic_fusion_autocrafter") || NepPresence.isLoaded()) {
            return List.of();
        }
        return List.of("DraconicPatternProviderLogicMixin");
    }
}
