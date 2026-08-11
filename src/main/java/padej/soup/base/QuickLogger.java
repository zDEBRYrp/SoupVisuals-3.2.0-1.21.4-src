package padej.soup.base;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public interface QuickLogger {
   DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

   static Text getPrefix() {
      String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
      MutableText timeText = Text.literal(timestamp);
      timeText.setStyle(timeText.getStyle().withColor(Formatting.DARK_GRAY));
      MutableText brandText = Text.literal(" │ ");
      brandText.append(Text.literal("SOUP").styled(style -> style.withColor(Formatting.RED).withBold(true)));
      brandText.append(Text.literal(" │ ").styled(style -> style.withColor(Formatting.DARK_GRAY)));
      MutableText prefix = Text.literal("");
      prefix.append(timeText);
      prefix.append(brandText);
      return prefix;
   }

   default void logDirect(Text... components) {
      MutableText component = Text.literal("");
      component.append(getPrefix());
      Arrays.asList(components).forEach(component::append);
      if (MinecraftClient.getInstance().player != null) {
         MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(component);
      }
   }

   default void logDirect(String message, Formatting color) {
      Stream.of(message.split("\n")).forEach(line -> {
         MutableText component = Text.literal(line.replace("\t", "    "));
         component.setStyle(component.getStyle().withColor(color));
         this.logDirect(component);
      });
   }

   default void logDirect(String message) {
      this.logDirect(message, Formatting.GRAY);
   }
}
