package com.attitude433.ark.engine;

import com.attitude433.ark.Ark;
import com.attitude433.ark.player.ArkAttachments;
import com.attitude433.ark.player.PlayerStats;
import com.attitude433.ark.registry.ArkRegistries;
import com.attitude433.ark.stat.DerivedRule;
import com.attitude433.ark.stat.PrimaryStat;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Optional;

/**
 * 스탯 코어의 심장 — 능력치 값 변경 → 관련 파생 규칙 재계산 → AttributeModifier 교체.
 *
 * <p>모디파이어 ID는 안정적인 {@code ark:derived/<source_ns>__<source_path>}
 * 형태로 만들어 매번 교체(remove → add)함 → 중복·누수 없음.
 */
public final class StatEngine {
    private StatEngine() {}

    /**
     * 능력치 값 설정 + 파생 규칙 재계산·적용.
     * 클램프와 값 저장은 여기서 다 처리.
     */
    public static void setStat(ServerPlayer player, ResourceLocation statId, int rawValue) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Registry<PrimaryStat> defs = server.registryAccess().registryOrThrow(ArkRegistries.PRIMARY_STAT_KEY);
        PrimaryStat def = defs.get(statId);
        int value = (def != null) ? def.clamp(rawValue) : rawValue;

        PlayerStats data = player.getData(ArkAttachments.PLAYER_STATS);
        data.set(statId, value);

        recompute(player);
    }

    /**
     * 데이터팩에 정의된 능력치 중 PlayerStats에 *없는* 항목을 {@code default_value}로 채우고,
     * 플레이어의 현재 경험치 레벨을 {@code highestLevel}에 반영한 뒤 재계산.
     * 신규 플레이어·신규 데이터팩 능력치 발견 시·기존 월드에 모드가 추가됐을 때 호출.
     */
    public static void ensureDefaultsAndRecompute(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Registry<PrimaryStat> defs = server.registryAccess().registryOrThrow(ArkRegistries.PRIMARY_STAT_KEY);
        PlayerStats stats = player.getData(ArkAttachments.PLAYER_STATS);

        for (var entry : defs.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!stats.has(id)) {
                stats.set(id, entry.getValue().defaultValue());
            }
        }
        // 현재 경험치 레벨도 highestLevel에 반영 (모드 추가 후 첫 진입 등).
        stats.updateHighestLevel(player.experienceLevel);
        recompute(player);
    }

    /**
     * 플레이어의 모든 파생 규칙을 다시 적용.
     * (접속·리스폰·차원 이동·데이터팩 리로드 시에도 호출 예정.)
     */
    public static void recompute(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Registry<DerivedRule> rules = server.registryAccess().registryOrThrow(ArkRegistries.DERIVED_RULE_KEY);
        Registry<Attribute> attrs = server.registryAccess().registryOrThrow(Registries.ATTRIBUTE);
        PlayerStats stats = player.getData(ArkAttachments.PLAYER_STATS);

        for (var entry : rules.entrySet()) {
            ResourceLocation ruleId = entry.getKey().location();
            DerivedRule rule = entry.getValue();

            Optional<Holder.Reference<Attribute>> attrHolderOpt =
                    attrs.getHolder(ResourceKey.create(Registries.ATTRIBUTE, rule.target()));
            if (attrHolderOpt.isEmpty()) {
                Ark.LOGGER.warn("[ARK] derived_rule {} → unknown target attribute {}", ruleId, rule.target());
                continue;
            }
            AttributeInstance inst = player.getAttribute(attrHolderOpt.get());
            if (inst == null) {
                // 플레이어가 이 Attribute를 갖지 않을 수도 (해당 엔티티 종이 아님). 스킵.
                continue;
            }

            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(
                    Ark.MODID,
                    "derived/" + ruleId.getNamespace() + "__" + ruleId.getPath()
            );

            inst.removeModifier(modifierId);
            int sourceValue = stats.get(rule.source());
            double amount = sourceValue * rule.perPoint();
            if (amount != 0.0) {
                inst.addPermanentModifier(new AttributeModifier(modifierId, amount, rule.operation()));
            }
        }
    }
}
