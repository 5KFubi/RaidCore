package me.fivekfubi.raidcore.Item.Data;

import java.util.List;

public enum KEY_Config {
    ITEM_NAME("item.name"),
    ITEM_MATERIAL("item.material"),
    ITEM_MODEL_DATA("item.model-data"),
    PLACEHOLDER_SINGLE("item.placeholders.single"),
    PLACEHOLDER_MULTIPLE("item.placeholders.multiple"),
    ATTR_STACKABLE("item.attributes.stackable"),
    ATTR_DURABILITY("item.attributes.durability"),
    ACTION_LEFT_CLICK_TEST("item.actions.event.LEFT_CLICK.test-event-action-1");

    private final String path;

    KEY_Config(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public List<String> asList() {
        return List.of(path.split("\\."));
    }
}
