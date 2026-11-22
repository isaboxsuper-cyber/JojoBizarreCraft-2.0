package com.jojomod;

import com.jojomod.data.JojoData;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JojoMod.MOD_ID);

    /** Copiado manualmente na morte (ver PlayerEvents.onClone). */
    public static final Supplier<AttachmentType<JojoData>> JOJO = ATTACHMENTS.register("jojo",
            () -> AttachmentType.builder(() -> new JojoData()).serialize(JojoData.CODEC).build());
}
