package org.belltaffmod.bellstaff_tplayers.client;


public final class ClientState {

    private ClientState () {}

    private static float tierFovReduction = 0.0F;
    private static boolean minigameActive = false;

    public static float getTierFovReduction() { return tierFovReduction; }
    public static void setTierFovReduction(float value) { tierFovReduction = value; }

    public static boolean isMinigameActive() { return minigameActive; }
    public static void setMinigameActive(boolean value) { minigameActive = value; }

    public static void reset() {
        tierFovReduction = 0.0F;
        minigameActive = false;
    }
}
