package dev.rylex.ae2importcard.provider;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.parts.automation.StackWorldBehaviors;
import com.mojang.serialization.Codec;
import dev.rylex.ae2importcard.config.ImportCardConfig;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public final class OwedImportTracker {

    /** Shared with nep so in-flight imports survive one mod replacing the other. */
    private static final String DISPATCHES_KEY = "nepOwedDispatches";

    private static final String LEGACY_KEY = "nepOwed";

    /** nep's marker for a dispatch owed by a remote machine rather than a side, which this mod cannot collect from. */
    private static final String MACHINE_KIND_KEY = "kind";

    private static final String SIDE_KEY = "side";

    private static final Codec<List<GenericStack>> STACKS_CODEC = GenericStack.CODEC.listOf();

    public record ImportResult(boolean moved, boolean changed) {}

    private final Map<Direction, List<OwedDispatch>> dispatches = new LinkedHashMap<>();
    private final Map<Direction, StackImportStrategy> strategies = new EnumMap<>(Direction.class);

    public boolean isEmpty() {
        return dispatches.isEmpty();
    }

    public void clear() {
        dispatches.clear();
        strategies.clear();
    }

    public boolean record(IPatternDetails pattern, @Nullable KeyCounter[] inputs, Direction side) {
        Map<AEKey, Long> owed = new LinkedHashMap<>();
        for (var output : pattern.getOutputs()) {
            if (output.amount() > 0) {
                owed.merge(output.what(), output.amount(), Long::sum);
            }
        }
        if (owed.isEmpty()) {
            return false;
        }
        dispatches.computeIfAbsent(side, s -> new ArrayList<>()).add(new OwedDispatch(parkedItems(inputs), owed));
        return true;
    }

    private static Map<AEKey, Long> parkedItems(@Nullable KeyCounter[] inputs) {
        Map<AEKey, Long> parked = new LinkedHashMap<>();
        if (inputs == null) {
            return parked;
        }
        for (KeyCounter counter : inputs) {
            if (counter == null) {
                continue;
            }
            for (var entry : counter) {
                if (entry.getLongValue() > 0) {
                    parked.merge(entry.getKey(), entry.getLongValue(), Long::sum);
                }
            }
        }
        return parked;
    }

    public ImportResult importOwed(ServerLevel level, BlockPos providerPos, IGrid grid, IActionSource actionSource) {
        ICraftingService crafting = grid.getCraftingService();
        var storage = grid.getStorageService();
        var energy = grid.getEnergyService();

        long now = level.getGameTime();
        long grace = ImportCardConfig.importCardGrace();

        boolean moved = false;
        boolean changed = false;
        Iterator<Map.Entry<Direction, List<OwedDispatch>>> sides =
                dispatches.entrySet().iterator();
        while (sides.hasNext()) {
            Map.Entry<Direction, List<OwedDispatch>> sideEntry = sides.next();
            Direction side = sideEntry.getKey();
            List<OwedDispatch> live = sideEntry.getValue();

            for (Map.Entry<AEKey, Long> budgeted : budgets(live).entrySet()) {
                AEKey key = budgeted.getKey();
                var context = new OwedImportContext(
                        storage, energy, actionSource, key, (int) Math.min(budgeted.getValue(), Integer.MAX_VALUE));
                strategyFor(level, providerPos, side).transfer(context);

                long imported = context.moved();
                if (imported > 0) {
                    moved = true;
                    changed = true;
                    settle(live, key, imported);
                }
            }

            changed |= expireDispatches(live, crafting, now, grace);

            if (live.isEmpty()) {
                strategies.remove(side);
                sides.remove();
            }
        }

        return new ImportResult(moved, changed);
    }

    private static boolean expireDispatches(List<OwedDispatch> live, ICraftingService crafting, long now, long grace) {
        boolean changed = false;
        Iterator<OwedDispatch> pending = live.iterator();
        while (pending.hasNext()) {
            OwedDispatch dispatch = pending.next();
            if (dispatch.isSettled()) {
                pending.remove();
                changed = true;
                continue;
            }
            boolean requested = isRequested(crafting, dispatch);
            if (requested) {
                dispatch.clearGrace();
            } else if (!dispatch.hasDeadline()) {
                dispatch.startGrace(now + grace);
            }
            if (!requested && dispatch.expired(now)) {
                pending.remove();
                changed = true;
            }
        }
        return changed;
    }

    private static Map<AEKey, Long> budgets(List<OwedDispatch> live) {
        Map<AEKey, Long> owed = new LinkedHashMap<>();
        Map<AEKey, Long> reserved = new HashMap<>();
        for (OwedDispatch dispatch : live) {
            for (Map.Entry<AEKey, Long> entry : dispatch.owed().entrySet()) {
                owed.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
            for (AEKey key : dispatch.parked().keySet()) {
                long amount = dispatch.reserved(key);
                if (amount > 0) {
                    reserved.merge(key, amount, Long::sum);
                }
            }
        }
        owed.entrySet().removeIf(entry -> {
            long budget = entry.getValue() - reserved.getOrDefault(entry.getKey(), 0L);
            entry.setValue(budget);
            return budget <= 0;
        });
        return owed;
    }

    private static void settle(List<OwedDispatch> live, AEKey key, long imported) {
        long remaining = imported;
        for (OwedDispatch dispatch : live) {
            if (remaining <= 0) {
                return;
            }
            remaining -= dispatch.take(key, remaining);
        }
    }

    private static boolean isRequested(ICraftingService crafting, OwedDispatch dispatch) {
        for (AEKey key : dispatch.owed().keySet()) {
            if (crafting.getRequestedAmount(key) > 0) {
                return true;
            }
        }
        return false;
    }

    public void writeToNBT(ValueOutput output) {
        var list = output.childrenList(DISPATCHES_KEY);
        for (Map.Entry<Direction, List<OwedDispatch>> sideEntry : dispatches.entrySet()) {
            for (OwedDispatch dispatch : sideEntry.getValue()) {
                var entryOutput = list.addChild();
                entryOutput.putByte(SIDE_KEY, (byte) sideEntry.getKey().get3DDataValue());
                entryOutput.store("owed", STACKS_CODEC, stacksOf(dispatch.owed()));
                if (!dispatch.parked().isEmpty()) {
                    entryOutput.store("parked", STACKS_CODEC, stacksOf(dispatch.parked()));
                }
                if (dispatch.hasDeadline()) {
                    entryOutput.putLong("grace", dispatch.deadline());
                }
            }
        }
    }

    public void readFromNBT(ValueInput input) {
        clear();

        for (var entryInput : input.childrenListOrEmpty(DISPATCHES_KEY)) {
            if (entryInput.getString(MACHINE_KIND_KEY).isPresent()) {
                continue;
            }
            Map<AEKey, Long> owed =
                    stacksInto(entryInput.read("owed", STACKS_CODEC).orElse(List.of()));
            if (owed.isEmpty()) {
                continue;
            }
            Map<AEKey, Long> parked =
                    stacksInto(entryInput.read("parked", STACKS_CODEC).orElse(List.of()));
            long deadline = entryInput.getLongOr("grace", OwedDispatch.NO_DEADLINE);
            add(readSide(entryInput), new OwedDispatch(parked, owed, deadline));
        }
        if (!dispatches.isEmpty()) {
            return;
        }

        for (var entryInput : input.childrenListOrEmpty(LEGACY_KEY)) {
            if (entryInput.getString(MACHINE_KIND_KEY).isPresent()) {
                continue;
            }
            var stack = entryInput.read("stack", GenericStack.CODEC).orElse(null);
            if (stack == null || stack.amount() <= 0) {
                continue;
            }
            long deadline = entryInput.getLongOr("grace", OwedDispatch.NO_DEADLINE);
            add(readSide(entryInput), new OwedDispatch(Map.of(), Map.of(stack.what(), stack.amount()), deadline));
        }
    }

    private static Direction readSide(ValueInput entryInput) {
        return Direction.from3DDataValue(entryInput.getByteOr(SIDE_KEY, (byte) 0));
    }

    private void add(Direction side, OwedDispatch dispatch) {
        dispatches.computeIfAbsent(side, s -> new ArrayList<>()).add(dispatch);
    }

    private static List<GenericStack> stacksOf(Map<AEKey, Long> stacks) {
        List<GenericStack> list = new ArrayList<>(stacks.size());
        for (Map.Entry<AEKey, Long> entry : stacks.entrySet()) {
            list.add(new GenericStack(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    private static Map<AEKey, Long> stacksInto(List<GenericStack> stacks) {
        Map<AEKey, Long> into = new LinkedHashMap<>();
        for (GenericStack stack : stacks) {
            if (stack != null && stack.amount() > 0) {
                into.merge(stack.what(), stack.amount(), Long::sum);
            }
        }
        return into;
    }

    private StackImportStrategy strategyFor(ServerLevel level, BlockPos providerPos, Direction side) {
        return strategies.computeIfAbsent(side, s -> createStrategy(level, providerPos, s));
    }

    private static StackImportStrategy createStrategy(ServerLevel level, BlockPos providerPos, Direction side) {
        BlockPos from = providerPos.relative(side);
        Direction face = side.getOpposite();
        StackImportStrategy items =
                StackWorldBehaviors.createImportFacade(level, from, face, type -> type == AEKeyType.items());
        StackImportStrategy fluids = new OwedFluidImport(level, from, face);
        return context -> items.transfer(context) | fluids.transfer(context);
    }
}
