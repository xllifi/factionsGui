package io.icker.factions.ui;

import io.icker.factions.api.persistents.Faction;
import io.icker.factions.util.GuiInteract;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.server.translations.api.Localization;

public class SelectFactionGui extends InputGui {
    Runnable closeCallback;

    public SelectFactionGui(
        ServerPlayerEntity player,
        Runnable closeCallback,
        SelectFactionGuiCallback successCallback
    ) {
        super(player);
        this.closeCallback = closeCallback;

        this.setTitle(Text.translatable("factions.gui.power.setfaction.title"));
        this.setDefaultInputValue(Localization.raw("factions.gui.power.setfaction.default", player));

        this.returnBtn.setCallback(() -> {
            GuiInteract.playClickSound(player);
            this.open();
        });
        this.confirmBtn.setCallback(
            (index, clickType, actionType) -> {
                GuiInteract.playClickSound(player);
                Faction faction = Faction.getByName(this.getInput());
                if (faction == null) {
                    this.showErrorMessage(
                        Text.translatable("factions.gui.power.setfaction.fail.no_faction").formatted(Formatting.RED),
                        index
                    );
                    return;
                }
                successCallback.exec(faction);
            }
        );
        this.open();
    }

}

@FunctionalInterface
interface SelectFactionGuiCallback {
    void exec(Faction faction);
}