package com.thegods.god;

import com.thegods.god.poseidon.PoseidonGod;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Il pantheon. Ogni nuovo dio si registra qui. */
public final class Gods {
    private static final Map<String, God> GODS = new LinkedHashMap<>();

    public static final PoseidonGod POSEIDON = register(new PoseidonGod());

    private static <T extends God> T register(T god) {
        GODS.put(god.id(), god);
        return god;
    }

    @Nullable
    public static God get(String id) {
        return id == null || id.isEmpty() ? null : GODS.get(id);
    }

    public static Collection<God> all() {
        return Collections.unmodifiableCollection(GODS.values());
    }

    public static Collection<String> ids() {
        return Collections.unmodifiableCollection(GODS.keySet());
    }

    /** Forza il caricamento della classe all'avvio. */
    public static void bootstrap() {}

    private Gods() {}
}
