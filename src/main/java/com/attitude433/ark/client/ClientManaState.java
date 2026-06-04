package com.attitude433.ark.client;

/**
 * 클라이언트 측 마나 상태 — 서버로부터 받은 가장 최신 값.
 * HUD가 매 프레임 이걸 읽어 렌더링.
 *
 * <p>단일 플레이어 기준(자기 자신의 마나만 보관). 단순 static 변수.
 */
public final class ClientManaState {
    private ClientManaState() {}

    private static float currentMana = 0.0f;
    private static float maxMana = 0.0f;
    private static boolean inCombat = false;

    public static void update(float current, float max, boolean combat) {
        currentMana = current;
        maxMana = max;
        inCombat = combat;
    }

    public static float currentMana() { return currentMana; }
    public static float maxMana() { return maxMana; }
    public static boolean inCombat() { return inCombat; }

    /** max가 양수일 때만 HUD 그릴 가치 있음(서버 동기화 받기 전엔 0). */
    public static boolean isReady() {
        return maxMana > 0.0f;
    }
}
