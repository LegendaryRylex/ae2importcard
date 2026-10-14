package dev.rylex.ae2importcard.compat.draconic;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.DraconicAPI;
import com.brandon3055.draconicevolution.api.crafting.IFusionInjector;
import com.brandon3055.draconicevolution.api.crafting.IFusionInventory;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.api.crafting.IFusionStateMachine;
import com.brandon3055.draconicevolution.blocks.machines.CraftingInjector;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import com.brandon3055.draconicevolution.init.DEContent;
import dev.rylex.ae2importcard.Ae2ImportCard;
import dev.rylex.ae2importcard.provider.ImportUpgradeHost;
import dev.rylex.ae2importcard.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Ae2ImportCard.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DraconicProviderImportGameTest {

    private static final String TEMPLATE = "empty_9x6x9";
    private static final String BATCH = "ae2importcard_draconic_import";

    private static final BlockPos CORE = new BlockPos(4, 2, 4);
    private static final BlockPos PROVIDER = new BlockPos(4, 1, 4);
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 4);
    private static final BlockPos ENERGY = new BlockPos(5, 1, 4);
    private static final BlockPos DRIVE = new BlockPos(4, 1, 3);

    private record InjectorSite(BlockPos pos, Direction facing) {}

    private static final List<InjectorSite> SITES = List.of(
            new InjectorSite(new BlockPos(1, 2, 4), Direction.EAST),
            new InjectorSite(new BlockPos(7, 2, 4), Direction.WEST),
            new InjectorSite(new BlockPos(4, 2, 1), Direction.SOUTH),
            new InjectorSite(new BlockPos(4, 2, 7), Direction.NORTH),
            new InjectorSite(new BlockPos(1, 3, 4), Direction.EAST),
            new InjectorSite(new BlockPos(1, 1, 4), Direction.EAST),
            new InjectorSite(new BlockPos(7, 3, 4), Direction.WEST),
            new InjectorSite(new BlockPos(7, 1, 4), Direction.WEST));

    private record CatalystOnly(ItemStack catalyst) implements IFusionInventory {
        @Override
        public ItemStack getCatalystStack() {
            return catalyst;
        }

        @Override
        public ItemStack getOutputStack() {
            return ItemStack.EMPTY;
        }

        @Override
        public void setCatalystStack(ItemStack stack) {}

        @Override
        public void setOutputStack(ItemStack stack) {}

        @Override
        public List<IFusionInjector> getInjectors() {
            return List.of();
        }

        @Override
        public TechLevel getMinimumTier() {
            return TechLevel.CHAOTIC;
        }

        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? catalyst : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    private DraconicProviderImportGameTest() {}

    @SuppressWarnings("unchecked")
    private static RecipeHolder<IFusionRecipe> wyvernPickaxe(Level level) {
        var type = (RecipeType<IFusionRecipe>) DraconicAPI.FUSION_RECIPE_TYPE.get();
        for (RecipeHolder<IFusionRecipe> holder : level.getRecipeManager().getAllRecipesFor(type)) {
            if (holder.value().getResultItem(level.registryAccess()).is(DEContent.PICKAXE_WYVERN.get())) {
                return holder;
            }
        }
        throw new AssertionError("Draconic Evolution has no fusion recipe for the wyvern pickaxe");
    }

    private static ItemStack firstOption(Ingredient ingredient) {
        ItemStack[] options = ingredient.getItems();
        if (options.length == 0) {
            throw new AssertionError("a fusion ingredient matched no items");
        }
        return options[0].copy();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 400)
    public static void aDraconicProviderCollectsWhatItsFusionCoreMade(GameTestHelper helper) {
        var level = helper.getLevel();
        IFusionRecipe recipe = wyvernPickaxe(level).value();

        ItemStack catalyst = firstOption(recipe.getCatalyst());
        List<GenericStack> inputs = new ArrayList<>();
        inputs.add(GenericStack.fromItemStack(catalyst));
        for (IFusionRecipe.IFusionIngredient ingredient : recipe.fusionIngredients()) {
            inputs.add(GenericStack.fromItemStack(firstOption(ingredient.get())));
        }
        ItemStack produced = FusionCoreImport.withoutProviderIdentity(
                recipe.assemble(new CatalystOnly(catalyst.copy()), level.registryAccess()));
        AEItemKey result = AEItemKey.of(produced);

        var provider = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(
                "ae2_draconic_fusion_autocrafter", "me_draconic_pattern_provider"));

        helper.startSequence()
                .thenExecute(() -> {
                    helper.setBlock(CONTROLLER, AEBlocks.CONTROLLER.block());
                    helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block());
                    helper.setBlock(DRIVE, AEBlocks.DRIVE.block());
                    helper.setBlock(PROVIDER, provider);
                    helper.setBlock(CORE, DEContent.CRAFTING_CORE.get().defaultBlockState());
                    for (InjectorSite site : SITES) {
                        helper.setBlock(
                                site.pos(),
                                DEContent.WYVERN_CRAFTING_INJECTOR
                                        .get()
                                        .defaultBlockState()
                                        .setValue(CraftingInjector.FACING, site.facing()));
                    }
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    DriveBlockEntity drive = helper.getBlockEntity(DRIVE);
                    drive.getInternalInventory().addItems(AEItems.ITEM_CELL_1K.stack());
                    PatternProviderBlockEntity be = helper.getBlockEntity(PROVIDER);
                    helper.assertTrue(
                            be instanceof ImportUpgradeHost, "the Draconic provider is not an import upgrade host");
                    helper.assertTrue(
                            ((ImportUpgradeHost) be)
                                    .ae2importcardUpgrades()
                                    .addItems(new ItemStack(ModItems.IMPORT_CARD.get()))
                                    .isEmpty(),
                            "the Draconic provider refused the card");
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    PatternProviderBlockEntity be = helper.getBlockEntity(PROVIDER);
                    ItemStack encoded = PatternDetailsHelper.encodeProcessingPattern(
                            inputs, List.of(new GenericStack(result, produced.getCount())));
                    IPatternDetails details = PatternDetailsHelper.decodePattern(encoded, level);
                    helper.assertTrue(details != null, "the pattern did not decode");
                    KeyCounter[] pushed = new KeyCounter[details.getInputs().length];
                    for (int slot = 0; slot < pushed.length; slot++) {
                        pushed[slot] = new KeyCounter();
                        IPatternDetails.IInput input = details.getInputs()[slot];
                        pushed[slot].add(input.getPossibleInputs()[0].what(), input.getMultiplier());
                    }
                    helper.assertTrue(be.getLogic().pushPattern(details, pushed), "the provider refused the push");
                    TileFusionCraftingCore core = helper.getBlockEntity(CORE);
                    helper.assertTrue(core.isCrafting(), "the core never started crafting");
                    core.setFusionState(IFusionStateMachine.FusionState.CRAFTING);
                    core.setCounter(Integer.MAX_VALUE - 1);
                    core.tick();
                    helper.assertTrue(!core.isCrafting(), "the core never finished");
                    helper.assertTrue(!core.getOutputStack().isEmpty(), "the core made nothing");
                })
                .thenIdle(60)
                .thenExecute(() -> {
                    TileFusionCraftingCore core = helper.getBlockEntity(CORE);
                    PatternProviderBlockEntity be = helper.getBlockEntity(PROVIDER);
                    IGrid grid = be.getMainNode().getGrid();
                    helper.assertTrue(grid != null, "the provider never joined a grid");
                    long inNetwork = grid.getStorageService()
                            .getInventory()
                            .extract(result, Long.MAX_VALUE, Actionable.SIMULATE, IActionSource.empty());
                    helper.assertTrue(
                            core.getOutputStack().isEmpty() && inNetwork == produced.getCount(),
                            "the fusion result was not imported: core holds " + core.getOutputStack()
                                    + ", network holds " + inNetwork);
                })
                .thenSucceed();
    }
}
