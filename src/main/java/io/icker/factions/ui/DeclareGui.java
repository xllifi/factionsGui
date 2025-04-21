package io.icker.factions.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.Relationship;
import io.icker.factions.command.DeclareCommand;
import io.icker.factions.util.Message;
import io.icker.factions.util.Command.Requires;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DeclareGui extends SimpleGui {
    Runnable closeCallback;

    public DeclareGui(ServerPlayerEntity player, Runnable closeCallback) {
        super(ScreenHandlerType.GENERIC_9X1, player, false); // just fucking ignore this I dont want thi

        new SelectFactionGui(player, null, (Faction faction) -> {
            new DeclareGui(player, faction, closeCallback);
        });
    }

    public DeclareGui(ServerPlayerEntity player, Faction faction, Runnable closeCallback) {
        super(ScreenHandlerType.GENERIC_9X1, player, false);
        this.closeCallback = closeCallback;

        this.setTitle(Text.translatable("factions.gui.declare.title", faction.getName().formatted(faction.getColor())));

        for (int i = 0; i < 9; i++)
            this.setSlot(i, new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).hideTooltip());

        if (Requires.hasPerms("factions.declare.enemy", 0).test(player.getCommandSource())) {
            this.setSlot(
                2,
                new GuiElementBuilder(Items.BLADE_POTTERY_SHERD)
                    .setName(
                        Text.translatable("factions.gui.declare.enemy")
                            .formatted(Formatting.RED)
                    )
                    .setCallback(() -> {
                        try {
                            DeclareCommand.updateRelationship(player, faction, Relationship.Status.ENEMY);
                        } catch (Exception e) {
                            new Message(e.getMessage()).fail().send(player, false);
                            return;
                        }
                    })
            );
        }
        if (Requires.hasPerms("factions.declare.neutral", 0).test(player.getCommandSource())) {
            this.setSlot(
                4,
                new GuiElementBuilder(Items.MINER_POTTERY_SHERD)
                    .setName(
                        Text.translatable("factions.gui.declare.neutral")
                            .formatted(Formatting.GRAY)
                    )
                    .setCallback(() -> {
                        try {
                            DeclareCommand.updateRelationship(player, faction, Relationship.Status.NEUTRAL);
                        } catch (Exception e) {
                            new Message(e.getMessage()).fail().send(player, false);
                            return;
                        }
                    })
            );
        }
        if (Requires.hasPerms("factions.declare.ally", 0).test(player.getCommandSource())) {
            this.setSlot(
                6,
                new GuiElementBuilder(Items.HEART_POTTERY_SHERD)
                    .setName(
                        Text.translatable("factions.gui.declare.ally")
                            .formatted(Formatting.GREEN)
                    )
                    .setCallback(() -> {
                        try {
                            DeclareCommand.updateRelationship(player, faction, Relationship.Status.ALLY);
                        } catch (Exception e) {
                            new Message(e.getMessage()).fail().send(player, false);
                            return;
                        }
                    })
            );
        }

        this.open();
    }

    @Override
    public void onClose() {
        if (closeCallback == null) {
            super.onClose();
            return;
        }
        closeCallback.run();
    }
}

