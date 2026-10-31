package org.belltaffmod.bellstaff_tplayers.effect;

import org.belltaffmod.bellstaff_tplayers.BellstaffConstants;
import org.belltaffmod.bellstaff_tplayers.server.FrightManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class NightmareishFrightEffect extends MobEffect {

    public NightmareishFrightEffect(){
        super(MobEffectCategory.HARMFUL, 0x1A0F2E); //cor
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier){
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity victim, int amplifier){
        if (!(victim.level() instanceof ServerLevel serverLevel)) return;

        UUID sourceId = FrightManager.getSource(victim.getUUID());
        if (sourceId == null) return;
        Entity source = serverLevel.getEntity(sourceId);
        if (source == null) return;

        //Distância para longe da fonte(atacante)
        double dx = victim.getX() - source.getX();
        double dz = victim.getZ() - source.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance >= BellstaffConstants.FRIGHT_REPEL_RADIUS) return;

        //Direção para longe da fonte(atacante)
        double pushX = BellstaffConstants.FRIGHT_PUSH_STRENGTH;
        double pushZ = 0.0D;
        if (distance > 0.001D){
            pushX = dx / distance * BellstaffConstants.FRIGHT_PUSH_STRENGTH;
            pushZ = dz / distance * BellstaffConstants.FRIGHT_PUSH_STRENGTH;
        }

        Vec3 motion = victim.getDeltaMovement();
        victim.setDeltaMovement(pushX, motion.y, pushZ);
        victim.hurtMarked = true;
    }
}