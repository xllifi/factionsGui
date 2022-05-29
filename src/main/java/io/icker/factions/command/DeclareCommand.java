package io.icker.factions.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.api.events.MutualRelationshipEvent;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.Locale;

public class DeclareCommand implements Command {
    private int ally(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return updateRelationship(context, Relationship.Status.ALLY);
    }

    private int neutral(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return updateRelationship(context, Relationship.Status.NEUTRAL);
    }

    private int enemy(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return updateRelationship(context, Relationship.Status.ENEMY);
    }

    private int updateRelationship(CommandContext<ServerCommandSource> context, Relationship.Status status) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "faction");
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        Faction targetFaction = Faction.getByName(name);

        if (targetFaction == null) {
            new Message("Cannot change faction relationship with a faction that doesn't exist").fail().send(player, false);
            return 0;
        }
        
        Faction sourceFaction = User.get(player.getUuid()).getFaction();

        if (Relationship.get(sourceFaction.getID(), targetFaction.getID()).status == status) {
            new Message("That faction relationship has already been declared with this faction").fail().send(player, false);
            return 0;
        }

        Relationship rel = new Relationship(sourceFaction.getID(), targetFaction.getID(), status);
        Relationship rev = rel.getReverse();
        Relationship.set(rel);

        Message msgStatus = rel.status == Relationship.Status.ALLY ? new Message("allies").format(Formatting.GREEN) 
        : rel.status == Relationship.Status.ENEMY ? new Message("enemies").format(Formatting.RED) 
        : new Message("neutral");

        if (rel.status == rev.status) {
            new Message("You are now mutually ").add(msgStatus).add(" with " + targetFaction.getName()).send(sourceFaction);
            new Message("You are now mutually ").add(msgStatus).add(" with " + sourceFaction.getName()).send(targetFaction);

            MutualRelationshipEvent.run(rel);
            return 1;
        }

        new Message("You have declared " + targetFaction.getName() + " as ").add(msgStatus).send(sourceFaction);
        new Message(sourceFaction.getName() + " have declared you as ").add(msgStatus).hover("Click to add them back").click(String.format("/factions %s %s", rel.status.toString().toLowerCase(Locale.ROOT), sourceFaction.getName())).send(targetFaction);
        return 1;
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
            .literal("declare")
            .requires(Requires.hasPerms("factions.declare", 0))
            .requires(Requires.isLeader())
            .then(
                CommandManager.literal("ally")
                .requires(Requires.hasPerms("factions.declare.ally", 0))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions())
                    .executes(this::ally)
                )
            )
            .then(
                CommandManager.literal("neutral")
                .requires(Requires.hasPerms("factions.declare.neutral", 0))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions())
                    .executes(this::neutral)
                )
            )
            .then(
                CommandManager.literal("enemy")
                .requires(Requires.hasPerms("factions.declare.enemy", 0))
                .then(
                    CommandManager.argument("faction", StringArgumentType.greedyString())
                    .suggests(Suggests.allFactions())
                    .executes(this::enemy)
                )
            )
            .build();
    }
    
}