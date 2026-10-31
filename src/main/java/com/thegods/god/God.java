package com.thegods.god;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Definizione di una divinità: identità, poteri e passivi. */
public abstract class God {
    private final String id;
    private final int color;

    protected God(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    /** Colore principale (RGB) usato da HUD e titoli. */
    public int color() {
        return color;
    }

    public Component displayName() {
        return Component.translatable("god.thegods." + id);
    }

    public Component epithet() {
        return Component.translatable("god.thegods." + id + ".epithet");
    }

    public float maxEnergy() {
        return 100f;
    }

    public abstract List<Ability> abilities();

    /** Mosse disponibili quando il dio è trasformato nel suo Avatar (vuoto se non ne ha). */
    public List<Ability> avatarMoves() {
        return List.of();
    }

    /** I poteri attivi in questo momento: le mosse dell'Avatar se trasformato, altrimenti i poteri normali. */
    public List<Ability> activeAbilities(GodData data) {
        return data.isAvatar() && !avatarMoves().isEmpty() ? avatarMoves() : abilities();
    }

    /** Energia rigenerata per tick. */
    public float energyRegen(ServerPlayer player) {
        return 0.3f;
    }

    /** Chiamato quando il giocatore assume la forma divina. */
    public void onManifest(ServerPlayer player, GodData data) {}

    /** Chiamato quando il giocatore lascia la forma divina. */
    public void onDismiss(ServerPlayer player, GodData data) {}

    /** Chiamato ogni tick mentre la forma divina è attiva. */
    public void tickManifested(ServerPlayer player, GodData data) {}

    /** Attributi applicati in forma divina (rimossi automaticamente alla fine). */
    public abstract List<DivineModifier> modifiers();

    /**
     * Passivo che salva da un colpo mortale.
     * @return true se la morte è stata annullata
     */
    public boolean tryEscapeDeath(ServerPlayer player, GodData data) {
        return false;
    }
}
