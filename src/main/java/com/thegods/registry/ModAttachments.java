package com.thegods.registry;

import com.thegods.TheGods;
import com.thegods.god.GodData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TheGods.MODID);

    /** Lo stato divino di ogni giocatore. Sopravvive alla morte. */
    public static final Supplier<AttachmentType<GodData>> GOD_DATA = ATTACHMENTS.register("god_data",
            () -> AttachmentType.serializable(GodData::new).copyOnDeath().build());

    private ModAttachments() {}
}
