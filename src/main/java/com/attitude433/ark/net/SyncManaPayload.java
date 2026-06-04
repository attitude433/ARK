package com.attitude433.ark.net;

import com.attitude433.ark.Ark;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 서버 → 클라이언트 마나 상태 동기화.
 * HUD 렌더링용. 값이 변경된 경우에만 전송 (틱마다 보내지는 마나 재생 변동도 포함).
 */
public record SyncManaPayload(float currentMana, float maxMana, boolean inCombat)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncManaPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(Ark.MODID, "sync_mana"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, SyncManaPayload::currentMana,
                    ByteBufCodecs.FLOAT, SyncManaPayload::maxMana,
                    ByteBufCodecs.BOOL, SyncManaPayload::inCombat,
                    SyncManaPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
