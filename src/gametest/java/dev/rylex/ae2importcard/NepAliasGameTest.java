package dev.rylex.ae2importcard;

import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.core.definitions.AEBlocks;
import dev.rylex.ae2importcard.provider.ImportCardUpgrades;
import dev.rylex.ae2importcard.provider.ImportUpgradeHost;
import dev.rylex.ae2importcard.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public final class NepAliasGameTest {

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 2);

    private NepAliasGameTest() {}

    public static void register(ModGameTests.Batch batch) {
        batch.add("neps_card_id_resolves_to_this_mods_card", NepAliasGameTest::nepsCardIdResolvesToThisModsCard)
                .add("a_provider_saved_by_nep_loads_its_card", NepAliasGameTest::aProviderSavedByNepLoadsItsCard);
    }

    public static void nepsCardIdResolvesToThisModsCard(GameTestHelper helper) {
        helper.assertTrue(
                BuiltInRegistries.ITEM.getValue(ModItems.NEP_CARD_ID) == ModItems.IMPORT_CARD.get(),
                "nep:import_card does not resolve to this mod's card, so a world that drops nep loses every card");
        helper.succeed();
    }

    public static void aProviderSavedByNepLoadsItsCard(GameTestHelper helper) {
        helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());
        PatternProviderBlockEntity be = helper.getBlockEntity(PROVIDER, PatternProviderBlockEntity.class);
        helper.assertTrue(be instanceof ImportUpgradeHost, "the pattern provider host mixin did not apply");
        var upgrades = ((ImportUpgradeHost) be).ae2importcardUpgrades();
        helper.assertTrue(
                upgrades.addItems(new ItemStack(ModItems.IMPORT_CARD.get())).isEmpty(),
                "the provider refused the card");

        var registries = helper.getLevel().registryAccess();
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        be.saveWithFullMetadata(output);
        CompoundTag saved = output.buildResult();
        helper.assertTrue(
                saved.contains(ImportCardUpgrades.UPGRADES_KEY), "the card was not saved under nep's upgrade key");
        int renamed = renameCardIds(saved.get(ImportCardUpgrades.UPGRADES_KEY));
        helper.assertTrue(renamed > 0, "the saved upgrade inventory never named the card's id");

        upgrades.clear();
        be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));

        helper.assertTrue(
                upgrades.isInstalled(ModItems.IMPORT_CARD.get()),
                "a provider saved with nep's card came back without one; the shared NBT key or the alias broke");
        helper.succeed();
    }

    private static int renameCardIds(Tag tag) {
        int renamed = 0;
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.keySet()) {
                Tag child = compound.get(key);
                if (child instanceof StringTag(String value) && value.equals(ModItems.CARD_ID.toString())) {
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
