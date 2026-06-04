package com.attitude433.ark.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 플레이어별 능력치 + 성장 상태 컨테이너 (Data Attachment에 직렬화).
 *
 * <p>저장 항목:
 * <ul>
 *   <li>{@code values}: {@code Map<능력치 id, 현재 정수값>} — default_value 자동 적용분 + 분배분 포함</li>
 *   <li>{@code highestLevel}: 플레이어가 도달한 최고 경험치 레벨 (영구·단조 증가)</li>
 *   <li>{@code pointsUsed}: 능력치에 분배에 쓴 포인트 합</li>
 * </ul>
 *
 * <p>적립 포인트 = {@code highestLevel × POINTS_PER_LEVEL}, 남은 = 적립 − pointsUsed.
 * 파생 스탯·실제 게임 효과는 {@link com.attitude433.ark.engine.StatEngine}이 계산.
 */
public class PlayerStats {
    public static final int POINTS_PER_LEVEL = 3;

    public static final Codec<PlayerStats> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                    .optionalFieldOf("values", Map.of()).forGetter(PlayerStats::asMap),
            Codec.INT.optionalFieldOf("highest_level", 0).forGetter(PlayerStats::highestLevel),
            Codec.INT.optionalFieldOf("points_used", 0).forGetter(PlayerStats::pointsUsed)
    ).apply(inst, PlayerStats::fromCodec));

    private final Map<ResourceLocation, Integer> values;
    private int highestLevel;
    private int pointsUsed;

    public PlayerStats() {
        this.values = new HashMap<>();
        this.highestLevel = 0;
        this.pointsUsed = 0;
    }

    private PlayerStats(Map<ResourceLocation, Integer> values, int highestLevel, int pointsUsed) {
        this.values = new HashMap<>(values);
        this.highestLevel = highestLevel;
        this.pointsUsed = pointsUsed;
    }

    private static PlayerStats fromCodec(Map<ResourceLocation, Integer> values, int highestLevel, int pointsUsed) {
        return new PlayerStats(values, highestLevel, pointsUsed);
    }

    // --- 능력치 값 ---

    public Map<ResourceLocation, Integer> asMap() {
        return Collections.unmodifiableMap(values);
    }

    public int get(ResourceLocation id) {
        return values.getOrDefault(id, 0);
    }

    public boolean has(ResourceLocation id) {
        return values.containsKey(id);
    }

    /** 능력치 값을 강제 설정 (포인트 시스템 우회 — 디버그·default 적용용). */
    public void set(ResourceLocation id, int value) {
        values.put(id, value);
    }

    // --- 성장 ---

    public int highestLevel() {
        return highestLevel;
    }

    /**
     * 도달 레벨이 더 높으면 영구 갱신 (영구·단조 증가).
     * @return 실제로 증가했으면 true
     */
    public boolean updateHighestLevel(int current) {
        if (current > highestLevel) {
            highestLevel = current;
            return true;
        }
        return false;
    }

    public int pointsUsed() {
        return pointsUsed;
    }

    public int pointsEarned() {
        return highestLevel * POINTS_PER_LEVEL;
    }

    public int pointsRemaining() {
        return Math.max(0, pointsEarned() - pointsUsed);
    }

    /** 능력치에 포인트 분배 — 값에 amount 더하고 pointsUsed 증가. 한도 검사는 호출자 책임. */
    public void invest(ResourceLocation id, int amount) {
        values.merge(id, amount, Integer::sum);
        pointsUsed += amount;
    }
}
