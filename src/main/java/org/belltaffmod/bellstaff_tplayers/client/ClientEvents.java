package org.belltaffmod.bellstaff_tplayers.client;

import org.belltaffmod.bellstaff_tplayers.BellstaffConstants;
import org.belltaffmod.bellstaff_tplayers.Bellstaff_tplayers;
import org.belltaffmod.bellstaff_tplayers.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Bellstaff_tplayers.MODID,
bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)

public final class ClientEvents {

    private ClientEvents(){}

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event){
        if (!event.usedConfiguredFov()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (ClientState.isMinigameActive()){
            event.setFOV(BellstaffConstants.MINIGAME_FOV);
            return;
        }

        if (mc.player.hasEffect(ModEffects.BELL_TOLL.get())){
            double reduced = event.getFOV() - ClientState.getTierFovReduction();
            event.setFOV(Math.max(BellstaffConstants.MIN_FOV, reduced));
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event){
        if (!ClientState.isMinigameActive()) return;
        Input input = event.getInput();
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.reset();
    }
}
