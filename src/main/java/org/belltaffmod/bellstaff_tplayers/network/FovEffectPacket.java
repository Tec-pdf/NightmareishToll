package org.belltaffmod.bellstaff_tplayers.network;

import org.belltaffmod.bellstaff_tplayers.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FovEffectPacket {

    private final float fovReduction;   // quantos graus reduzir

    public FovEffectPacket(float fovReduction) {
        this.fovReduction = fovReduction;
    }

    // objeto -> bytes (lado que ENVIA)
    public static void encode(FovEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.fovReduction);
    }

    // bytes -> objeto (lado que RECEBE). Devolve o próprio pacote.
    public static FovEffectPacket decode(FriendlyByteBuf buf) {
        return new FovEffectPacket(buf.readFloat());
    }

    // Roda no CLIENTE, na thread principal.
    public static void handle(FovEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleFov(msg.fovReduction));
    }
}