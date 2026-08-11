package padej.soup.base.util.other;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import padej.soup.api.system.font.Fonts;
import padej.soup.base.QuickImports;
import padej.soup.base.util.entity.PlayerIntersectionUtil;

public final class StringUtil implements QuickImports {
   public static String randomString(int length) {
      return IntStream.range(0, length).mapToObj(operand -> String.valueOf((char)QuickImports.random().nextInt(97, 123))).collect(Collectors.joining());
   }

   public static String getBindName(int key) {
      return key < 0
         ? "N/A"
         : PlayerIntersectionUtil.getKeyType(key)
            .createFromCode(key)
            .getTranslationKey()
            .replace("key.keyboard.", "")
            .replace("key.mouse.", "mouse ")
            .replace(".", " ")
            .toUpperCase();
   }

   public static String wrap(String input, int width, int size) {
      String[] words = input.split(" ");
      StringBuilder output = new StringBuilder();
      float lineWidth = 0.0F;

      for (String word : words) {
         float wordWidth = Fonts.getSize(size).getStringWidth(word);
         if (lineWidth + wordWidth > width) {
            output.append("\n");
            lineWidth = 0.0F;
         } else if (lineWidth > 0.0F) {
            output.append(" ");
            lineWidth += Fonts.getSize(size).getStringWidth(" ");
         }

         output.append(word);
         lineWidth += wordWidth;
      }

      return output.toString();
   }

   public static String getUserRole() {
      if (mc.player == null) {
         return mc.getSession() == null ? "ERROR" : RoleCache.getUserRole(mc.getSession().getUsername());
      } else {
         String username = mc.player.getName().getString();
         return RoleCache.getUserRole(username);
      }
   }

   public static void refreshRoles() {
      RoleCache.forceRefresh();
   }

   public static Set<String> getDevelopers() {
      return RoleCache.getDevelopers();
   }

   public static Set<String> getYoutubers() {
      return RoleCache.getYoutubers();
   }

   public static Set<String> getTesters() {
      return RoleCache.getTesters();
   }

   public static Set<String> getPasters() {
      return RoleCache.getPasters();
   }

   public static Set<String> getCrow() {
      return RoleCache.getCrow();
   }

   public static String getDuration(int time) {
      int mins = time / 60;
      String sec = String.format("%02d", time % 60);
      return mins + ":" + sec;
   }

   public static String toRoman(int number) {
      if (number <= 0) {
         return "";
      } else if (number >= 10) {
         return "X";
      } else {
         String[] romanNumerals = new String[]{"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
         return number <= romanNumerals.length ? romanNumerals[number - 1] : String.valueOf(number);
      }
   }

   private StringUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
