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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runs only under {@code -Pwith_nep=<nep jar>}, which swaps the enabled GameTest namespace to this holder's. */
@GameTestHolder(NepInstalledGameTest.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class NepInstalledGameTest {

    static final String NAMESPACE = Ae2ImportCard.MOD_ID + "_nep";

    private static final String TEMPLATE = "empty_5x5x5";
    private static final String BATCH = "ae2importcard_nep_installed";

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 2);

    private NepInstalledGameTest() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void thisModsCardIdResolvesToNepsCard(GameTestHelper helper) {
        var item = BuiltInRegistries.ITEM.get(Ae2ImportCard.id("import_card"));
        helper.assertTrue(
                ModItems.NEP_CARD_ID.equals(BuiltInRegistries.ITEM.getKey(item)),
                "ae2importcard:import_card resolved to " + BuiltInRegistries.ITEM.getKey(item)
                        + " instead of nep's card, so cards from a world without nep are lost");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
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

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void thisModsRecipeStaysUnloaded(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel()
                        .getRecipeManager()
                        .byKey(Ae2ImportCard.id("import_card"))
                        .isEmpty(),
                "this mod's recipe loaded beside nep's, which shows two recipes for the same card");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void nepsCardStillInstallsInAProvider(GameTestHelper helper) {
        helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());
        var be = helper.getBlockEntity(PROVIDER);
        IUpgradeInventory upgrades;
        try {
            upgrades = (IUpgradeInventory)
                    be.getClass().getMethod("nepImportUpgrades").invoke(be);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("nep's provider upgrade host is missing: " + e, e);
        }
        var card = BuiltInRegistries.ITEM.get(ModItems.NEP_CARD_ID);
        helper.assertTrue(upgrades.addItems(new ItemStack(card)).isEmpty(), "nep's provider refused nep's card");
        helper.assertTrue(upgrades.isInstalled(card), "nep's card did not register as installed");
        helper.succeed();
    }
}
