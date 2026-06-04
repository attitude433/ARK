package com.attitude433.ark.player;

import com.attitude433.ark.Ark;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * NeoForge Data Attachment 등록.
 * 플레이어 한 명당 {@link PlayerStats} 한 개를 영속·복제 보존.
 */
public final class ArkAttachments {
    private ArkAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Ark.MODID);

    public static final Supplier<AttachmentType<PlayerStats>> PLAYER_STATS = ATTACHMENT_TYPES.register(
            "player_stats",
            () -> AttachmentType.<PlayerStats>builder(PlayerStats::new)
                    .serialize(PlayerStats.CODEC)
                    .copyOnDeath()
                    .build()
    );

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
