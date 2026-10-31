package org.belltaffmod.bellstaff_tplayers.network;

import org.belltaffmod.bellstaff_tplayers.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MinigameStatePacket {

    private final boolean active; //true = abre minigame, false = fecha minigame

    public MinigameStatePacket(boolean active){
        this.active = active;
    }

    public static void encode(MinigameStatePacket msg, FriendlyByteBuf buf){
        buf.writeBoolean(msg.active);
    }

    public static MinigameStatePacket decode(FriendlyByteBuf buf){
        return new MinigameStatePacket(buf.readBoolean());
    }

    public static void handle(MinigameStatePacket msg,Supplier<NetworkEvent.Context> ctx){
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleMinigameState(msg.active));
    }
}
