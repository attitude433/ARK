package com.attitude433.ark.command;

import com.attitude433.ark.Ark;
import com.attitude433.ark.engine.StatEngine;
import com.attitude433.ark.player.ArkAttachments;
import com.attitude433.ark.player.PlayerStats;
import com.attitude433.ark.registry.ArkRegistries;
import com.attitude433.ark.stat.PrimaryStat;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 개발·테스트용 명령어: {@code /ark stat <get|set|invest>}.
 *
 * <ul>
 *   <li>{@code /ark stat get} — 자신의 레벨·포인트·모든 능력치 값 표시</li>
 *   <li>{@code /ark stat invest <stat_id> <amount>} — 적립 포인트 분배 (한도 검사)</li>
 *   <li>{@code /ark stat set <stat_id> <value>} — 디버그: 포인트 시스템 우회하고 값 강제 설정</li>
 * </ul>
 */
@EventBusSubscriber(modid = Ark.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ArkCommands {
    private ArkCommands() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("ark")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("stat")
                        .then(Commands.literal("get").executes(ArkCommands::statGet))
                        .then(Commands.literal("invest")
                                .then(Commands.argument("stat", ResourceLocationArgument.id())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ArkCommands::statInvest))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("stat", ResourceLocationArgument.id())
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(ArkCommands::statSet)))));
        event.getDispatcher().register(root);
    }

    private static int statGet(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerStats stats = player.getData(ArkAttachments.PLAYER_STATS);
        Registry<PrimaryStat> defs = player.getServer().registryAccess().registryOrThrow(ArkRegistries.PRIMARY_STAT_KEY);

        ctx.getSource().sendSystemMessage(Component.literal(
                "[ARK] " + player.getName().getString()
                        + " — highest lv " + stats.highestLevel()
                        + ", points " + stats.pointsUsed() + "/" + stats.pointsEarned()
                        + " (remaining: " + stats.pointsRemaining() + ")"));

        if (defs.size() == 0) {
            ctx.getSource().sendSystemMessage(Component.literal("  (no primary_stat registered)"));
            return 0;
        }

        for (var entry : defs.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            PrimaryStat def = entry.getValue();
            int value = stats.has(id) ? stats.get(id) : def.defaultValue();
            String marker = stats.has(id) ? "" : " (default)";
            ctx.getSource().sendSystemMessage(Component.literal(
                    "  " + id + " = " + value + " / " + def.max() + marker));
        }
        return 1;
    }

    private static int statInvest(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation statId = ResourceLocationArgument.getId(ctx, "stat");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        Registry<PrimaryStat> defs = player.getServer().registryAccess().registryOrThrow(ArkRegistries.PRIMARY_STAT_KEY);
        PrimaryStat def = defs.get(statId);
        if (def == null) {
            ctx.getSource().sendFailure(Component.literal("[ARK] unknown stat: " + statId));
            return 0;
        }

        PlayerStats stats = player.getData(ArkAttachments.PLAYER_STATS);
        int remaining = stats.pointsRemaining();
        if (amount > remaining) {
            ctx.getSource().sendFailure(Component.literal(
                    "[ARK] not enough points (have " + remaining + ", need " + amount + ")"));
            return 0;
        }

        int currentValue = stats.has(statId) ? stats.get(statId) : def.defaultValue();
        int headroom = def.max() - currentValue;
        if (headroom <= 0) {
            ctx.getSource().sendFailure(Component.literal(
                    "[ARK] " + statId + " already at max (" + def.max() + ")"));
            return 0;
        }
        if (amount > headroom) {
            ctx.getSource().sendFailure(Component.literal(
                    "[ARK] would exceed max (" + def.max() + "); max invest here: " + headroom));
            return 0;
        }

        // 기존에 attachment에 없던(default만 표시된) 경우 먼저 현재값을 명시 저장 → invest 가산.
        if (!stats.has(statId)) {
            stats.set(statId, currentValue);
        }
        stats.invest(statId, amount);
        StatEngine.recompute(player);

        ctx.getSource().sendSystemMessage(Component.literal(
                "[ARK] " + statId + " +" + amount + " → " + stats.get(statId)
                        + " (remaining: " + stats.pointsRemaining() + ")"));
        return 1;
    }

    private static int statSet(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation statId = ResourceLocationArgument.getId(ctx, "stat");
        int value = IntegerArgumentType.getInteger(ctx, "value");

        Registry<PrimaryStat> defs = player.getServer().registryAccess().registryOrThrow(ArkRegistries.PRIMARY_STAT_KEY);
        if (!defs.containsKey(statId)) {
            ctx.getSource().sendFailure(Component.literal("[ARK] unknown stat: " + statId));
            return 0;
        }

        StatEngine.setStat(player, statId, value);
        PlayerStats stats = player.getData(ArkAttachments.PLAYER_STATS);
        ctx.getSource().sendSystemMessage(Component.literal("[ARK] (debug) " + statId + " = " + stats.get(statId)));
        return 1;
    }
}
