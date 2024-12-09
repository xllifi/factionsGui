package io.icker.factions.ui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.api.persistents.User;
import io.icker.factions.util.Icons;
import io.icker.factions.util.Message;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

public class AdminGui extends SimpleGui {
    /**
     * Constructs a new simple container gui for the supplied player.
     *
     * @param player                the player to server this gui to
     *                              will be treated as slots of this gui
     */
    public AdminGui(ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X1, player, false);
        User user = User.get(player.getUuid());

        // GUI
        this.setTitle(Text.literal("Factions / Admin"));

        // Bypass icon
        this.setSlot(1, new GuiElementBuilder(Items.PLAYER_HEAD)
            .setSkullOwner(user.bypass ? Icons.GUI_BYPASS_ACTIVE : Icons.GUI_BYPASS_INACTIVE)
            .setName(Text.literal(user.bypass ? "Bypass : ON" : "Bypass : OFF"))
            .setLore(List.of(Text.literal(user.bypass ? "Click to disable" : "Click to enable").setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.WHITE))))
            .setCallback((index, clickType, actionType) -> {
                user.bypass = !user.bypass;
                ItemStack item = this.getSlot(index).getItemStack();
                item.set(DataComponentTypes.ITEM_NAME, Text.literal(user.bypass ? "Bypass : ON" : "Bypass : OFF"));
                item.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal(user.bypass ? "Click to disable" : "Click to enable").setStyle(Style.EMPTY))));

                PropertyMap map = new PropertyMap();
                map.put("textures", new Property("textures", user.bypass ? Icons.GUI_BYPASS_ACTIVE : Icons.GUI_BYPASS_INACTIVE, null));
                item.set(DataComponentTypes.PROFILE, new ProfileComponent(Optional.empty(), Optional.empty(), map));

                new Message("Successfully toggled claim bypass").filler("·")
                    .add(new Message(user.bypass ? "ON" : "OFF")
                        .format(user.bypass ? Formatting.GREEN : Formatting.RED))
                    .send(player, false);
            })
        );

        // Power icon
        this.setSlot(3, new GuiElementBuilder(Items.PLAYER_HEAD)
            .setSkullOwner(Icons.GUI_POWER)
            .setName(Text.literal("Change faction's power"))
            .setLore(List.of(Text.literal("Click to enter faction's name and power").setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.WHITE))))
            .setCallback((index, clickType, actionType) -> {
                new FactionPowerInputUI(player, this);
            })
        );

        // Spoof icon
        this.setSlot(5, new GuiElementBuilder(Items.PLAYER_HEAD)
            .setSkullOwner(Icons.GUI_SPOOF)
            .setName(Text.literal("Spoof a player"))
            .setLore(List.of(Text.literal("Click to make Factions think you are that player.").setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.WHITE))))
            .setCallback((index, clickType, actionType) -> {
                new SpoofPlayerInputUI(player, user, this);
            })
        );

        // Audit icon
        this.setSlot(7, new GuiElementBuilder(Items.PLAYER_HEAD)
            .setSkullOwner(Icons.GUI_AUDIT)
            .setName(Text.literal("Run an audit"))
            .setLore(List.of(Text.literal("Click to check saved data to ensure there's no issues.").setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.WHITE))))
            .setCallback((index, clickType, actionType) -> {
                for (int i = 0; i < 4; i++) {
                    Claim.audit();
                    Faction.audit();
                    User.audit();
                }

                new Message("Successful audit").send(player, false);
            })
        );

        this.open();
    }
}
class FactionPowerInputUI extends AnvilInputGui {
    Faction selectedFaction;
    int selectedPower;

