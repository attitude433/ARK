package com.attitude433.ark.stat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 데이터팩으로 정의하는 1차 능력치(또는 하위 능력치) 타입.
 *
 * <p>예: {@code data/ark/ark/primary_stat/strength.json}
 * <pre>{@code
 * {
 *   "display_name": "Strength",
 *   "default_value": 5,
 *   "min": 0,
 *   "max": 100
 * }
 * }</pre>
 *
 * <p>실제 게임플레이 효과는 {@link DerivedRule}이 정함(이 record는 메타데이터만).
 */
public record PrimaryStat(String displayName, int defaultValue, int min, int max) {
    public static final Codec<PrimaryStat> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("display_name").forGetter(PrimaryStat::displayName),
            Codec.INT.fieldOf("default_value").forGetter(PrimaryStat::defaultValue),
            Codec.INT.optionalFieldOf("min", 0).forGetter(PrimaryStat::min),
            Codec.INT.optionalFieldOf("max", 100).forGetter(PrimaryStat::max)
    ).apply(inst, PrimaryStat::new));

    /** 값을 [min, max] 범위로 클램프. */
    public int clamp(int raw) {
        return Math.max(min, Math.min(max, raw));
    }
}
