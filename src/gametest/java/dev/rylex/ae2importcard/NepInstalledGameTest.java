package dev.rylex.ae2importcard;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEBlocks;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.AEBaseMenu;
import dev.rylex.ae2importcard.provider.ImportUpgradeHost;
import dev.rylex.ae2importcard.registry.ModItems;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

public final class NepInstalledGameTest {

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 2);

    private NepInstalledGameTest() {}

    public static void register(ModGameTests.Batch batch) {
        batch.add("this_mods_card_id_resolves_to_neps_card", NepInstalledGameTest::thisModsCardIdResolvesToNepsCard)
                .add("none_of_this_mods_mixins_apply", NepInstalledGameTest::noneOfThisModsMixinsApply)
                .add("this_mods_recipe_stays_unloaded", NepInstalledGameTest::thisModsRecipeStaysUnloaded)
                .add("neps_card_still_installs_in_a_provider", NepInstalledGameTest::nepsCardStillInstallsInAProvider);
    }

    public static void thisModsCardIdResolvesToNepsCard(GameTestHelper helper) {
        var item = BuiltInRegistries.ITEM.getValue(Ae2ImportCard.id("import_card"));
        helper.assertTrue(
                ModItems.NEP_CARD_ID.equals(BuiltInRegistries.ITEM.getKey(item)),
                "ae2importcard:import_card resolved to " + BuiltInRegistries.ITEM.getKey(item)
                        + " instead of nep's card, so cards from a world without nep are lost");
        helper.succeed();
    }

    public static void noneOfThisModsMixinsApply(GameTestHelper helper) {
        helper.assertTrue(
                !ImportUpgradeHost.class.isAssignableFrom(PatternProviderLogic.class),
                "this mod's provider mixin applied alongside nep's, so a provider carries two card slots");
        var menuMethods = AEBaseMenu.class.getDeclaredMethods();
        helper.assertTrue(
                menuMethods.length > 0
                        && Arrays.stream(menuMethods).noneMatch(m -> m.getName().contains("ae2importcard$")),
                "this mod's menu mixins applied alongside nep's");
        helper.succeed();
    }

    public static void thisModsRecipeStaysUnloaded(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel()
                        .recipeAccess()
                        .byKey(ResourceKey.create(Registries.RECIPE, Ae2ImportCard.id("import_card")))
                        .isEmpty(),
                "this mod's recipe loaded beside nep's, which shows two recipes for the same card");
        helper.succeed();
    }

    public static void nepsCardStillInstallsInAProvider(GameTestHelper helper) {
        helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());
        var be = helper.getLevel().getBlockEntity(helper.absolutePos(PROVIDER));
        IUpgradeInventory upgrades;
        try {
            upgrades = (IUpgradeInventory)
                    be.getClass().getMethod("nepImportUpgrades").invoke(be);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("nep's provider upgrade host is missing: " + e, e);
        }
        var card = BuiltInRegistries.ITEM.getValue(ModItems.NEP_CARD_ID);
        helper.assertTrue(upgrades.addItems(new ItemStack(card)).isEmpty(), "nep's provider refused nep's card");
        helper.assertTrue(upgrades.isInstalled(card), "nep's card did not register as installed");
        helper.succeed();
    }
}
