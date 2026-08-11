package padej.soup.base.util.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import padej.soup.core.server.ServerConfigManager;

public final class VisibleUtils {
   public static VisibleUtils.VisibilityLevel getVisibilityLevel(LivingEntity entity) {
      if (!entity.isInvisible()) {
         return VisibleUtils.VisibilityLevel.FULLY_VISIBLE;
      } else if (!entity.hasStatusEffect(StatusEffects.GLOWING) && !entity.isGlowing()) {
         boolean hasArmor = hasEquippedItems(entity);
         return hasArmor ? VisibleUtils.VisibilityLevel.PARTIALLY_VISIBLE : VisibleUtils.VisibilityLevel.FULLY_INVISIBLE;
      } else {
         return VisibleUtils.VisibilityLevel.PARTIALLY_VISIBLE;
      }
   }

   public static boolean isFullyVisible(LivingEntity entity) {
      return getVisibilityLevel(entity) == VisibleUtils.VisibilityLevel.FULLY_VISIBLE;
   }

   public static boolean isPartiallyVisible(LivingEntity entity) {
      return getVisibilityLevel(entity) == VisibleUtils.VisibilityLevel.PARTIALLY_VISIBLE;
   }

   public static boolean isFullyInvisible(LivingEntity entity) {
      return getVisibilityLevel(entity) == VisibleUtils.VisibilityLevel.FULLY_INVISIBLE;
   }

   public static boolean canBeTargeted(LivingEntity entity) {
      VisibleUtils.VisibilityLevel level = getVisibilityLevel(entity);
      String levelName = level.name();
      return ServerConfigManager.canBeTargeted(levelName);
   }

   public static boolean shouldShowSkin(LivingEntity entity) {
      VisibleUtils.VisibilityLevel level = getVisibilityLevel(entity);
      String levelName = level.name();
      return ServerConfigManager.showSkin(levelName);
   }

   public static boolean shouldShowName(LivingEntity entity) {
      VisibleUtils.VisibilityLevel level = getVisibilityLevel(entity);
      String levelName = level.name();
      return ServerConfigManager.showName(levelName);
   }

   public static boolean shouldShowHp(LivingEntity entity) {
      VisibleUtils.VisibilityLevel level = getVisibilityLevel(entity);
      String levelName = level.name();
      return ServerConfigManager.showHp(levelName);
   }

   private static boolean hasEquippedItems(LivingEntity entity) {
      for (ItemStack stack : entity.getArmorItems()) {
         if (!stack.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   public static String getDisplayName(LivingEntity entity) {
      return !shouldShowName(entity) ? "???" : entity.getName().getString();
   }

   private VisibleUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static enum VisibilityLevel {
      FULLY_VISIBLE,
      PARTIALLY_VISIBLE,
      FULLY_INVISIBLE;
   }
}
