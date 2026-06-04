package com.attitude433.ark;

import com.attitude433.ark.engine.StatEngine;
import com.attitude433.ark.player.ArkAttachments;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/**
 * ARK — NeoForge 1.21.x 기반 RPG 모드 진입점.
 *
 * <p>설계 메모: {@code docs/design/2026-06-01-ARK-게임디자인-메모.md}
 *
 * <p>모듈: {@code stat / registry / player / engine / net(추후) / command}.
 */
@Mod(Ark.MODID)
public class Ark {
    public static final String MODID = "ark";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Ark(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[ARK] mod loading (NeoForge 1.21.x)");
        ArkAttachments.register(modEventBus);
        NeoForge.EVENT_BUS.register(LifecycleHandler.class);
    }

    /**
     * 게임 이벤트 핸들러 — 접속·리스폰·차원이동 시 파생 규칙 재적용.
     * Attribute 모디파이어는 엔티티 인스턴스에 붙어 있어 매 세션마다 다시 발라야 함.
     */
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)
    public static final class LifecycleHandler {
        private LifecycleHandler() {}

        /**
         * 데이터팩 동기화 시점에 default_value 보정 + 재계산.
         * 로그인 시(서버→클라 sync)와 {@code /reload} 시 둘 다 호출됨.
         * - {@code getPlayer() != null} → 그 플레이어만 (로그인)
         * - {@code getPlayer() == null} → 모든 온라인 플레이어 (/reload)
         */
        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            if (event.getPlayer() != null) {
                StatEngine.ensureDefaultsAndRecompute(event.getPlayer());
            } else {
                for (ServerPlayer sp : event.getPlayerList().getPlayers()) {
                    StatEngine.ensureDefaultsAndRecompute(sp);
                }
            }
        }

        // Respawn·ChangeDimension은 데이터팩 sync는 안 발생하지만 Attribute 인스턴스가 새로
        // 만들어지므로 모디파이어를 다시 발라줘야 함.
        @SubscribeEvent
        public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer sp) {
                StatEngine.recompute(sp);
            }
        }

        @SubscribeEvent
        public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer sp) {
                StatEngine.recompute(sp);
            }
        }
    }
}
