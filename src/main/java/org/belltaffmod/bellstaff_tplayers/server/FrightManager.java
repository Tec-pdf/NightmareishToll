package org.belltaffmod.bellstaff_tplayers.server;

import org.belltaffmod.bellstaff_tplayers.BellstaffConstants;
import org.belltaffmod.bellstaff_tplayers.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FrightManager {

    private FrightManager() {}

    // vítima -> fonte do medo (quem usou o cajado)
    private static final Map<UUID, UUID> SOURCES = new HashMap<>();

    public static void apply(LivingEntity victim, UUID sourceId) {
        SOURCES.put(victim.getUUID(), sourceId);
        victim.addEffect(new MobEffectInstance(ModEffects.NIGHTMAREISH_FRIGHT.get(),
                BellstaffConstants.FRIGHT_DURATION_SECONDS * 20, 0, false, true, true));
    }

    public static UUID getSource(UUID victimId) { return SOURCES.get(victimId); }

    public static void clear(UUID victimId) { SOURCES.remove(victimId); }

    public static void clearAll() { SOURCES.clear(); }
}