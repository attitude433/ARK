package com.attitude433.ark;

import com.attitude433.ark.engine.ManaEngine;
import com.attitude433.ark.engine.StatEngine;
import com.attitude433.ark.player.ArkAttachments;
import com.attitude433.ark.player.PlayerStats;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
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
     * 게임 이벤트 핸들러 — 데이터팩 sync·리스폰·차원이동·경험치 변화·죽음 시 처리.
     */
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME)
    public static final class LifecycleHandler {
        private LifecycleHandler() {}

        // 인내 — 능력치 → 받는 피해 % 감소. *현재는 하드코드*: 마력·제압과 함께
        // "효과 부품(damage_reduction 등) 데이터팩 표현"으로 일반화 예정.
        private static final ResourceLocation ENDURANCE_ID =
                ResourceLocation.fromNamespaceAndPath(MODID, "endurance");
        private static final float ENDURANCE_PER_POINT_PCT = 0.005f; // 1pt = 0.5% 감소 (한 스탯 몰빵 만렙 150pt = 75%)
        private static final float ENDURANCE_REDUCTION_CAP = 0.95f;  // 절대 무적 방지

        /**
         * 데이터팩 동기화 시점에 default_value 보정 + 재계산.
         * 로그인 시(서버→클라 sync)와 {@code /reload} 시 둘 다 호출됨.
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

        // Respawn·ChangeDimension은 Attribute 인스턴스가 새로 만들어지므로 모디파이어 재적용 필요.
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

        /**
         * 경험치 레벨 변화 시 highestLevel 갱신.
         * 자연 획득·{@code /xp add}는 이 이벤트로 잡힘. 단 {@code /xp set}은
         * 이벤트 없이 필드 직접 설정이라 안 잡히므로 아래 틱 폴링이 보완.
         */
        @SubscribeEvent
        public static void onLevelChange(PlayerXpEvent.LevelChange event) {
            if (!(event.getEntity() instanceof ServerPlayer sp)) return;
            int newLevel = sp.experienceLevel + event.getLevels();
            if (newLevel > 0) {
                PlayerStats stats = sp.getData(ArkAttachments.PLAYER_STATS);
                stats.updateHighestLevel(newLevel);
            }
        }

        /**
         * 매 틱: 마나 재생 (전투 상태에 따라 다름).
         * 매 20틱: highestLevel 폴링 — /xp set 같이 이벤트 없는 경로 안전망.
         */
        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            if (!(event.getEntity() instanceof ServerPlayer sp)) return;
            ManaEngine.tickRegenerate(sp);
            if (sp.tickCount % 20 == 0) {
                PlayerStats stats = sp.getData(ArkAttachments.PLAYER_STATS);
                stats.updateHighestLevel(sp.experienceLevel);
            }
        }

        /**
         * 플레이어 사망 시 XP orb 드롭 막기 (메모: "경험치·레벨은 영구 유지, 손실 없음").
         * 몹 사망 XP는 영향 없음 — 플레이어 entity일 때만 캔슬.
         */
        @SubscribeEvent
        public static void onExperienceDrop(LivingExperienceDropEvent event) {
            if (event.getEntity() instanceof Player) {
                event.setCanceled(true);
            }
        }

        /** 리스폰 복제 시 XP 필드 복원 (사망 케이스만). */
        @SubscribeEvent
        public static void onClone(PlayerEvent.Clone event) {
            if (!event.isWasDeath()) return;
            Player original = event.getOriginal();
            Player clone = event.getEntity();
            // public 필드 접근만 — Data Attachment(PlayerStats)는 copyOnDeath()로 NeoForge가 자동 처리.
            clone.experienceLevel = original.experienceLevel;
            clone.experienceProgress = original.experienceProgress;
            clone.totalExperience = original.totalExperience;
        }

        /**
         * 인내(endurance) 비율 피해 감소 + 전투 상태 추적(피해자·공격자 모두).
         * (※ endurance 부분은 데이터팩 주도 설계에서 살짝 벗어난 하드코드 —
         * 마력·제압 슬라이스에서 효과 부품으로 일반화 예정.)
         */
        @SubscribeEvent
        public static void onIncomingDamage(LivingIncomingDamageEvent event) {
            // 1) 인내 감소 (피해자가 플레이어일 때)
            if (event.getEntity() instanceof ServerPlayer victim) {
                PlayerStats stats = victim.getData(ArkAttachments.PLAYER_STATS);
                int endurance = stats.get(ENDURANCE_ID);
                if (endurance > 0) {
                    float pct = Math.min(ENDURANCE_REDUCTION_CAP, endurance * ENDURANCE_PER_POINT_PCT);
                    event.setAmount(event.getAmount() * (1.0f - pct));
                }
                stats.markCombat(victim.tickCount);
            }
            // 2) 공격자가 플레이어면 그도 전투 상태
            if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
                PlayerStats stats = attacker.getData(ArkAttachments.PLAYER_STATS);
                stats.markCombat(attacker.tickCount);
            }
        }
    }
}
