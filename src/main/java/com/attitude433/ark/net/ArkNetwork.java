package com.attitude433.ark.net;

import com.attitude433.ark.Ark;
import com.attitude433.ark.client.ClientManaState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * ARK 네트워크 페이로드 등록 + 핸들러.
 *
 * <p>현재: 마나 상태 동기화(서버→클라, play 단계, S2C only).
 */
@EventBusSubscriber(modid = Ark.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ArkNetwork {
    private ArkNetwork() {}

    public static final String VERSION = "1";

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Ark.MODID).versioned(VERSION);
        registrar.playToClient(
                SyncManaPayload.TYPE,
                SyncManaPayload.STREAM_CODEC,
                ArkNetwork::handleSyncMana);
    }

    private static void handleSyncMana(SyncManaPayload payload, IPayloadContext ctx) {
        // 메인 스레드에서 클라 상태 갱신.
        ctx.enqueueWork(() -> ClientManaState.update(payload.currentMana(), payload.maxMana(), payload.inCombat()));
    }
}
