package org.belltaffmod.bellstaff_tplayers.registry;

import net.minecraft.client.resources.sounds.Sound;
import org.belltaffmod.bellstaff_tplayers.Bellstaff_tplayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Bellstaff_tplayers.MODID);

    public static final RegistryObject<SoundEvent> BELL_HIT = SOUNDS.register(
            "bell_hit",
            () -> {
                return SoundEvent.createVariableRangeEvent(
                        new ResourceLocation(Bellstaff_tplayers.MODID, "bell_hit")
                );
            }
    );
}
