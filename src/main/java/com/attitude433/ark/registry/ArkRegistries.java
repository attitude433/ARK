package com.attitude433.ark.registry;

import com.attitude433.ark.Ark;
import com.attitude433.ark.stat.DerivedRule;
import com.attitude433.ark.stat.PrimaryStat;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * 데이터팩에서 정의되는 ARK 레지스트리 키 + 등록.
 *
 * <ul>
 *   <li>{@code ark:primary_stat} — 1차/하위 능력치 정의 ({@link PrimaryStat})</li>
 *   <li>{@code ark:derived_rule} — 능력치 → Attribute 파생 규칙 ({@link DerivedRule})</li>
 * </ul>
 *
 * 데이터팩 경로: {@code data/<pack>/ark/primary_stat/<id>.json} 등
 * (1.21 데이터팩 레지스트리 표준 — 첫 ark는 레지스트리 네임스페이스).
 */
@EventBusSubscriber(modid = Ark.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ArkRegistries {
    private ArkRegistries() {}

    public static final ResourceKey<Registry<PrimaryStat>> PRIMARY_STAT_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Ark.MODID, "primary_stat"));

    public static final ResourceKey<Registry<DerivedRule>> DERIVED_RULE_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Ark.MODID, "derived_rule"));

    @SubscribeEvent
    public static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(PRIMARY_STAT_KEY, PrimaryStat.CODEC);
        event.dataPackRegistry(DERIVED_RULE_KEY, DerivedRule.CODEC);
    }
}
