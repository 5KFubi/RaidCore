package me.fivekfubi.raidcore.Entity;

import org.bukkit.entity.Entity;

import java.util.function.BiConsumer;

public class ENTITY_Ticker<T extends Entity> {
    public final ENTITY_Part<T> part;
    public final int refresh_rate;
    public final BiConsumer<T, CUSTOM_Entity> handler;
    public int counter = 0;

    public ENTITY_Ticker(ENTITY_Part<T> part, int refresh_rate, BiConsumer<T, CUSTOM_Entity> handler) {
        this.part = part;
        this.refresh_rate = refresh_rate;
        this.handler = handler;
    }
}
