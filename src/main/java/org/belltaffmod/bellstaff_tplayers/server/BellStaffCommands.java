package org.belltaffmod.bellstaff_tplayers.server;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.belltaffmod.bellstaff_tplayers.Bellstaff_tplayers;

import java.util.Collection;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Bellstaff_tplayers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BellStaffCommands {

    // Nível de permissão exigido: 2 = operador (cheats ativados no singleplayer).
    private static final int PERMISSION_LEVEL = 2;

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

        // /bellstaff owner set | setuuid | setentity | clear | get
        LiteralArgumentBuilder<CommandSourceStack> ownerCommand = Commands.literal("owner")
                .then(Commands.literal("set")
                        .then(Commands.argument("jogador", GameProfileArgument.gameProfile())
                                .executes(BellStaffCommands::setOwner)))
                .then(Commands.literal("setuuid")
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .executes(BellStaffCommands::setOwnerByUuid)))
                .then(Commands.literal("setentity")
                        .then(Commands.argument("entidade", EntityArgument.entity())
                                .executes(BellStaffCommands::setOwnerEntity)))
                .then(Commands.literal("clear")
                        .executes(BellStaffCommands::clearOwner))
                .then(Commands.literal("get")
                        .executes(BellStaffCommands::showOwner));

        // Um único registro do comando raiz /bellstaff.
        dispatcher.register(Commands.literal("bellstaff")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .then(Commands.literal("minigame")
                        .then(startCommand)
                        .then(stopCommand))
                .then(ownerCommand));
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

    // /bellstaff owner set <jogador>  (nome ou seletor; pode estar offline)
    private static int setOwner(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "jogador");
        if (profiles.size() != 1) {
            source.sendFailure(Component.literal("Escolha exatamente um jogador."));
            return 0;
        }

        GameProfile profile = profiles.iterator().next();
        final String name = profile.getName();
        StaffOwnerData.get(source.getServer()).setOwner(profile.getId(), name);
        source.sendSuccess(() -> Component.literal(name + " agora é o dono da Bellstaff."), true);
        return 1;
    }

    // /bellstaff owner setuuid <uuid>  (não consulta a internet; bom para testes)
    private static int setOwnerByUuid(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        UUID id = UuidArgument.getUuid(ctx, "uuid");

        ServerPlayer online = source.getServer().getPlayerList().getPlayer(id);
        final String name = (online != null) ? online.getGameProfile().getName() : id.toString();

        StaffOwnerData.get(source.getServer()).setOwner(id, name);
        source.sendSuccess(() -> Component.literal("Dono da Bellstaff definido: " + name), true);
        return 1;
    }

    // /bellstaff owner setentity <entidade>  (qualquer entidade, inclusive mobs)
    private static int setOwnerEntity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Entity entity = EntityArgument.getEntity(ctx, "entidade");

        final String name = entity.getName().getString();
        StaffOwnerData.get(source.getServer()).setOwner(entity.getUUID(), name);
        source.sendSuccess(() -> Component.literal("Dono da Bellstaff definido: " + name), true);
        return 1;
    }

    // /bellstaff owner clear
    private static int clearOwner(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        StaffOwnerData.get(source.getServer()).clearOwner();
        source.sendSuccess(() -> Component.literal("A Bellstaff ficou sem dono."), true);
        return 1;
    }

    // /bellstaff owner get
    private static int showOwner(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        StaffOwnerData data = StaffOwnerData.get(source.getServer());
        if (data.getOwner() == null) {
            source.sendSuccess(() -> Component.literal("A Bellstaff não tem dono definido."), false);
            return 0;
        }

        final String name = data.getOwnerName();
        source.sendSuccess(() -> Component.literal("Dono da Bellstaff: " + name), false);
        return 1;
    }
}
