package org.belltaffmod.bellstaff_tplayers.server;

import org.belltaffmod.bellstaff_tplayers.BellstaffConstants;
import org.belltaffmod.bellstaff_tplayers.network.FovEffectPacket;
import org.belltaffmod.bellstaff_tplayers.network.ModNetwork;
import org.belltaffmod.bellstaff_tplayers.registry.ModEffects;
import org.belltaffmod.bellstaff_tplayers.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class BellStaffHitHandler {

    private BellStaffHitHandler(){}

    private static final String CRIT_KEY = "bellstaff_last_hit_crit"; //NBT Temp que guarda se o ultimo hit foi crit

    public static void markCritical(Player player, boolean critical){
        player.getPersistentData().putBoolean(CRIT_KEY, critical); //Marcador constante de crit
    }

    //Lê a marcação de crit e limpa em seguida
    public static boolean consumeCritical(LivingEntity attacker){
        boolean critical = attacker.getPersistentData().getBoolean(CRIT_KEY);
        attacker.getPersistentData().remove(CRIT_KEY);
        return critical;
    }

    //Main Lógica
    public static void onHit(LivingEntity attacker, LivingEntity target, boolean critical){
        Level level = attacker.level();
        if (level.isClientSide) return;

        //Som de sino por proximidade
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                ModSounds.BELL_HIT.get(), SoundSource.PLAYERS,
                BellstaffConstants.HIT_SOUND_VOLUME, BellstaffConstants.HIT_SOUND_PITCH);

        //Dano Mágico
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().indirectMagic(attacker, attacker),
                BellstaffConstants.MAGIC_DAMAGE);

        //Efeitos Funny
        if (BellstaffConstants.AFFECT_ONLY_PLAYERS && !(target instanceof ServerPlayer)) return;
        if (!target.isAlive()) return;
        if (target instanceof ServerPlayer p && MinigameManager.isActive(p)) return;

        int currentTier = getCurrentTier(target);
        RandomSource random = target.getRandom();

        if (currentTier < BellstaffConstants.MAX_TIER){
            float chance = BellstaffConstants.TIER_CHANCES[currentTier]
                    + (critical ? BellstaffConstants.CRIT_BONUS_TIER_CHANCE : 0.0F);
            if (random.nextFloat() < chance){
                applyTier(target, currentTier + 1);
            }
        } else if (target instanceof ServerPlayer player){
            float chance = BellstaffConstants.MINIGAME_CHANCE
                    + (critical ? BellstaffConstants.CRIT_BONUS_MINIGAME_CHANCE : 0.0F);

            if (random.nextFloat() < chance){
                MinigameManager.start(player, attacker);
            }
        }
    }

    //Lê o nível atual do alvo partindo do Bell Toll
    public static int getCurrentTier(LivingEntity entity){
        MobEffectInstance effect = entity.getEffect(ModEffects.BELL_TOLL.get());
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    //Packet que aplica nível de fov, slow e weakness
    public static void applyTier(LivingEntity target, int tier) {
        int index = tier - 1;
        int duration = BellstaffConstants.EFFECT_DURATION_SECONDS * 20;

        target.addEffect(new MobEffectInstance(ModEffects.BELL_TOLL.get(),
                duration, index, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                duration, BellstaffConstants.WEAKNESS_AMPLIFIERS[index]));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                duration, BellstaffConstants.SLOWNESS_AMPLIFIERS[index]));

        if (target instanceof ServerPlayer player){
            ModNetwork.sendToPlayer(
                    new FovEffectPacket(BellstaffConstants.FOV_REDUCTIONS[index]), player
            );
        }
    }
}