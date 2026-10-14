package dev.rylex.ae2importcard.compat.draconic;

import dev.rylex.ae2importcard.provider.MachineImports;

public final class DraconicCompat {

    private DraconicCompat() {}

    public static void init() {
        MachineImports.register(MachineImports.DRACONIC_FUSION_CORE, FusionCoreImport::create);
    }
}
