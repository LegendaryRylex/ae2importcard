package dev.rylex.ae2importcard.compat.advancedae.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rylex.ae2importcard.provider.ImportCardState;
import dev.rylex.ae2importcard.provider.ImportTrackerHost;
import dev.rylex.ae2importcard.provider.ImportUpgradeHost;
import dev.rylex.ae2importcard.provider.OwedSource;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogic;
import net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogicHost;
import net.pedroksl.advanced_ae.common.patterns.IAdvPatternDetails;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvPatternProviderLogic.class)
public abstract class AdvPatternProviderLogicMixin implements ImportUpgradeHost, ImportTrackerHost {

    @Shadow
    @Final
    private AdvPatternProviderLogicHost host;

    @Shadow
    @Final
    private IManagedGridNode mainNode;

    @Shadow
    @Final
    private IActionSource actionSource;

    @Shadow
    private Direction sendDirection;

    @Unique
    private ImportCardState ae2importcard$state;

    @Inject(
            method =
                    "<init>(Lappeng/api/networking/IManagedGridNode;Lnet/pedroksl/advanced_ae/common/logic/AdvPatternProviderLogicHost;I)V",
            at = @At("RETURN"))
    private void ae2importcard$createUpgradeInventory(
            IManagedGridNode mainNode, AdvPatternProviderLogicHost host, int patternInventorySize, CallbackInfo ci) {
        this.ae2importcard$state =
                new ImportCardState(this.mainNode, actionSource, this.host::getBlockEntity, this.host::saveChanges);
    }

    @Override
    public IUpgradeInventory ae2importcardUpgrades() {
        return ae2importcard$state == null ? UpgradeInventories.empty() : ae2importcard$state.upgrades();
    }

    @Override
    public void ae2importcardRecordOwed(IPatternDetails pattern, KeyCounter[] inputs, OwedSource source) {
        ae2importcard$state.recordOwed(pattern, inputs, source);
    }

    @WrapOperation(
            method = "pushPattern",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lappeng/api/implementations/blockentities/ICraftingMachine;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;Lnet/minecraft/core/Direction;)Z"))
    private boolean ae2importcard$recordMachineDispatch(
            ICraftingMachine machine,
            IPatternDetails pattern,
            KeyCounter[] inputs,
            Direction ejectionDirection,
            Operation<Boolean> original) {
        boolean pushed = original.call(machine, pattern, inputs, ejectionDirection);
        if (pushed) {
            ae2importcardRecordOwed(pattern, inputs, new OwedSource.Side(ejectionDirection.getOpposite()));
        }
        return pushed;
    }

    @Inject(
            method = "pushPattern",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/pedroksl/advanced_ae/common/logic/AdvPatternProviderLogic;sendStacksOut()Z"))
    private void ae2importcard$recordExternalDispatch(
            IPatternDetails patternDetails, KeyCounter[] inputHolder, CallbackInfoReturnable<Boolean> cir) {
        if (sendDirection != null) {
            ae2importcardRecordOwed(patternDetails, inputHolder, new OwedSource.Side(sendDirection));
        }
    }

    @Inject(
            method = "pushInputsDirectionally",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/pedroksl/advanced_ae/common/logic/AdvPatternProviderLogic;sendStacksOut()Z"))
    private void ae2importcard$recordDirectionalDispatch(
            Direction direction,
            KeyCounter[] inputHolder,
            IAdvPatternDetails patternDetails,
            CallbackInfoReturnable<Boolean> cir) {
        ae2importcardRecordOwed((IPatternDetails) patternDetails, inputHolder, new OwedSource.Side(direction));
    }

    @Inject(method = "hasWorkToDo", at = @At("RETURN"), cancellable = true)
    private void ae2importcard$hasImportWork(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ae2importcard$state.hasWork()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "doWork", at = @At("RETURN"), cancellable = true)
    private void ae2importcard$doImportWork(CallbackInfoReturnable<Boolean> cir) {
        if (ae2importcard$state.importOwed()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void ae2importcard$writeToNBT(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        ae2importcard$state.writeToNBT(tag, registries);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void ae2importcard$readFromNBT(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        ae2importcard$state.readFromNBT(tag, registries);
    }

    @Inject(method = "addDrops", at = @At("RETURN"))
    private void ae2importcard$addDrops(List<ItemStack> drops, CallbackInfo ci) {
        ae2importcard$state.addDrops(drops);
    }

    @Inject(method = "clearContent", at = @At("RETURN"))
    private void ae2importcard$clearContent(CallbackInfo ci) {
        ae2importcard$state.clear();
    }
}
