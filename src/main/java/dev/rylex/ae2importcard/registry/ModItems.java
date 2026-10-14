package dev.rylex.ae2importcard.registry;

import appeng.api.upgrades.Upgrades;
import dev.rylex.ae2importcard.Ae2ImportCard;
import dev.rylex.ae2importcard.NepPresence;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final Identifier CARD_ID = Ae2ImportCard.id("import_card");
    public static final Identifier NEP_CARD_ID = Identifier.fromNamespaceAndPath(NepPresence.MOD_ID, CARD_ID.getPath());

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ae2ImportCard.MOD_ID);

    public static final DeferredItem<Item> IMPORT_CARD =
            ITEMS.registerItem(CARD_ID.getPath(), Upgrades::createUpgradeCardItem);

    private ModItems() {}
}
