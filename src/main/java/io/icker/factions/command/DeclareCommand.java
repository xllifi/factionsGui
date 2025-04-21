package io.icker.factions.command;

import java.util.Locale;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.icker.factions.api.events.RelationshipEvents;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.ui.DeclareGui;
import io.icker.factions.util.Command;
import io.icker.factions.util.Message;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.server.translations.api.Localization;

public class DeclareCommand implements Command {
    private int open(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        // Show UI
        new DeclareGui(player, null);

        return 1;
    }

    private int ally(CommandContext<ServerCommandSource> context) {
        return updateRelationship(context, Relationship.Status.ALLY);
    }

    private int neutral(CommandContext<ServerCommandSource> context) {
        return updateRelationship(context, Relationship.Status.NEUTRAL);
    }

    private int enemy(CommandContext<ServerCommandSource> context) {
        return updateRelationship(context, Relationship.Status.ENEMY);
    }

    private int updateRelationship(CommandContext<ServerCommandSource> context, Relationship.Status status) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String name = StringArgumentType.getString(context, "faction");
        Faction faction = Faction.getByName(name);

        try {
            updateRelationship(player, faction, status);
        } catch (Exception e) {
            new Message(e.getMessage()).fail().send(player, false);
            return 0;
        }
        return 1;
    }

    public static void updateRelationship(
            ServerPlayerEntity player,
            Faction faction,
            Relationship.Status status) throws Exception {

        if (faction == null) {
            throw new Exception(Localization.raw("factions.command.declare.fail.nonexistent_faction", player));
        }

        Faction sourceFaction = Command.getUser(player).getFaction();

        if (sourceFaction.equals(faction)) {
            throw new Exception(Localization.raw("factions.command.declare.fail.own_faction", player));
        }

        if (sourceFaction.getRelationship(faction.getID()).status == status) {
            throw new Exception(Localization.raw("factions.command.declare.fail.no_change", player));
        }

        Relationship.Status mutual = null;

        if (sourceFaction.getRelationship(faction.getID()).status == faction
                .getRelationship(sourceFaction.getID()).status) {
            mutual = sourceFaction.getRelationship(faction.getID()).status;
        }

        Relationship rel = new Relationship(faction.getID(), status);
        Relationship rev = faction.getRelationship(sourceFaction.getID());
        sourceFaction.setRelationship(rel);

        RelationshipEvents.NEW_DECLARATION.invoker().onNewDecleration(rel);

        MutableText msgStatus = rel.status == Relationship.Status.ALLY
                ? Text.translatable("factions.command.declare.success.status.ally").formatted(Formatting.GREEN)
                : rel.status == Relationship.Status.ENEMY
                        ? Text.translatable("factions.command.declare.success.status.enemy").formatted(Formatting.RED)
                        : Text.translatable("factions.command.declare.success.status.neutral");

        if (rel.status == rev.status) {
            RelationshipEvents.NEW_MUTUAL.invoker().onNewMutual(rel);
            new Message(
                    Text.translatable("factions.command.declare.success.mutual", msgStatus, faction.getName()))
                    .send(sourceFaction);
            new Message(
                    Text.translatable("factions.command.declare.success.mutual", msgStatus, sourceFaction.getName()))
                    .send(faction);
            return;
        } else if (mutual != null) {
            RelationshipEvents.END_MUTUAL.invoker().onEndMutual(rel, mutual);
        }

        new Message(Text.translatable("factions.command.declare.success.actor", faction.getName(), msgStatus))
                .send(sourceFaction);

        if (rel.status != Relationship.Status.NEUTRAL)
            new Message(
                    Text.translatable("factions.command.declare.success.subject", sourceFaction.getName(), msgStatus))
                    .hover(Text.translatable("factions.command.declare.success.subject.hover"))
                    .click(String.format("/factions declare %s %s",
                            rel.status.toString().toLowerCase(Locale.ROOT),
                            sourceFaction.getName()))
                    .send(faction);
    }

    @Override
    public LiteralCommandNode<ServerCommandSource> getNode() {
        return CommandManager
                .literal("declare").requires(
                        Requires.isLeader())
                .executes(this::open)
                .then(
                        CommandManager.literal("ally")
                                .requires(Requires.hasPerms("factions.declare.ally", 0))
                                .then(
                                        CommandManager.argument("faction", StringArgumentType.greedyString())
                                                .suggests(Suggests.allFactions(false)).executes(this::ally)))
                .then(
                        CommandManager.literal("neutral")
                                .requires(Requires.hasPerms("factions.declare.neutral", 0))
                                .then(
                                        CommandManager.argument("faction", StringArgumentType.greedyString())
                                                .suggests(Suggests.allFactions(false)).executes(this::neutral)))
                .then(
                        CommandManager.literal("enemy")
                                .requires(Requires.hasPerms("factions.declare.enemy", 0))
                                .then(
                                        CommandManager.argument("faction", StringArgumentType.greedyString())
                                                .suggests(Suggests.allFactions(false)).executes(this::enemy)))
                .build();
    }

}
