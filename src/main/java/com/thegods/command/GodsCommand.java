package com.thegods.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.thegods.god.God;
import com.thegods.god.GodData;
import com.thegods.god.GodManager;
import com.thegods.god.Gods;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * /gods grant <giocatori> <dio>
 * /gods revoke <giocatori>
 * /gods refill [giocatori]
 * /gods who
 * /gods list
 */
public final class GodsCommand {
    private static final DynamicCommandExceptionType UNKNOWN_GOD =
            new DynamicCommandExceptionType(id -> Component.translatable("command.thegods.unknown_god", id));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gods")
                .then(Commands.literal("list").executes(GodsCommand::list))
                .then(Commands.literal("who").executes(GodsCommand::who))
                .then(Commands.literal("grant").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("god", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Gods.ids(), b))
                                        .executes(GodsCommand::grant))))
                .then(Commands.literal("revoke").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(GodsCommand::revoke)))
                .then(Commands.literal("refill").requires(s -> s.hasPermission(2))
                        .executes(c -> refill(c, java.util.List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> refill(c, EntityArgument.getPlayers(c, "targets"))))));
    }

    private static int grant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "god");
        God god = Gods.get(id);
        if (god == null) throw UNKNOWN_GOD.create(id);
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer p : targets) GodManager.grant(p, god);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.thegods.granted", god.displayName(), targets.size()), true);
        return targets.size();
    }

    private static int revoke(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer p : targets) GodManager.revoke(p);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.thegods.revoked", targets.size()), true);
        return targets.size();
    }

    private static int refill(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            GodData d = GodManager.data(p);
            God g = d.god();
            if (g == null) continue;
            d.setEnergy(g.maxEnergy());
            d.clearCooldowns();
            GodManager.syncAll(p);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.thegods.refilled", targets.size()), false);
        return targets.size();
    }

    private static int who(CommandContext<CommandSourceStack> ctx) {
        int n = 0;
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            God g = GodManager.godOf(p);
            if (g == null) continue;
            n++;
            ctx.getSource().sendSuccess(() -> Component.literal(" • ").append(p.getDisplayName()).append(" → ")
                    .append(g.displayName().copy().withStyle(s -> s.withColor(g.color()))), false);
        }
        if (n == 0) ctx.getSource().sendSuccess(() -> Component.translatable("command.thegods.nobody"), false);
        return n;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        for (God g : Gods.all()) {
            ctx.getSource().sendSuccess(() -> Component.literal(" • ").append(g.displayName().copy().withStyle(s -> s.withColor(g.color())))
                    .append(Component.literal(" (" + g.id() + ") — ")).append(g.epithet()), false);
        }
        return Gods.all().size();
    }

    private GodsCommand() {}
}
