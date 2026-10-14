package dev.rylex.ae2importcard;

import dev.rylex.ae2importcard.compat.advancedae.AdvancedAeMixinGameTest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Ae2ImportCard.MOD_ID)
public final class ModGameTests {

    public static final String SMALL = "empty_5x5x5";
    public static final String LARGE = "empty_9x6x9";

    private static final int DEFAULT_TICKS = 100;

    private static final Map<Identifier, Consumer<GameTestHelper>> BODIES = new HashMap<>();

    private ModGameTests() {}

    @SubscribeEvent
    static void registerInstanceType(RegisterEvent event) {
        event.register(
                Registries.TEST_INSTANCE_TYPE,
                registry -> registry.register(Ae2ImportCard.id("named"), ModTestInstance.CODEC));
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        BODIES.clear();

        if (NepPresence.isLoaded()) {
            NepInstalledGameTest.register(new Batch(event, "ae2importcard_nep_installed", SMALL));
            return;
        }

        ImportCardInstallGameTest.register(new Batch(event, "ae2importcard_import_card_install", SMALL));
        AddonProviderImportCardGameTest.register(new Batch(event, "ae2importcard_addon_providers", SMALL));
        ImportCardChainedCraftGameTest.register(new Batch(event, "ae2importcard_import_card_chain", LARGE));
        NepAliasGameTest.register(new Batch(event, "ae2importcard_nep_alias", SMALL));
        if (ModList.get().isLoaded("advanced_ae")) {
            AdvancedAeMixinGameTest.register(new Batch(event, "ae2importcard_advancedae", SMALL));
        }
    }

    static Consumer<GameTestHelper> body(Identifier name) {
        Consumer<GameTestHelper> body = BODIES.get(name);
        if (body == null) {
            throw new IllegalStateException("no game test body registered under " + name);
        }
        return body;
    }

    public static final class Batch {

        private final RegisterGameTestsEvent event;
        private final String name;
        private final Identifier structure;
        private final Holder<TestEnvironmentDefinition<?>> environment;

        private Batch(RegisterGameTestsEvent event, String name, String structure) {
            this.event = event;
            this.name = name;
            this.structure = Ae2ImportCard.id(structure);
            this.environment = event.registerEnvironment(Ae2ImportCard.id(name));
        }

        public Batch add(String test, Consumer<GameTestHelper> body) {
            return add(test, DEFAULT_TICKS, body);
        }

        public Batch add(String test, int maxTicks, Consumer<GameTestHelper> body) {
            Identifier id = Ae2ImportCard.id(name + "/" + test);
            BODIES.put(id, body);
            event.registerTest(
                    id,
                    new ModTestInstance(id, new TestData<>(environment, structure, maxTicks, 0, true, Rotation.NONE)));
            return this;
        }
    }
}
