package org.belltaffmod.bellstaff_tplayers.server;

import org.belltaffmod.bellstaff_tplayers.BellstaffConstants;
import org.belltaffmod.bellstaff_tplayers.Bellstaff_tplayers;
import org.belltaffmod.bellstaff_tplayers.item.BellStaffItem;
import org.belltaffmod.bellstaff_tplayers.network.FovEffectPacket;
import org.belltaffmod.bellstaff_tplayers.network.ModNetwork;
import org.belltaffmod.bellstaff_tplayers.registry.ModEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = Bellstaff_tplayers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEvents {

    private ServerEvents(){}

    //Verifica se o golpe foi crítico
    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event){
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player.getMainHandItem().getItem() instanceof BellStaffItem)) return;

        boolean critical = event.getResult() == Event.Result.ALLOW
                || (event.isVanillaCritical() && event.getResult() == Event.Result.DEFAULT);
        BellStaffHitHandler.markCritical(player, critical);
    }

    //Morrer = encerra o minigame e o efeito
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event){
        if (event.getEntity() instanceof ServerPlayer player) {
            MinigameManager.cancel(player);
            FrightManager.clear(player.getUUID());
        }
    }

    //Saiu do jogo = remove a paralisia antes de salvar o player e limpa a mem
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event){
        if (event.getEntity() instanceof ServerPlayer player){
            player.removeEffect(ModEffects.SLEEP_PARALYSIS.get());
            MinigameManager.forget(player.getUUID());
            FrightManager.clear(player.getUUID());
        }
    }

    //Entrou no jogo: chave de segurança para não ficar preso a sessão antiga
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event){
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!MinigameManager.isActive(player)){
            player.removeEffect(ModEffects.SLEEP_PARALYSIS.get());
        }
        int tier = BellStaffHitHandler.getCurrentTier(player);
        if (tier > 0){
            ModNetwork.sendToPlayer(
                    new FovEffectPacket(BellstaffConstants.FOV_REDUCTIONS[tier - 1]), player
            );
        }
    }

    //Contador de ticks
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event){
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.getTickCount() % 20 == 0) { //20 ticks = 1 sec
            MinigameManager.tickTimeouts(server);
        }
    }

    //Server/mundo fechar = zera dados
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event){
        MinigameManager.clearAll();
        FrightManager.clearAll();
    }
}