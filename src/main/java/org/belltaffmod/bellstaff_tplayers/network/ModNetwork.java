package org.belltaffmod.bellstaff_tplayers.network;

import org.belltaffmod.bellstaff_tplayers.Bellstaff_tplayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {

    private ModNetwork(){}

    //Se mudar o formato de algum pacote, aumente este número ("2", "3"...)
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Bellstaff_tplayers.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register(){
        int id = 0;

        CHANNEL.messageBuilder(FovEffectPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FovEffectPacket::encode)
                .decoder(FovEffectPacket::decode)
                .consumerMainThread(FovEffectPacket::handle)
                .add();

        CHANNEL.messageBuilder(MinigameStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MinigameStatePacket::encode)
                .decoder(MinigameStatePacket::decode)
                .consumerMainThread(MinigameStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(MinigameCompletePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MinigameCompletePacket::encode)
                .decoder(MinigameCompletePacket::decode)
                .consumerMainThread(MinigameCompletePacket::handle)
                .add();
    }

    //Ponte server -> jogador específico
    public static void sendToPlayer(Object packet, ServerPlayer player){
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    //Ponte client -> server
    public static void sendToServer(Object packet){
        CHANNEL.sendToServer(packet);
    }
}