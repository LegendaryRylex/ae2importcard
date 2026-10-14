package dev.rylex.ae2importcard.provider;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.MachineUpgradesChanged;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.upgrades.Upgrades;
import appeng.core.localization.Tooltips;
import dev.rylex.ae2importcard.registry.ModItems;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

public final class ImportCardUpgrades {

    /** Shared with nep so a provider keeps its card when one mod replaces the other. */
    public static final String UPGRADES_KEY = "nepUpgrades";

    private static volatile List<Item> providers = List.of();
    private static volatile List<Component> supportedBy;

    private ImportCardUpgrades() {}

    /**
     * Keyed to the card rather than a pattern provider, because other addons key their own provider upgrade
     * inventories to the provider item and AE2 lets those accept every card registered against it.
     */
    public static IUpgradeInventory create(MachineUpgradesChanged onChanged) {
        return UpgradeInventories.forMachine(ModItems.IMPORT_CARD.get(), 1, onChanged);
    }

    public static void register(List<Item> providers) {
        Upgrades.add(ModItems.IMPORT_CARD, ModItems.IMPORT_CARD, 1);
        ImportCardUpgrades.providers = List.copyOf(providers);
        supportedBy = null;
    }

    public static List<Component> supportedBy() {
        var lines = supportedBy;
        if (lines == null) {
            lines = providers.stream()
                    .map(Item::getDescription)
                    .distinct()
                    .map(n -> (Component) n.copy().withStyle(Tooltips.NORMAL_TOOLTIP_TEXT))
                    .toList();
            supportedBy = lines;
        }
        return lines;
    }
}
