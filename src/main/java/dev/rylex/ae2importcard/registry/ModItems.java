package dev.rylex.ae2importcard.registry;

import appeng.api.upgrades.Upgrades;
import dev.rylex.ae2importcard.Ae2ImportCard;
import dev.rylex.ae2importcard.NepPresence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final ResourceLocation CARD_ID = Ae2ImportCard.id("import_card");
    public static final ResourceLocation NEP_CARD_ID =
            ResourceLocation.fromNamespaceAndPath(NepPresence.MOD_ID, CARD_ID.getPath());

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ae2ImportCard.MOD_ID);

    public static final DeferredItem<Item> IMPORT_CARD =
            ITEMS.registerItem(CARD_ID.getPath(), Upgrades::createUpgradeCardItem, new Item.Properties());

    private ModItems() {}
}
