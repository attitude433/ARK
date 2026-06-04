package com.attitude433.ark.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 플레이어별 능력치·성장·자원 컨테이너 (Data Attachment에 직렬화).
 *
 * <p>저장 항목:
 * <ul>
 *   <li>{@code values}: 능력치 id → 정수값 (default 적용분 + 분배분)</li>
 *   <li>{@code highestLevel}: 도달한 최고 경험치 레벨 (영구·단조 증가)</li>
 *   <li>{@code pointsUsed}: 분배에 쓴 포인트 합</li>
 *   <li>{@code currentMana}: 현재 마나 (재생·소모로 변동, 영구 저장)</li>
 *   <li>{@code lastCombatTick}: 마지막 전투 행동(가하거나 받음) 틱 — 마나 재생률 결정</li>
 * </ul>
 */
public class PlayerStats {
    public static final int POINTS_PER_LEVEL = 3;

    public static final Codec<PlayerStats> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                    .optionalFieldOf("values", Map.of()).forGetter(PlayerStats::asMap),
            Codec.INT.optionalFieldOf("highest_level", 0).forGetter(PlayerStats::highestLevel),
            Codec.INT.optionalFieldOf("points_used", 0).forGetter(PlayerStats::pointsUsed),
            Codec.FLOAT.optionalFieldOf("current_mana", 0.0f).forGetter(PlayerStats::currentMana),
            Codec.LONG.optionalFieldOf("last_combat_tick", 0L).forGetter(PlayerStats::lastCombatTick)
    ).apply(inst, PlayerStats::fromCodec));

    private final Map<ResourceLocation, Integer> values;
    private int highestLevel;
    private int pointsUsed;
    private float currentMana;
    private long lastCombatTick;

    public PlayerStats() {
        this.values = new HashMap<>();
        this.highestLevel = 0;
        this.pointsUsed = 0;
        this.currentMana = 0.0f;
        this.lastCombatTick = 0L;
    }

    private PlayerStats(Map<ResourceLocation, Integer> values, int highestLevel, int pointsUsed,
                        float currentMana, long lastCombatTick) {
        this.values = new HashMap<>(values);
        this.highestLevel = highestLevel;
        this.pointsUsed = pointsUsed;
        this.currentMana = currentMana;
        this.lastCombatTick = lastCombatTick;
    }

    private static PlayerStats fromCodec(Map<ResourceLocation, Integer> values, int highestLevel,
                                        int pointsUsed, float currentMana, long lastCombatTick) {
        return new PlayerStats(values, highestLevel, pointsUsed, currentMana, lastCombatTick);
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

    public void invest(ResourceLocation id, int amount) {
        values.merge(id, amount, Integer::sum);
        pointsUsed += amount;
    }

    // --- 마나 ---

    public float currentMana() {
        return currentMana;
    }

    public void setCurrentMana(float mana) {
        this.currentMana = mana;
    }

    public long lastCombatTick() {
        return lastCombatTick;
    }

    public void markCombat(long tick) {
        this.lastCombatTick = tick;
    }
}
