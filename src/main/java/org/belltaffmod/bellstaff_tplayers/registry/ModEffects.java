package org.belltaffmod.bellstaff_tplayers.registry;

import org.belltaffmod.bellstaff_tplayers.Bellstaff_tplayers;
import org.belltaffmod.bellstaff_tplayers.effect.NightmareishFrightEffect;
import org.belltaffmod.bellstaff_tplayers.effect.SimpleMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;   // <- Attributes (plural)
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Bellstaff_tplayers.MODID);

    // guarda o NÍVEL do jogador: amplifier 0 = nível 1, 1 = nível 2, 2 = nível 3.
    public static final RegistryObject<MobEffect> BELL_TOLL = EFFECTS.register(
            "bell_toll",
            () -> new SimpleMobEffect(MobEffectCategory.HARMFUL, 0xC9A227)); // Cor

    // imobiliza: velocidade de movimento -100%.
    public static final RegistryObject<MobEffect> SLEEP_PARALYSIS = EFFECTS.register(
            "sleep_paralysis",
            () -> new SimpleMobEffect(MobEffectCategory.HARMFUL, 0x3B2A6B)   // Cor
                    .addAttributeModifier(
                            Attributes.MOVEMENT_SPEED,
                            "5c2b1e0a-8f3d-4a77-9b61-2d4e7a90c1f3",
                            -1.0D,                                            // -1.0 = -100%
                            AttributeModifier.Operation.MULTIPLY_TOTAL));

    // medo pós-libertação: repele o jogador de quem usou o cajado.
    public static final RegistryObject<MobEffect> NIGHTMAREISH_FRIGHT = EFFECTS.register(
            "nightmareish_fright",
            NightmareishFrightEffect::new);
}