package dev.rylex.ae2importcard.client;

import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import appeng.core.localization.GuiText;
import appeng.menu.AEBaseMenu;
import dev.rylex.ae2importcard.mixin.client.ScreenStyleAccessor;
import dev.rylex.ae2importcard.provider.ImportCardSlotSemantics;
import dev.rylex.ae2importcard.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

public final class ImportUpgradePanels {

    public static final String WIDGET_ID = "ae2importcardUpgrades";
    public static final String STOCK_WIDGET_ID = "upgrades";

    private ImportUpgradePanels() {}

    public static void add(ScreenStyle style, AEBaseMenu menu, WidgetContainer widgets) {
        if (style instanceof ScreenStyleAccessor accessor) {
            add(menu.getSlots(ImportCardSlotSemantics.IMPORT_UPGRADE), accessor.ae2importcard$widgets(), widgets);
        }
    }

    private static void add(List<Slot> slots, Map<String, WidgetStyle> widgetStyles, WidgetContainer widgets) {
        var stock = widgetStyles.get(STOCK_WIDGET_ID);
        if (slots.isEmpty() || stock == null) {
            return;
        }

        widgetStyles.put(WIDGET_ID, stock);
        widgets.add(WIDGET_ID, new StackedUpgradesPanel(slots, ImportUpgradePanels::compatibleUpgrades, widgets));
    }

    public static List<Component> compatibleUpgrades() {
        var lines = new ArrayList<Component>();
        lines.add(GuiText.CompatibleUpgrades.text());
        lines.add(GuiText.CompatibleUpgrade.text(
                        ModItems.IMPORT_CARD.get().getDefaultInstance().getItemName(), 1)
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
