package me.fivekfubi.raidcore.NKey;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

import static me.fivekfubi.raidcore.RaidCore.*;

public class MANAGER_Key {
    public final String CLUMP_KEY_EVENT         =           "clump_key_event"        ;
    public final String CLUMP_KEY_GUI           =           "clump_key_gui"          ;

    public NamespacedKey file_plugin            = generate( "file_plugin"            );
    public NamespacedKey file_path              = generate( "file_path"              );
    public NamespacedKey session_key            = generate( "session_key"            );
    public NamespacedKey session_value          = generate( SESSION_VALUE            );

    public NamespacedKey item_key               = generate( "item_key"               );
    public NamespacedKey item_slot              = generate( "item_slot"              );
    public NamespacedKey item_slot_category     = generate( "item_slot_category"     );
    public NamespacedKey item_variant           = generate( "item_variant"           );
    public NamespacedKey item_variant_id        = generate( "item_variant_id"        );
    public NamespacedKey item_stackable         = generate( "item_stackable"         );
    public NamespacedKey item_durability        = generate( "item_durability"        );

    public NamespacedKey player                 = generate( "player"                 );
    public NamespacedKey player_name            = generate( "player_name"            );
    public NamespacedKey player_uuid            = generate( "player_uuid"            );

    public NamespacedKey target                 = generate( "target"                 );
    public NamespacedKey target_name            = generate( "target_name"            );
    public NamespacedKey target_uuid            = generate( "target_uuid"            );

    public NamespacedKey eco_price              = generate( "eco_price"              );
    public NamespacedKey price_money            = generate( "price_money"            );
    public NamespacedKey price_items            = generate( "price_items"            );

    public NamespacedKey gui_item               = generate( "gui_item"               );
    public NamespacedKey gui_item_id            = generate( "gui_item_id"            );
    public NamespacedKey gui_item_group         = generate( "gui_item_group"         );
    public NamespacedKey gui_item_page          = generate( "gui_item_page"          );
    public NamespacedKey gui_item_clicked_index = generate( "gui_item_clicked_index" );
    public NamespacedKey gui_item_empty_slot    = generate( "gui_item_empty_slot"    );

    public NamespacedKey generate(){
        return generate(CORE, UUID.randomUUID().toString());
    }
    public NamespacedKey generate(String key){
        return generate(CORE, key);
    }
    public NamespacedKey generate(JavaPlugin plugin, String key){
        NamespacedKey n = new NamespacedKey(plugin, key);
        return n;
    }


    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------
    /// TODO: ----------------------------------------------------------------------------------------------------------


    public final String attribute_item_stackable            = "item.attributes.stackable";
    public final String attribute_item_durability           = "item.attributes.durability";
    public final String attribute_item_on_break_replacement = "item.attributes.on-break-replacement";
    public final String attribute_item_allow_enchant        = "item.attributes.allow-enchant";
    public final String attribute_item_allow_anvil          = "item.attributes.allow-anvil";
    public final String attribute_item_is_currency          = "item.attributes.currency";
}
