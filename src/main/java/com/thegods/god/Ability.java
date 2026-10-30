package com.thegods.god;

import net.minecraft.server.level.ServerPlayer;

/**
 * Un potere divino.
 *
 * @param id       identificatore (usato per lingua e ricariche)
 * @param cooldown ricarica in tick
 * @param cost     energia divina consumata
 * @param ultimate se è il potere supremo del dio
 * @param caster   logica lato server; ritorna false se il lancio fallisce (nessun costo)
 */
public record Ability(String id, int cooldown, float cost, boolean ultimate, Caster caster) {

    @FunctionalInterface
    public interface Caster {
        boolean cast(ServerPlayer player, GodData data);
    }

    public String nameKey() {
        return "ability.thegods." + id;
    }

    public String descKey() {
        return "ability.thegods." + id + ".desc";
    }
}
