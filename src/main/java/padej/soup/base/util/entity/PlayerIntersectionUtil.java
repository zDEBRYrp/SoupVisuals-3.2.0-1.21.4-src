package padej.soup.base.util.entity;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import padej.soup.base.QuickImports;
import padej.soup.implement.menu.MenuScreen;

public final class PlayerIntersectionUtil implements QuickImports {
   public static String getHealthString(float hp) {
      return String.format("%.1f", hp).replace(",", ".").replace(".0", "");
   }

   public static float getHealth(LivingEntity entity) {
      float hp = entity.getHealth() + entity.getAbsorptionAmount();
      return MathHelper.clamp(hp, 0.0F, entity.getMaxHealth());
   }

   public static Type getKeyType(int key) {
      return key < 8 ? Type.MOUSE : Type.KEYSYM;
   }

   public static boolean isChat(Screen screen) {
      return screen instanceof ChatScreen;
   }

   public static boolean isMenu(Screen screen) {
      return screen instanceof MenuScreen;
   }

   public static boolean isChatOrMenu(Screen screen) {
      return isChat(screen) || isMenu(screen);
   }

   public static boolean nullCheck() {
      return mc.player == null || mc.world == null;
   }

   private PlayerIntersectionUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
