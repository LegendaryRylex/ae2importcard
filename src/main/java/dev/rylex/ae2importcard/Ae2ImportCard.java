package dev.rylex.ae2importcard;

import appeng.api.ids.AECreativeTabIds;
import appeng.core.definitions.AEBlocks;
import dev.rylex.ae2importcard.client.ClientSetup;
import dev.rylex.ae2importcard.config.ImportCardConfig;
import dev.rylex.ae2importcard.provider.ImportCardSlotSemantics;
import dev.rylex.ae2importcard.provider.ImportCardUpgrades;
import dev.rylex.ae2importcard.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Ae2ImportCard.MOD_ID)
public final class Ae2ImportCard {
    public static final String MOD_ID = "ae2importcard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public Ae2ImportCard(IEventBus modBus, ModContainer container, Dist dist) {
        if (NepPresence.isLoaded()) {
            modBus.addListener(RegisterEvent.class, event -> aliasCard(event, ModItems.CARD_ID, ModItems.NEP_CARD_ID));
            LOGGER.info("nep provides the Import Card, so {} stays inactive", MOD_ID);
            return;
        }

        ModItems.ITEMS.register(modBus);
        ImportCardSlotSemantics.init();
        container.registerConfig(ModConfig.Type.SERVER, ImportCardConfig.SPEC);
        modBus.addListener(RegisterEvent.class, event -> aliasCard(event, ModItems.NEP_CARD_ID, ModItems.CARD_ID));
        modBus.addListener(Ae2ImportCard::commonSetup);
        modBus.addListener(Ae2ImportCard::addToCreativeTab);
        modBus.addListener(Ae2ImportCard::addGuidePack);

        if (dist.isClient()) {
            ClientSetup.init(container);
        }
    }

    private static void aliasCard(RegisterEvent event, Identifier from, Identifier to) {
        if (event.getRegistryKey().equals(Registries.ITEM)) {
            event.getRegistry().addAlias(from, to);
        }
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var providers = new ArrayList<Item>();
            providers.add(AEBlocks.PATTERN_PROVIDER.asItem());
            addProviders(providers, "advanced_ae", "adv_pattern_provider", "small_adv_pattern_provider");
            addProviders(providers, "extendedae", "ex_pattern_provider");
            addProviders(providers, "expandedae", "exp_pattern_provider");
            addProviders(providers, "megacells", "mega_pattern_provider");
            addProviders(providers, "appliedcreate", "andesite_pattern_provider", "brass_pattern_provider");
            ImportCardUpgrades.register(providers);
        });
    }

    private static void addProviders(List<Item> providers, String modId, String... itemPaths) {
        if (!ModList.get().isLoaded(modId)) {
            return;
        }
        for (var path : itemPaths) {
            BuiltInRegistries.ITEM
                    .getOptional(Identifier.fromNamespaceAndPath(modId, path))
                    .ifPresent(providers::add);
        }
    }

    private static void addGuidePack(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addPackFinders(
                    id("resourcepacks/guide"),
                    PackType.CLIENT_RESOURCES,
                    Component.translatable("ae2importcard.pack.guide"),
                    PackSource.BUILT_IN,
                    true,
                    Pack.Position.TOP);
        }
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(AECreativeTabIds.MAIN)) {
            event.accept(ModItems.IMPORT_CARD);
        }
    }
}
