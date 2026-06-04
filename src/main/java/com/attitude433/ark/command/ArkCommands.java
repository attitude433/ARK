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
 * 개발·테스트용 명령어: {@code /ark stat get|set}.
 *
 * <ul>
 *   <li>{@code /ark stat get} — 자신의 모든 스탯 값 표시</li>
 *   <li>{@code /ark stat set <stat_id> <value>} — 값 설정 후 파생 규칙 재적용</li>
 * </ul>
 *
 * 정식 GUI는 추후 슬라이스.
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

        if (defs.size() == 0) {
            ctx.getSource().sendSystemMessage(Component.literal("[ARK] no primary_stat registered (load a datapack)"));
            return 0;
        }

        ctx.getSource().sendSystemMessage(Component.literal("[ARK] stats for " + player.getName().getString() + ":"));
        for (var entry : defs.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            PrimaryStat def = entry.getValue();
            int value = stats.has(id) ? stats.get(id) : def.defaultValue();
            String marker = stats.has(id) ? "" : " (default)";
            ctx.getSource().sendSystemMessage(Component.literal("  " + id + " = " + value + marker));
        }
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
        ctx.getSource().sendSystemMessage(Component.literal("[ARK] " + statId + " = " + stats.get(statId)));
        return 1;
    }
}
