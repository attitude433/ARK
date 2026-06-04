package com.attitude433.ark.player;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 플레이어별 능력치 값 컨테이너 (Data Attachment에 직렬화됨).
 *
 * <p>저장은 {@code Map<능력치 id, 현재 정수값>} 하나만. 파생 스탯·실제 게임 효과는
 * {@link com.attitude433.ark.engine.StatEngine}이 매번 계산해 적용 (단일 진실 공급원).
 */
public class PlayerStats {
    /** Codec — Data Attachment NBT 직렬화에 사용. */
    public static final Codec<PlayerStats> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
            .xmap(PlayerStats::fromMap, PlayerStats::asMap);

    private final Map<ResourceLocation, Integer> values;

    public PlayerStats() {
        this.values = new HashMap<>();
    }

    private PlayerStats(Map<ResourceLocation, Integer> values) {
        this.values = new HashMap<>(values);
    }

    public static PlayerStats fromMap(Map<ResourceLocation, Integer> map) {
        return new PlayerStats(map);
    }

    public Map<ResourceLocation, Integer> asMap() {
        return Collections.unmodifiableMap(values);
    }

    /** 능력치 값 조회 — 없으면 0 (호출자가 기본값 결정). */
    public int get(ResourceLocation id) {
        return values.getOrDefault(id, 0);
    }

    public boolean has(ResourceLocation id) {
        return values.containsKey(id);
    }

    /** 값 설정 (호출자가 PrimaryStat.clamp로 미리 클램프해 전달하는 게 책임). */
    public void set(ResourceLocation id, int value) {
        values.put(id, value);
    }
}
