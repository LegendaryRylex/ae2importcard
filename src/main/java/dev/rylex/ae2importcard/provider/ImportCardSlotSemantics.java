package dev.rylex.ae2importcard.provider;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import dev.rylex.ae2importcard.Ae2ImportCard;

public final class ImportCardSlotSemantics {

    private static final int QUICK_MOVE_PRIORITY = 100;

    public static final SlotSemantic IMPORT_UPGRADE =
            SlotSemantics.register(Ae2ImportCard.MOD_ID + ":import_upgrade", false, QUICK_MOVE_PRIORITY);

    private ImportCardSlotSemantics() {}

    public static void init() {}
}
