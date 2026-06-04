package com.attitude433.ark.stat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 1차/하위 능력치 → 바닐라(또는 커스텀) Attribute로 흘러가는 파생 규칙.
 *
 * <p>예: {@code data/ark/ark/derived_rule/strength_attack.json}
 * <pre>{@code
 * {
 *   "source": "ark:strength",
 *   "target": "minecraft:attack_damage",
 *   "operation": "add_value",
 *   "per_point": 0.5
 * }
 * }</pre>
 *
 * <p>적용된 모디파이어 값 = {@code source 능력치의 현재 값 × per_point}.
 *
 * <p>{@code operation}은 바닐라 {@link AttributeModifier.Operation}을 그대로 차용:
 * {@code add_value}, {@code add_multiplied_base}, {@code add_multiplied_total}.
 */
public record DerivedRule(
        ResourceLocation source,
        ResourceLocation target,
        AttributeModifier.Operation operation,
        double perPoint
) {
    public static final Codec<DerivedRule> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("source").forGetter(DerivedRule::source),
            ResourceLocation.CODEC.fieldOf("target").forGetter(DerivedRule::target),
            AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(DerivedRule::operation),
            Codec.DOUBLE.fieldOf("per_point").forGetter(DerivedRule::perPoint)
    ).apply(inst, DerivedRule::new));
}
