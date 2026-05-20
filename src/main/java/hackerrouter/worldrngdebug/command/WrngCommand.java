package hackerrouter.worldrngdebug.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import hackerrouter.worldrngdebug.RngAccessor;
import hackerrouter.worldrngdebug.mixin.LevelRandValueAccessor;
import hackerrouter.worldrngdebug.state.RngState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

import java.util.stream.StreamSupport;

public class WrngCommand {

    private static final SuggestionProvider<CommandSourceStack> DIMENSION_SUGGESTIONS = (ctx, builder) -> {
        Iterable<ServerLevel> levels = ctx.getSource().getServer().getAllLevels();
        return SharedSuggestionProvider.suggest(
            StreamSupport.stream(levels.spliterator(), false)
                .map(level -> level.dimension().identifier().getPath()),
            builder
        );
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wrng")
            .then(Commands.literal("step")
                .then(Commands.literal("random")
                    .then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .executes(ctx -> stepRandom(ctx, null))
                        .then(Commands.argument("dimension", StringArgumentType.word())
                            .suggests(DIMENSION_SUGGESTIONS)
                            .executes(ctx -> stepRandom(ctx, StringArgumentType.getString(ctx, "dimension"))))))
                .then(Commands.literal("randvalue")
                    .then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .executes(WrngCommand::stepRandValue))))
            .then(Commands.literal("set")
                .then(Commands.literal("random")
                    .then(Commands.argument("value", StringArgumentType.word())
                        .executes(ctx -> setRandom(ctx, null))
                        .then(Commands.argument("dimension", StringArgumentType.word())
                            .suggests(DIMENSION_SUGGESTIONS)
                            .executes(ctx -> setRandom(ctx, StringArgumentType.getString(ctx, "dimension"))))))
                .then(Commands.literal("randvalue")
                    .then(Commands.argument("value", StringArgumentType.word())
                        .executes(WrngCommand::setRandValue))))
            .then(Commands.literal("freeze")
                .then(Commands.literal("random")
                    .executes(ctx -> freezeRandom(ctx, null, null))
                    .then(Commands.literal("on")
                        .executes(ctx -> freezeRandom(ctx, true, null))
                        .then(Commands.argument("dimension", StringArgumentType.word())
                            .suggests(DIMENSION_SUGGESTIONS)
                            .executes(ctx -> freezeRandom(ctx, true, StringArgumentType.getString(ctx, "dimension")))))
                    .then(Commands.literal("off")
                        .executes(ctx -> freezeRandom(ctx, false, null))
                        .then(Commands.argument("dimension", StringArgumentType.word())
                            .suggests(DIMENSION_SUGGESTIONS)
                            .executes(ctx -> freezeRandom(ctx, false, StringArgumentType.getString(ctx, "dimension"))))))
                .then(Commands.literal("randvalue")
                    .executes(ctx -> freezeRandValue(ctx, null))
                    .then(Commands.literal("on").executes(ctx -> freezeRandValue(ctx, true)))
                    .then(Commands.literal("off").executes(ctx -> freezeRandValue(ctx, false)))))
            .then(Commands.literal("query").executes(WrngCommand::query))
        );
    }

    // ---- Helpers ----

    private static long parseLong(String s) {
        if (s.startsWith("0x") || s.startsWith("0X")) return Long.parseUnsignedLong(s.substring(2), 16);
        return Long.parseLong(s);
    }

    private static String dimName(ServerLevel level) {
        return level.dimension().identifier().getPath();
    }

    private static LegacyRandomSource rng(ServerLevel level) {
        return (LegacyRandomSource) ((LevelRandValueAccessor) level).getRandom();
    }

    private static ServerLevel getLevel(CommandSourceStack src, String dim) {
        if (dim == null) return src.getLevel();
        for (ServerLevel level : src.getServer().getAllLevels()) {
            Identifier id = level.dimension().identifier();
            if (id.toString().equals(dim) || id.getPath().equals(dim)) return level;
        }
        return null;
    }

    /** Create a copyable value component (GOLD color, click to copy, hover hint) */
    private static MutableComponent copyableValue(String display) {
        return Component.literal(display).withStyle(style -> style
            .withColor(ChatFormatting.GOLD)
            .withClickEvent(new ClickEvent.CopyToClipboard(display))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy"))));
    }

    /** "level.random" or "randValue" label in AQUA */
    private static MutableComponent aquaLabel(String label) {
        return Component.literal(label).withStyle(ChatFormatting.AQUA);
    }

    // ---- Commands ----

    private static int stepRandom(CommandContext<CommandSourceStack> ctx, String dim) {
        ServerLevel level = getLevel(ctx.getSource(), dim);
        if (level == null) { ctx.getSource().sendFailure(Component.literal("Unknown dimension: " + dim)); return 0; }
        int count = IntegerArgumentType.getInteger(ctx, "count");
        RngAccessor.advance(rng(level), count);
        long seed = RngAccessor.getSeed(rng(level));
        String seedStr = String.format("0x%012X", seed);
        String dn = dimName(level);
        ctx.getSource().sendSuccess(() -> Component.literal("[WRNG] Stepped ")
            .append(aquaLabel("level.random"))
            .append(Component.literal(" " + count + " times in " + dn + ", seed now: "))
            .append(copyableValue(seedStr)), false);
        return 1;
    }

    private static int stepRandValue(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int count = IntegerArgumentType.getInteger(ctx, "count");
        LevelRandValueAccessor acc = (LevelRandValueAccessor) level;
        int v = acc.getRandValue();
        for (int i = 0; i < count; i++) v = v * 3 + 1013904223;
        acc.setRandValue(v);
        String valStr = String.format("0x%08X", v);
        ctx.getSource().sendSuccess(() -> Component.literal("[WRNG] Stepped ")
            .append(aquaLabel("randValue"))
            .append(Component.literal(" " + count + " times, now: "))
            .append(copyableValue(valStr)), false);
        return 1;
    }

    private static int setRandom(CommandContext<CommandSourceStack> ctx, String dim) {
        ServerLevel level = getLevel(ctx.getSource(), dim);
        if (level == null) { ctx.getSource().sendFailure(Component.literal("Unknown dimension: " + dim)); return 0; }
        long value;
        try { value = parseLong(StringArgumentType.getString(ctx, "value")); }
        catch (NumberFormatException e) { ctx.getSource().sendFailure(Component.literal("Invalid value")); return 0; }
        RngAccessor.setSeed(rng(level), value);
        String seedStr = String.format("0x%012X", value);
        String dn = dimName(level);
        ctx.getSource().sendSuccess(() -> Component.literal("[WRNG] Set ")
            .append(aquaLabel("level.random"))
            .append(Component.literal(" to "))
            .append(copyableValue(seedStr))
            .append(Component.literal(" in " + dn)), false);
        return 1;
    }

    private static int setRandValue(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        long value;
        try { value = parseLong(StringArgumentType.getString(ctx, "value")); }
        catch (NumberFormatException e) { ctx.getSource().sendFailure(Component.literal("Invalid value")); return 0; }
        ((LevelRandValueAccessor) level).setRandValue((int) value);
        String valStr = String.format("0x%08X", (int) value);
        ctx.getSource().sendSuccess(() -> Component.literal("[WRNG] Set ")
            .append(aquaLabel("randValue"))
            .append(Component.literal(" to "))
            .append(copyableValue(valStr)), false);
        return 1;
    }

    private static int freezeRandom(CommandContext<CommandSourceStack> ctx, Boolean on, String dim) {
        ServerLevel level = getLevel(ctx.getSource(), dim);
        if (level == null) { ctx.getSource().sendFailure(Component.literal("Unknown dimension: " + dim)); return 0; }
        LegacyRandomSource r = rng(level);
        boolean newState = (on != null) ? on : !RngState.isFrozen(r);
        RngState.setFrozen(r, newState);
        String dn = dimName(level);
        MutableComponent stateComp = newState
            ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
            : Component.literal("OFF").withStyle(ChatFormatting.RED);
        ctx.getSource().sendSuccess(() -> Component.literal("[WRNG] ")
            .append(aquaLabel("level.random"))
            .append(Component.literal(" freeze "))
            .append(stateComp)
            .append(Component.literal(" in " + dn)), false);
        return 1;
    }

    private static int freezeRandValue(CommandContext<CommandSourceStack> ctx, Boolean on) {
        ServerLevel level = ctx.getSource().getLevel();
        boolean newState = (on != null) ? on : !RngState.isRandValueFrozen(level);
        RngState.setRandValueFrozen(level, newState);
        MutableComponent stateComp = newState
            ? Component.literal("ON").withStyle(ChatFormatting.GREEN)
            : Component.literal("OFF").withStyle(ChatFormatting.RED);
        ctx.getSource().sendSuccess(() -> Component.literal("[WRNG] ")
            .append(aquaLabel("randValue"))
            .append(Component.literal(" freeze "))
            .append(stateComp), false);
        return 1;
    }

    private static int query(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ctx.getSource().sendSuccess(() -> Component.literal("[WRNG] === RNG State ===").withStyle(ChatFormatting.GRAY), false);
        for (ServerLevel level : server.getAllLevels()) {
            LegacyRandomSource r = rng(level);
            long seed = RngAccessor.getSeed(r);
            boolean frozenRng = RngState.isFrozen(r);
            int rv = ((LevelRandValueAccessor) level).getRandValue();
            boolean frozenRv = RngState.isRandValueFrozen(level);

            String seedStr = String.format("0x%012X", seed);
            String rvStr = String.format("0x%08X", rv);
            String dn = dimName(level);

            MutableComponent frozenRngComp = frozenRng
                ? Component.literal("frozen").withStyle(ChatFormatting.RED)
                : Component.literal("active").withStyle(ChatFormatting.GREEN);
            MutableComponent frozenRvComp = frozenRv
                ? Component.literal("frozen").withStyle(ChatFormatting.RED)
                : Component.literal("active").withStyle(ChatFormatting.GREEN);

            ctx.getSource().sendSuccess(() -> Component.literal("[WRNG] ")
                .append(Component.literal(dn).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" | "))
                .append(aquaLabel("level.random"))
                .append(Component.literal(": "))
                .append(copyableValue(seedStr))
                .append(Component.literal(" ("))
                .append(frozenRngComp)
                .append(Component.literal(") | "))
                .append(aquaLabel("randValue"))
                .append(Component.literal(": "))
                .append(copyableValue(rvStr))
                .append(Component.literal(" ("))
                .append(frozenRvComp)
                .append(Component.literal(")")), false);
        }
        return 1;
    }
}
