package com.goshan.blackmark.command;

import com.goshan.blackmark.curse.Curse;
import com.goshan.blackmark.curse.CurseRegistry;
import com.goshan.blackmark.mark.MarkAffliction;
import com.goshan.blackmark.mark.MarkCapability;
import com.goshan.blackmark.mark.MarkData;
import com.goshan.blackmark.mark.MarkManager;
import com.goshan.blackmark.mark.SlotLock;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

/**
 * Admin and testing entry point. Nothing here is meant to be reachable in normal play.
 */
public final class BlackMarkCommand {

    private static final SuggestionProvider<CommandSourceStack> CURSE_IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(CurseRegistry.all().stream().map(Curse::id), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("blackmark")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> info(ctx, EntityArgument.getPlayer(ctx, "target")))))

                .then(Commands.literal("afflict")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> afflictRandom(ctx, EntityArgument.getPlayer(ctx, "target")))
                                .then(Commands.argument("curse", ResourceLocationArgument.id())
                                        .suggests(CURSE_IDS)
                                        .executes(ctx -> afflictSpecific(
                                                ctx,
                                                EntityArgument.getPlayer(ctx, "target"),
                                                ResourceLocationArgument.getId(ctx, "curse"))))))

                .then(Commands.literal("free")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> free(ctx, EntityArgument.getPlayer(ctx, "target")))))

                .then(Commands.literal("curses")
                        .executes(BlackMarkCommand::listCurses)));
    }

    private static int info(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        MarkData data = MarkCapability.of(target).orElse(null);
        if (data == null) {
            ctx.getSource().sendFailure(Component.literal("No mark capability on that player."));
            return 0;
        }

        if (!data.isMarked()) {
            ctx.getSource().sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " is unmarked.")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        long minutes = data.getCarriedTicks() / (20L * 60L);
        String curses = String.join(", ", data.curseIds().stream().map(ResourceLocation::toString).toList());
        String blocked = Arrays.toString(SlotLock.blocked(data));

        ctx.getSource().sendSuccess(() -> Component.literal(
                target.getGameProfile().getName() + " is marked."
                        + "\n  carried: " + minutes + " min"
                        + "\n  state: " + data.getState()
                        + "\n  defiance: " + data.getDefiance()
                        + "\n  slot: " + data.getMarkSlot()
                        + "\n  curses: " + curses
                        + "\n  devoured slots: " + blocked
        ).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int afflictRandom(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        MarkAffliction.afflict(target, target.getRandom());
        ctx.getSource().sendSuccess(
                () -> Component.literal("The mark has chosen " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int afflictSpecific(CommandContext<CommandSourceStack> ctx, ServerPlayer target,
                                       ResourceLocation curseId) {
        Curse curse = CurseRegistry.get(curseId);
        if (curse == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown curse: " + curseId));
            return 0;
        }

        MarkManager.afflict(target, List.of(curse));
        ctx.getSource().sendSuccess(
                () -> Component.literal(target.getGameProfile().getName() + " now carries " + curseId + "."), true);
        return 1;
    }

    private static int free(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        MarkManager.free(target, false);
        ctx.getSource().sendSuccess(
                () -> Component.literal(target.getGameProfile().getName() + " has been released."), true);
        return 1;
    }

    private static int listCurses(CommandContext<CommandSourceStack> ctx) {
        StringBuilder out = new StringBuilder("Registered curses:");
        for (Curse curse : CurseRegistry.all()) {
            out.append("\n  ").append(curse.id()).append("  (weight ").append(curse.weight()).append(')');
        }
        ctx.getSource().sendSuccess(() -> Component.literal(out.toString()).withStyle(ChatFormatting.GRAY), false);
        return CurseRegistry.all().size();
    }

    private BlackMarkCommand() {
    }
}
