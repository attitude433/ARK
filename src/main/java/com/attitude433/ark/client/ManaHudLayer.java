package com.attitude433.ark.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * 마나 HUD 레이어 — 미니멀 코드 fill (PNG 텍스처 없이 사각형 + 텍스트).
 *
 * <p>경험치 바 바로 위에 가로 막대 + 비율로 채운 파란 색 + 가운데 텍스트 {@code mana / max}.
 *
 * <p>추후 PNG 텍스처로 교체 가능 (assets/ark/textures/gui/sprites/mana_bar/ 등).
 */
public final class ManaHudLayer implements LayeredDraw.Layer {
    public static final ManaHudLayer INSTANCE = new ManaHudLayer();

    private static final int BAR_WIDTH = 182;          // 바닐라 경험치 바와 동일
    private static final int BAR_HEIGHT = 5;
    private static final int Y_OFFSET_ABOVE_XP_BAR = 9; // 경험치 바 위 (경험치 바는 화면 하단에서 y=22 근처)

    // 색 (ARGB)
    private static final int BG_COLOR = 0xC0202020;     // 배경 (어두운 회색, 반투명)
    private static final int FILL_COLOR = 0xFF3A7AD9;   // 파란 채움
    private static final int BORDER_COLOR = 0xFF000000; // 테두리
    private static final int TEXT_COLOR = 0xFFFFFFFF;   // 흰 텍스트
    private static final int TEXT_SHADOW = true ? 1 : 0;

    private ManaHudLayer() {}

    @Override
    public void render(GuiGraphics gui, DeltaTracker delta) {
        if (!ClientManaState.isReady()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        int screenW = gui.guiWidth();
        int screenH = gui.guiHeight();
        int x = (screenW - BAR_WIDTH) / 2;
        int y = screenH - 32 - Y_OFFSET_ABOVE_XP_BAR; // 경험치 바 위

        float current = ClientManaState.currentMana();
        float max = ClientManaState.maxMana();
        float ratio = max > 0 ? Math.min(1.0f, current / max) : 0.0f;
        int filledWidth = Math.round(BAR_WIDTH * ratio);

        // 테두리 (한 픽셀 외곽)
        gui.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER_COLOR);
        // 배경 (빈 부분)
        gui.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BG_COLOR);
        // 채움
        if (filledWidth > 0) {
            gui.fill(x, y, x + filledWidth, y + BAR_HEIGHT, FILL_COLOR);
        }

        // 텍스트 — 바 위에 작은 글자 가운데 정렬
        Font font = mc.font;
        String text = String.format("%.0f / %.0f", current, max);
        if (ClientManaState.inCombat()) {
            text = text + " ⚔";
        }
        int textWidth = font.width(text);
        int textX = (screenW - textWidth) / 2;
        int textY = y - 10;
        gui.drawString(font, Component.literal(text), textX, textY, TEXT_COLOR, true);
    }
}
