package dev.rylex.ae2importcard.provider;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import org.jetbrains.annotations.Nullable;

public interface ImportTrackerHost {

    void ae2importcardRecordOwed(IPatternDetails pattern, @Nullable KeyCounter[] inputs, OwedSource source);
}
