package dev.rylex.ae2importcard;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public final class ModTestInstance extends GameTestInstance {

    public static final MapCodec<ModTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.fieldOf("name").forGetter(ModTestInstance::name),
                    TestData.CODEC.forGetter(ModTestInstance::info))
            .apply(instance, ModTestInstance::new));

    private final Identifier name;

    public ModTestInstance(Identifier name, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.name = name;
    }

    @Override
    public void run(GameTestHelper helper) {
        ModGameTests.body(name).accept(helper);
    }

    @Override
    public MapCodec<ModTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal(Ae2ImportCard.MOD_ID);
    }

    private Identifier name() {
        return name;
    }
}
