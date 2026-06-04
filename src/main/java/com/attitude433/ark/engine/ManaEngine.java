package com.attitude433.ark.engine;

import com.attitude433.ark.Ark;
import com.attitude433.ark.player.ArkAttachments;
import com.attitude433.ark.player.PlayerStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 마나 자원 시스템.
 *
 * <p>설계 (메모 §2.3):
 * <ul>
 *   <li>{@link #maxMana(PlayerStats)} = {@link #BASE_MAX_MANA} + (마력+제압) × {@link #MANA_PER_INT_POINT}</li>
 *   <li>재생: 전투 외 {@link #REGEN_OUT_OF_COMBAT}/틱, 전투 중 {@link #REGEN_IN_COMBAT}/틱</li>
 *   <li>전투 상태: 최근 {@link #COMBAT_LINGER_TICKS}틱(5초) 내 데미지 주고받음</li>
 * </ul>
 */
public final class ManaEngine {
    private ManaEngine() {}

    public static final ResourceLocation ARCANA_ID =
            ResourceLocation.fromNamespaceAndPath(Ark.MODID, "arcana");
    public static final ResourceLocation DOMINION_ID =
            ResourceLocation.fromNamespaceAndPath(Ark.MODID, "dominion");

    public static final float BASE_MAX_MANA = 100.0f;
    public static final float MANA_PER_INT_POINT = 1.0f;       // 1pt = +1 max mana
    public static final float REGEN_OUT_OF_COMBAT = 0.5f;      // 매 틱 → 초당 10
    public static final float REGEN_IN_COMBAT = 0.05f;         // 매 틱 → 초당 1
    public static final int COMBAT_LINGER_TICKS = 100;         // 5초

    public static float maxMana(PlayerStats stats) {
        int intPoints = stats.get(ARCANA_ID) + stats.get(DOMINION_ID);
        return BASE_MAX_MANA + intPoints * MANA_PER_INT_POINT;
    }

    public static boolean isInCombat(ServerPlayer player, PlayerStats stats) {
        return player.tickCount - stats.lastCombatTick() < COMBAT_LINGER_TICKS;
    }

    /**
     * 매 틱 마나 재생.
     * 전투 상태에 따라 재생량이 다름. max 클램프.
     */
    public static void tickRegenerate(ServerPlayer player) {
        PlayerStats stats = player.getData(ArkAttachments.PLAYER_STATS);
        float max = maxMana(stats);
        float current = stats.currentMana();
        if (current >= max) {
            if (current > max) stats.setCurrentMana(max);  // 능력치 줄어든 경우 클램프
            return;
        }
        float regen = isInCombat(player, stats) ? REGEN_IN_COMBAT : REGEN_OUT_OF_COMBAT;
        stats.setCurrentMana(Math.min(max, current + regen));
    }

    /**
     * 마나 풀로 초기화 (신규 캐릭터·모드 첫 추가 시).
     */
    public static void fillToMax(ServerPlayer player) {
        PlayerStats stats = player.getData(ArkAttachments.PLAYER_STATS);
        stats.setCurrentMana(maxMana(stats));
    }
}