    /**
     * Constructs a new simple container gui for the supplied player.
     *
     * @param player                the player to server this gui to
     *                              will be treated as slots of this gui
     */
    public FactionPowerInputUI(ServerPlayerEntity player, AdminGui parentUi) {
        super(player, false);
        Timer timer = new Timer();

        this.setTitle(Text.literal("Specify a faction..."));

        this.setSlot(0, new GuiElementBuilder(Items.PAPER).setName(Text.literal("Faction Name")));
        this.setSlot(1, new GuiElementBuilder(Items.BARRIER)
            .setName(Text.literal("Click to Cancel"))
            .setCallback(() -> {
                this.close();
                parentUi.open();
            })
        );
        this.setSlot(2, new GuiElementBuilder(Items.SLIME_BALL)
            .setName(Text.literal("Click to Confirm"))
            .setCallback((index, clickType, actionType) -> {
                this.selectedFaction = Faction.getByName(this.getInput());
                if (this.selectedFaction == null) {
                    ItemStack item = Objects.requireNonNull(this.getSlot(index)).getItemStack();
                    item.set(DataComponentTypes.CUSTOM_NAME, Text.literal("No such faction!").formatted(Formatting.RED));
                    player.playSoundToPlayer(SoundEvent.of(Identifier.of("minecraft:item.shield.break")), SoundCategory.BLOCKS, 1, 1);
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            item.remove(DataComponentTypes.CUSTOM_NAME);
                        }
                    }, 1500);
                    return;
                }

                this.setTitle(Text.literal("Specify power..."));
                this.setDefaultInputValue("Faction Power");
                this.setSlot(0, new GuiElementBuilder(Items.PAPER).setName(Text.literal("Faction Power")));
                this.setSlot(2, new GuiElementBuilder(Items.SLIME_BALL)
                    .setName(Text.literal("Click to Confirm"))
                    .setCallback((index2, clickType2, actionType2) -> {
                        try {
                            this.selectedPower = Integer.parseInt(this.getInput());
                        } catch (Exception e) {
                            ItemStack item = Objects.requireNonNull(this.getSlot(index)).getItemStack();
                            item.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Not a number!").formatted(Formatting.RED));
                            player.playSoundToPlayer(SoundEvent.of(Identifier.of("minecraft:item.shield.break")), SoundCategory.BLOCKS, 1, 1);
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    item.remove(DataComponentTypes.CUSTOM_NAME);
                                }
                            }, 1500);
                            return;
                        }

                        selectedFaction.addAdminPower(selectedPower);

                        if (selectedPower != 0) {
                            if (selectedPower > 0) {
                                new Message("Admin %s added %d power", player.getName().getString(), selectedPower)
                                    .send(selectedFaction);
                                new Message("Added %d power", selectedPower).send(player, false);
                            } else {
                                new Message("Admin %s removed %d power", player.getName().getString(), selectedPower)
                                    .send(selectedFaction);
                                new Message("Removed %d power", selectedPower).send(player, false);
                            }
                        } else {
                            new Message("No change to power").fail().send(player, false);
                        }
                        parentUi.open();
                    })
                );
            })
        );
        this.open();
    }
}
class SpoofPlayerInputUI extends AnvilInputGui {

    /**
     * Constructs a new input gui for the provided player.
     *
     * @param player                the player to serve this gui to
     *                              will be treated as slots of this gui
     */
    public SpoofPlayerInputUI(ServerPlayerEntity player, User user, AdminGui parentUi) {
        super(player, false);
        Timer timer = new Timer();

        this.setTitle(Text.literal("Specify a player..."));

        this.setSlot(0, new GuiElementBuilder(Items.PAPER).setName(Text.literal("Faction Name")));
        this.setSlot(1, new GuiElementBuilder(Items.BARRIER)
            .setName(Text.literal("Click to Cancel"))
            .setCallback(() -> {
                this.close();
                parentUi.open();
            })
        );
        this.setSlot(2, new GuiElementBuilder(Items.SLIME_BALL)
            .setName(Text.literal("Click to Confirm"))
            .setCallback((index, clickType, actionType) -> {
                String input = this.getInput();

                User target;
                Optional<GameProfile> profile;
                if ((profile = player.getServer().getUserCache().findByName(input)).isPresent()) {
                    target = User.get(profile.get().getId());
                } else {
                    try {
                        target = User.get(UUID.fromString(input));
                    } catch (Exception e) {
                        ItemStack item = Objects.requireNonNull(this.getSlot(index)).getItemStack();
                        item.set(DataComponentTypes.CUSTOM_NAME, Text.literal(String.format("No such player %s!", input)).formatted(Formatting.RED));
                        player.playSoundToPlayer(SoundEvent.of(Identifier.of("minecraft:item.shield.break")), SoundCategory.BLOCKS, 1, 1);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                item.remove(DataComponentTypes.CUSTOM_NAME);
                            }
                        }, 1500);
                        return;
                    }
                }
                user.setSpoof(target);
                new Message("Set spoof to player %s", input).send(player, false);
                this.close();
                parentUi.open();
            })
        );
        this.open();
    }
}