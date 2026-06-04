package com.attitude433.ark;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * ARK — NeoForge 1.21.x 기반 RPG 모드 진입점.
 *
 * <p>설계 메모: {@code docs/design/2026-06-01-ARK-게임디자인-메모.md}
 *
 * <p>현재 상태: 스캐폴딩 직후 — 첫 슬라이스(스탯 코어, 메모 §3)부터 모듈을 붙여감.
 * 모듈 경계(예정): {@code stat / registry / player / engine / net / command}.
 */
@Mod(Ark.MODID)
public class Ark {
    public static final String MODID = "ark";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Ark(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[ARK] mod loading (NeoForge 1.21.x, design memo at docs/design/)");
        // 첫 슬라이스(스탯 코어)에서 stat/registry/player/engine/net/command 모듈 등록 예정.
    }
}
