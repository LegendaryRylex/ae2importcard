package dev.rylex.ae2importcard.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ImportCardConfig {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue GRACE;

    static {
        var builder = new ModConfigSpec.Builder();
        GRACE = builder.comment(
                        "Ticks an Import Card keeps collecting a result the network has stopped waiting for. This is what returns items already sent to a machine when a job is cancelled partway through.",
                        "The clock restarts every time the card collects something, so a run of results still in flight all come back. This grace period only runs once a job ends.",
                        "Set to 0 to give a cancelled job a single collection attempt and nothing more.")
                .translation("ae2importcard.configuration.importCardGrace")
                .defineInRange("importCardGrace", 200, 0, 24_000);
        SPEC = builder.build();
    }

    private ImportCardConfig() {}

    public static int importCardGrace() {
        return SPEC.isLoaded() ? GRACE.get() : GRACE.getDefault();
    }
}
