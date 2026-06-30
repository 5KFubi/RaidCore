package me.fivekfubi.raidcore.Dialogue;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.fivekfubi.raidcore.Dialogue.Data.DATA_Dialogue;
import me.fivekfubi.raidcore.Dialogue.Data.DATA_Dialogue_Button;
import me.fivekfubi.raidcore.Dialogue.Data.DATA_Dialogue_Input;
import me.fivekfubi.raidcore.Holder.HOLDER;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Subst;

import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;

@SuppressWarnings("UnstableApiUsage")
public class MANAGER_Dialogue implements Listener {

    // player -> (key_string -> [plugin_name, path_string, button_id, holder])
    public final Map<UUID, Map<String, Object[]>> pending = new HashMap<>();

    public void open(String plugin_name, Player player, String path_string, HOLDER holder) {
        DATA_Dialogue data = m_dialogue_loader.get_dialogue_data(plugin_name, path_string);
        if (data == null) {
            utils.error_message("<white>Failed to open dialogue: <yellow>" + path_string, null);
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Object[]> player_keys = pending.computeIfAbsent(uuid, k -> new HashMap<>());

        // --- build action buttons ---
        List<ActionButton> action_buttons = new ArrayList<>();
        for (DATA_Dialogue_Button btn : data.buttons) {
            ActionButton ab = build_action_button(plugin_name, data.path_string, btn, player_keys, holder);
            action_buttons.add(ab);
        }

        // --- exit button ---
        ActionButton exit_action_button = null;
        if (data.exit_button != null) {
            exit_action_button = build_action_button(plugin_name, data.path_string, data.exit_button, player_keys, holder);
        }

        // --- base ---
        DialogBase.Builder base_builder = DialogBase.builder(
                m_placeholder.replace_placeholders_component(data.title, holder)
        ).canCloseWithEscape(data.can_close_escape);

        if (data.external_title != null) {
            base_builder.externalTitle(m_placeholder.replace_placeholders_component(data.external_title, holder));
        }

        // after-action
        if (data.after_action != null) {
            DialogBase.DialogAfterAction after = switch (data.after_action) {
                case "none" -> DialogBase.DialogAfterAction.NONE;
                default     -> DialogBase.DialogAfterAction.CLOSE;
            };
            base_builder.afterAction(after);
        }

        // body
        List<DialogBody> body_parts = new ArrayList<>();
        if (data.body != null) {
            for (String line : data.body) {
                body_parts.add(DialogBody.plainMessage(m_placeholder.replace_placeholders_component(line, holder)));
            }
        }
        if (data.body_item != null) {
            try {
                Material mat = Material.valueOf(data.body_item.toUpperCase());
                body_parts.add(DialogBody.item(new ItemStack(mat)).build());
            } catch (Exception ignored) {}
        }
        if (!body_parts.isEmpty()) base_builder.body(body_parts);

        // inputs
        if (data.inputs != null && !data.inputs.isEmpty()) {
            List<io.papermc.paper.registry.data.dialog.input.DialogInput> dialog_inputs = new ArrayList<>();
            for (DATA_Dialogue_Input inp : data.inputs) {
                io.papermc.paper.registry.data.dialog.input.DialogInput di = build_input(inp);
                if (di != null) dialog_inputs.add(di);
            }
            if (!dialog_inputs.isEmpty()) base_builder.inputs(dialog_inputs);
        }

        DialogBase base = base_builder.build();

        // --- type ---
        DialogType dialog_type = switch (data.type) {
            case "notice" -> action_buttons.isEmpty()
                    ? DialogType.notice()
                    : DialogType.notice(action_buttons.get(0));
            case "confirmation" -> {
                ActionButton yes = !action_buttons.isEmpty()
                        ? action_buttons.get(0)
                        : ActionButton.builder(Component.text("OK")).build();
                ActionButton no = action_buttons.size() > 1
                        ? action_buttons.get(1)
                        : ActionButton.builder(Component.text("Cancel")).build();
                yield DialogType.confirmation(yes, no);
            }
            default -> DialogType.multiAction(action_buttons, exit_action_button, data.columns);
        };

        Dialog dialog = Dialog.create(factory ->
                factory.empty()
                        .base(base)
                        .type(dialog_type)
        );

        player.showDialog(dialog);
    }

    private ActionButton build_action_button(
            String plugin_name,
            String path_string,
            DATA_Dialogue_Button btn,
            Map<String, Object[]> player_keys,
            HOLDER holder
    ) {
        String map_key = build_key_string(plugin_name, path_string, btn.id);
        player_keys.put(map_key, new Object[]{ plugin_name, path_string, btn.id, holder });

        Component label   = m_placeholder.replace_placeholders_component(btn.label, holder);
        Component tooltip = btn.tooltip != null
                ? m_placeholder.replace_placeholders_component(btn.tooltip, holder)
                : null;

        @Subst("plugin") String namespace = CORE.getName().toLowerCase();
        @Subst("plugin_path_button") String value = map_key.replace("/", "_");
        Key action_key = Key.key(namespace, value);

        ActionButton.Builder ab = ActionButton.builder(label)
                .width(btn.width)
                .action(DialogAction.customClick(action_key, null));
        if (tooltip != null) ab.tooltip(tooltip);
        return ab.build();
    }

    private io.papermc.paper.registry.data.dialog.input.DialogInput build_input(DATA_Dialogue_Input inp) {
        try {
            Component label = m_placeholder.replace_placeholders_component(inp.label, null);
            return switch (inp.type) {
                case "bool" -> DialogInput.bool(inp.key, label).build();
                case "single_option" -> {
                    if (inp.options == null || inp.options.isEmpty()) yield null;
                    List<io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry> opts = new ArrayList<>();
                    for (String opt : inp.options) {
                        opts.add(io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry.create(opt, Component.text(opt), false));
                    }
                    yield DialogInput.singleOption(inp.key, label, opts).build();
                }
                case "text" -> DialogInput.text(inp.key, label).build();
                case "number_range" -> {
                    var builder = DialogInput.numberRange(inp.key, label, inp.min, inp.max)
                            .step(inp.step)
                            .initial(inp.initial)
                            .width(inp.width);
                    if (inp.label_format != null) builder.labelFormat(inp.label_format);
                    yield builder.build();
                }
                default -> null;
            };
        } catch (Exception e) {
            utils.error_message("<red>Failed to build dialog input: " + inp.key, e);
            return null;
        }
    }

    public void close(Player player) {
        player.closeDialog();
        pending.remove(player.getUniqueId());
    }

    @EventHandler
    public void onDialogClick(PlayerCustomClickEvent event) {
        if (!(event.getCommonConnection() instanceof io.papermc.paper.connection.PlayerGameConnection conn)) return;
        Player player = conn.getPlayer();
        UUID uuid = player.getUniqueId();

        Map<String, Object[]> player_keys = pending.get(uuid);
        if (player_keys == null) return;

        Object[] entry = null;
        for (Map.Entry<String, Object[]> e : player_keys.entrySet()) {
            if (event.getIdentifier().value().equals(e.getKey().replace("/", "_"))) {
                entry = e.getValue();
                break;
            }
        }
        if (entry == null) return;

        String plugin_name = (String) entry[0];
        String path_string = (String) entry[1];
        String button_id   = (String) entry[2];
        HOLDER holder      = (HOLDER) entry[3];

        pending.remove(uuid);

        DATA_Dialogue data = m_dialogue_loader.get_dialogue_data(plugin_name, path_string);
        if (data == null) return;

        // collect input values into a map + holder
        DialogResponseView response_view = event.getDialogResponseView();
        Map<String, Object> collected = new HashMap<>();

        if (response_view != null && data.inputs != null) {
            for (DATA_Dialogue_Input inp : data.inputs) {
                try {
                    switch (inp.type) {
                        case "bool"          -> collected.put(inp.key, response_view.getBoolean(inp.key));
                        case "text"          -> collected.put(inp.key, response_view.getText(inp.key));
                        case "single_option" -> collected.put(inp.key, response_view.getText(inp.key));
                        case "number_range"  -> collected.put(inp.key, response_view.getFloat(inp.key));
                    }
                } catch (Exception ignored) {}
            }
            collected.forEach((k, v) -> holder.set("input_" + k, v));
        }

        // NEW: forward to MANAGER_Input if this player has a pending request
        if (m_input.pending.containsKey(uuid)) {
            m_input.submit(player, collected);
        }

        // find and execute the clicked button
        List<DATA_Dialogue_Button> all_buttons = new ArrayList<>(data.buttons);
        if (data.exit_button != null) all_buttons.add(data.exit_button);

        for (DATA_Dialogue_Button btn : all_buttons) {
            if (!btn.id.equals(button_id)) continue;
            if (btn.then == null || btn.then.isEmpty()) break;

            HOLDER exec_holder = new HOLDER(new HashMap<>(holder.get_data()));
            exec_holder.set(NKEY.player.getKey(), player);

            m_executable.execute(
                    plugin_name,
                    player,
                    true,
                    "DIALOG_CLICK",
                    null,
                    null,
                    btn.then,
                    exec_holder
            );
            break;
        }
    }

    private String build_key_string(String plugin_name, String path_string, String button_id) {
        return (plugin_name + "/" + path_string + "/" + button_id).toLowerCase(Locale.ROOT);
    }
}