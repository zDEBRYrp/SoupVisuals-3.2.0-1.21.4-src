package padej.soup.core.perftest;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class PerfTestCommand {
   private PerfTestCommand() {
   }

   public static void register() {
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> dispatcher.register(
               (LiteralArgumentBuilder)ClientCommandManager.literal("soup")
                  .then(
                     ClientCommandManager.literal("perf-test")
                        .then(
                           ((RequiredArgumentBuilder)ClientCommandManager.argument("seconds", IntegerArgumentType.integer(1, 600))
                                 .executes(ctx -> startTest(ctx, IntegerArgumentType.getInteger(ctx, "seconds"), null)))
                              .then(
                                 ClientCommandManager.argument("testName", StringArgumentType.string())
                                    .executes(
                                       ctx -> startTest(ctx, IntegerArgumentType.getInteger(ctx, "seconds"), StringArgumentType.getString(ctx, "testName"))
                                    )
                              )
                        )
                  )
            )
         );
   }

   private static int startTest(CommandContext<FabricClientCommandSource> ctx, int seconds, String rawName) {
      if (PerfTestSession.isActive()) {
         ((FabricClientCommandSource)ctx.getSource()).sendError(Text.literal("Тест уже идёт — дождись завершения."));
         return 0;
      } else {
         String sanitized = sanitizeName(rawName);
         PerfTestSession.start(seconds * 1000L, sanitized);
         String suffix = sanitized != null ? " · файл: §f" + sanitized + ".html" : "";
         ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Text.literal("§a[perf] Запущен тест на §e" + seconds + "§a сек." + suffix));
         return 1;
      }
   }

   private static String sanitizeName(String raw) {
      if (raw == null) {
         return null;
      } else {
         String trimmed = raw.trim();
         if (trimmed.isEmpty()) {
            return null;
         } else {
            StringBuilder sb = new StringBuilder(trimmed.length());

            for (int i = 0; i < trimmed.length(); i++) {
               char c = trimmed.charAt(i);
               boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-';
               sb.append(ok ? c : '_');
            }

            String s = sb.toString();
            return s.isEmpty() ? null : s;
         }
      }
   }
}
