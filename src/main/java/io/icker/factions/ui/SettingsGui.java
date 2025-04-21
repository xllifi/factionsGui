package io.icker.factions.ui;

import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

public class SettingsGui extends SimpleGui {

  public SettingsGui(ServerPlayerEntity player) {
    super(ScreenHandlerType.GENERIC_9X1, player, false);
  }
  
}
