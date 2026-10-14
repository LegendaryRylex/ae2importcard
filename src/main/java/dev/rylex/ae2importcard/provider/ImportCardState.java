package dev.rylex.ae2importcard.provider;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import dev.rylex.ae2importcard.registry.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ImportCardState {

    private final OwedImportTracker tracker = new OwedImportTracker();
    private final IUpgradeInventory upgrades;
    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private final Supplier<BlockEntity> blockEntity;
    private final Runnable saveChanges;

    public ImportCardState(
            IManagedGridNode mainNode,
            IActionSource actionSource,
            Supplier<BlockEntity> blockEntity,
            Runnable saveChanges) {
        this.mainNode = mainNode;
        this.actionSource = actionSource;
        this.blockEntity = blockEntity;
        this.saveChanges = saveChanges;
        this.upgrades = ImportCardUpgrades.create(this::onUpgradesChanged);
    }

    public IUpgradeInventory upgrades() {
        return upgrades;
    }

    public void recordOwed(IPatternDetails pattern, KeyCounter[] inputs, Direction side) {
        if (!hasImportCard()) {
            return;
        }
        if (tracker.record(pattern, inputs, side)) {
            mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
            saveChanges.run();
        }
    }

    public boolean hasWork() {
        return !tracker.isEmpty() && hasImportCard();
    }

    public boolean importOwed() {
        if (tracker.isEmpty() || !hasImportCard() || !mainNode.isActive()) {
            return false;
        }

        var host = blockEntity.get();
        if (!(host.getLevel() instanceof ServerLevel level)) {
            return false;
        }

        var result = tracker.importOwed(level, host.getBlockPos(), mainNode.getGrid(), actionSource);
        if (result.changed()) {
            saveChanges.run();
        }
        return result.moved();
    }

    public void writeToNBT(ValueOutput output) {
        upgrades.writeToNBT(output, ImportCardUpgrades.UPGRADES_KEY);
        tracker.writeToNBT(output);
    }

    public void readFromNBT(ValueInput input) {
        upgrades.readFromNBT(input, ImportCardUpgrades.UPGRADES_KEY);
        tracker.readFromNBT(input);
    }

    public void addDrops(List<ItemStack> drops) {
        for (var stack : upgrades) {
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }

    public void clear() {
        upgrades.clear();
        tracker.clear();
    }

    private void onUpgradesChanged() {
        if (!hasImportCard()) {
            tracker.clear();
        }
        saveChanges.run();
    }

    private boolean hasImportCard() {
        return upgrades.isInstalled(ModItems.IMPORT_CARD.get());
    }
}
