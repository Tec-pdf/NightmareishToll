package org.belltaffmod.bellstaff_tplayers.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.belltaffmod.bellstaff_tplayers.Bellstaff_tplayers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = Bellstaff_tplayers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BellStaffCommands {

    // Nível de permissão exigido: 2 = operador (cheats ativados no singleplayer).
    private static final int PERMISSION_LEVEL = 2;   // ALTERE ESTE VALOR AQUI

    private BellStaffCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /bellstaff minigame stop <alvos>
        LiteralArgumentBuilder<CommandSourceStack> stopCommand = Commands.literal("stop")
                .then(Commands.argument("alvos", EntityArgument.players())
                        .executes(BellStaffCommands::stop));

        // /bellstaff minigame start <alvos> [<atacante>]
        LiteralArgumentBuilder<CommandSourceStack> startCommand = Commands.literal("start")
                .then(Commands.argument("alvos", EntityArgument.players())
                        .executes(ctx -> start(ctx, null))
                        .then(Commands.argument("atacante", EntityArgument.entity())
                                .executes(ctx -> start(ctx, EntityArgument.getEntity(ctx, "atacante")))));

        // /bellstaff minigame ...
        dispatcher.register(Commands.literal("bellstaff")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .then(Commands.literal("minigame")
                        .then(startCommand)
                        .then(stopCommand)));
    }

    private static int start(CommandContext<CommandSourceStack> ctx, Entity chosenAttacker)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "alvos");

        // Fonte do medo: o atacante escolhido ou, se não houver, quem executou o comando.
        Entity attackerEntity = (chosenAttacker != null) ? chosenAttacker : source.getEntity();
        LivingEntity attacker = (attackerEntity instanceof LivingEntity living) ? living : null;

        int started = 0;
        for (ServerPlayer target : targets) {
            if (!target.isAlive()) continue;
            // O jogador não pode ser a fonte do próprio medo.
            LivingEntity fearSource = (attacker == target) ? null : attacker;
            if (MinigameManager.start(target, fearSource)) {
                started++;
            }
        }

        final int count = started;
        if (count == 0) {
            source.sendFailure(Component.literal("Nenhum jogador iniciou o minigame (já estavam nele ou estão mortos)."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Minigame iniciado em " + count + " jogador(es)."), true);
        return count;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        int stopped = 0;
        for (ServerPlayer target : EntityArgument.getPlayers(ctx, "alvos")) {
            if (MinigameManager.isActive(target)) {
                MinigameManager.cancel(target);
                stopped++;
            }
        }

        final int count = stopped;
        if (count == 0) {
            source.sendFailure(Component.literal("Nenhum dos jogadores estava no minigame."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Minigame encerrado em " + count + " jogador(es)."), true);
        return count;
    }
}