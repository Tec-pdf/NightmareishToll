package org.belltaffmod.bellstaff_tplayers.network;

import org.belltaffmod.bellstaff_tplayers.server.MinigameManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MinigameCompletePacket {

    public MinigameCompletePacket() {}      // não carrega dados

    public static void encode(MinigameCompletePacket msg, FriendlyByteBuf buf) {}

    public static MinigameCompletePacket decode(FriendlyByteBuf buf) {
        return new MinigameCompletePacket();
    }

    // Roda no SERVIDOR. getSender() é quem mandou o pacote (o jogador que venceu).
    public static void handle(MinigameCompletePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            MinigameManager.complete(sender);   // o servidor VALIDA antes de libertar
        }
    }
}