package dev.rylex.ae2importcard;

import net.neoforged.fml.loading.LoadingModList;

/**
 * nep ships the same card; when it is installed this mod registers nothing, applies no mixins, and aliases its own
 * card id onto nep's, so either mod reads the other's saved cards and in-flight imports.
 */
public final class NepPresence {

    public static final String MOD_ID = "nep";

    private NepPresence() {}

    public static boolean isLoaded() {
        return LoadingModList.get().getModFileById(MOD_ID) != null;
    }
}
