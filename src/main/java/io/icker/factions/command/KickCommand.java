package io.icker.factions.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class KickCommand implements Command {
    private int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity initiator = context.getSource().getPlayer();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");

        try {
            execKick(initiator, target);
        } catch (Exception e) {
            new Message(Text.translatable(e.getMessage()))
                .fail().send(initiator, false);
            return 0;
        }

        new Message(Text.translatable("factions.command.kick.success.actor", target.getName().getString()))
            .send(initiator, false);
        new Message(Text.translatable("factions.command.kick.success.subject", initiator.getName().getString()))
            .send(target, false);
        return 1;
    }

    public static void execKick(ServerPlayerEntity initiator, ServerPlayerEntity target) throws Exception {
        if (target.getUuid().equals(initiator.getUuid())) {
            throw new Exception("factions.command.kick.fail.self");
        }

        User selfUser = Command.getUser(initiator);
        User targetUser = User.get(target.getUuid());

        if (selfUser.getFaction() != null || targetUser.getFaction().getID() != selfUser.getFaction().getID()) {
            throw new Exception("factions.command.kick.fail.other_faction");
        }

        if (selfUser.rank == User.Rank.LEADER && (targetUser.rank == User.Rank.LEADER || targetUser.rank == User.Rank.OWNER)) {
            throw new Exception("factions.command.kick.fail.high_rank");
        }

        targetUser.leaveFaction();
        initiator.getServer().getPlayerManager().sendCommandTree(target);
    }

    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager.literal("kick")
            .requires(
                Requires.multiple(
                    Requires.isLeader(),
                    Requires.hasPerms("factions.kick", 0)
                )
            )
            .then(
                CommandManager.argument("player", EntityArgumentType.player())
                    .executes(this::run)
            )
            .build();
    }
}
