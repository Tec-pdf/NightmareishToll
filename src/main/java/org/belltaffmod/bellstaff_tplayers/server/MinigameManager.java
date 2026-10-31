package org.belltaffmod.bellstaff_tplayers.server;

import org.belltaffmod.bellstaff_tplayers.BellstaffConstants;
import org.belltaffmod.bellstaff_tplayers.network.MinigameStatePacket;
import org.belltaffmod.bellstaff_tplayers.network.ModNetwork;
import org.belltaffmod.bellstaff_tplayers.registry.ModEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class MinigameManager {

    private MinigameManager() {}

    // Dados de uma sessão: quem causou (pode ser null) e em que tick do mundo começou.
    private record Session(UUID attackerId, long startGameTime) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static boolean isActive(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    // Começa o minigame no alvo. Devolve false se ele já estava no minigame.
    // "attacker" pode ser null (ex.: comando sem executor): nesse caso não há Fright no fim.
    public static boolean start(ServerPlayer target, LivingEntity attacker) {
        if (isActive(target)) return false;
        target.closeContainer();   // fecha baú/inventário aberto, se houver

        UUID attackerId = (attacker == null) ? null : attacker.getUUID();
        SESSIONS.put(target.getUUID(),
                new Session(attackerId, target.level().getGameTime()));

        // Paralisia (imobiliza) pelo tempo máximo do minigame.
        target.addEffect(new MobEffectInstance(ModEffects.SLEEP_PARALYSIS.get(),
                BellstaffConstants.MINIGAME_TIMEOUT_SECONDS * 20, 0, false, true, true));

        // Manda o cliente abrir a GUI e travar o FOV.
        ModNetwork.sendToPlayer(new MinigameStatePacket(true), target);
        return true;
    }

    // Chamado quando o CLIENTE diz "enchi a barra". O servidor valida.
    public static void complete(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;                     // não estava no minigame: ignora

        // Anti-trapaça: vitória mais rápida que o humanamente possível é ignorada.
        long elapsed = player.level().getGameTime() - session.startGameTime();
        long minimumTicks = (long) (BellstaffConstants.MINIGAME_REQUIRED_PRESSES
                / BellstaffConstants.MINIGAME_MAX_PRESSES_PER_SECOND * 20.0F);
        if (elapsed < minimumTicks) return;

        SESSIONS.remove(player.getUUID());
        close(player);

        if (BellstaffConstants.CLEAR_TIER_EFFECTS_ON_FREEDOM) {
            player.removeEffect(ModEffects.BELL_TOLL.get());      // seu efeito: ModEffects + .get()
            player.removeEffect(MobEffects.WEAKNESS);             // efeito do jogo: MobEffects
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);    // efeito do jogo: MobEffects
        }

        // Só há medo se alguém causou o minigame (comando sem atacante não gera Fright).
        if (session.attackerId() != null) {
            FrightManager.apply(player, session.attackerId());
        }
    }

    // Encerra o minigame SEM vitória (morte, comando stop...), avisando o cliente.
    public static void cancel(ServerPlayer player) {
        if (SESSIONS.remove(player.getUUID()) != null) {
            close(player);
        }
    }

    // Esquece o jogador sem enviar pacote (usado ao desconectar).
    public static void forget(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    // Chamado 1 vez por segundo: expira sessões que passaram do tempo máximo.
    public static void tickTimeouts(MinecraftServer server) {
        long limit = BellstaffConstants.MINIGAME_TIMEOUT_SECONDS * 20L;
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {            // jogador saiu
                it.remove();
            } else if (player.level().getGameTime() - entry.getValue().startGameTime() > limit) {
                it.remove();
                close(player);               // solto, mas SEM o Fright
            }
        }
    }

    public static void clearAll() {
        SESSIONS.clear();
    }

    // Remove a paralisia e manda o cliente fechar a GUI.
    private static void close(ServerPlayer player) {
        player.removeEffect(ModEffects.SLEEP_PARALYSIS.get());
        ModNetwork.sendToPlayer(new MinigameStatePacket(false), player);
    }
}