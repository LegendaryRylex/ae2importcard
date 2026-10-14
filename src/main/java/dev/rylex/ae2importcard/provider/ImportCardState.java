package dev.rylex.ae2importcard.provider;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import dev.rylex.ae2importcard.registry.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

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

    public void recordOwed(IPatternDetails pattern, KeyCounter[] inputs, OwedSource source) {
        if (!hasImportCard()) {
            return;
        }
        if (tracker.record(pattern, inputs, source)) {
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

    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        upgrades.writeToNBT(tag, ImportCardUpgrades.UPGRADES_KEY, registries);
        tracker.writeToNBT(tag, registries);
    }

    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        upgrades.readFromNBT(tag, ImportCardUpgrades.UPGRADES_KEY, registries);
        tracker.readFromNBT(tag, registries);
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
