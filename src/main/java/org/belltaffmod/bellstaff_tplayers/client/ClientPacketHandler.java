package org.belltaffmod.bellstaff_tplayers.client;

import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {

    private ClientPacketHandler(){}

    public static void handleFov(float fovReduction){
        ClientState.setTierFovReduction(fovReduction);
    }

    public static void handleMinigameState(boolean active) {
        Minecraft mc = Minecraft.getInstance();
        ClientState.setMinigameActive(active);
        if (active){
            mc.setScreen(new SleepMinigameScreen());
        } else if (mc.screen instanceof SleepMinigameScreen){
            mc.setScreen(null);
        }
    }
}
