package dev.rylex.ae2importcard;

import appeng.core.definitions.AEBlocks;
import dev.rylex.ae2importcard.provider.ImportCardUpgrades;
import dev.rylex.ae2importcard.provider.ImportUpgradeHost;
import dev.rylex.ae2importcard.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Ae2ImportCard.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NepAliasGameTest {

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "ae2importcard_nep_alias";

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 2);

    private NepAliasGameTest() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void nepsCardIdResolvesToThisModsCard(GameTestHelper helper) {
        helper.assertTrue(
                BuiltInRegistries.ITEM.get(ModItems.NEP_CARD_ID) == ModItems.IMPORT_CARD.get(),
                "nep:import_card does not resolve to this mod's card, so a world that drops nep loses every card");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void aProviderSavedByNepLoadsItsCard(GameTestHelper helper) {
        helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());
        BlockEntity be = helper.getBlockEntity(PROVIDER);
        helper.assertTrue(be instanceof ImportUpgradeHost, "the pattern provider host mixin did not apply");
        var upgrades = ((ImportUpgradeHost) be).ae2importcardUpgrades();
        helper.assertTrue(
                upgrades.addItems(new ItemStack(ModItems.IMPORT_CARD.get())).isEmpty(),
                "the provider refused the card");

        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = be.saveWithoutMetadata(registries);
        helper.assertTrue(
                saved.contains(ImportCardUpgrades.UPGRADES_KEY), "the card was not saved under nep's upgrade key");
        int renamed = renameCardIds(saved.get(ImportCardUpgrades.UPGRADES_KEY));
        helper.assertTrue(renamed > 0, "the saved upgrade inventory never named the card's id");

        upgrades.clear();
        be.loadWithComponents(saved, registries);

        helper.assertTrue(
                upgrades.isInstalled(ModItems.IMPORT_CARD.get()),
                "a provider saved with nep's card came back without one; the shared NBT key or the alias broke");
        helper.succeed();
    }

    private static int renameCardIds(Tag tag) {
        int renamed = 0;
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                Tag child = compound.get(key);
                if (child instanceof StringTag string && string.getAsString().equals(ModItems.CARD_ID.toString())) {
                    compound.putString(key, ModItems.NEP_CARD_ID.toString());
                    renamed++;
                } else {
                    renamed += renameCardIds(child);
                }
            }
        } else if (tag instanceof ListTag list) {
            for (Tag child : list) {
                renamed += renameCardIds(child);
            }
        }
        return renamed;
    }
}
