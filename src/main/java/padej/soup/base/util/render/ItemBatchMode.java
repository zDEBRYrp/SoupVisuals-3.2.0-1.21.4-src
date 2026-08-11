package padej.soup.base.util.render;

import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.modules.hud.ItemOverlay;

public final class ItemBatchMode {
   private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
   public static boolean cachedEnabled;
   public static boolean cachedScaleEnabled;
   public static float cachedScaleValue;
   public static boolean cachedCustomDuraBar;
   public static boolean cachedDuraShadow;
   public static int[] cachedBarColors;
   public static String cachedBarAnimation;
   public static boolean cachedCustomStackCount;
   public static boolean cachedUseCustomFont;
   public static boolean cachedAddPrefix;
   public static boolean cachedEnableCustomColor;
   public static int cachedStackCountColor;
   public static int cachedTextColor;
   public static boolean cachedCustomCooldown;
   public static String cachedCooldownStyle;
   public static boolean cachedShowCooldownNumber;
   public static int cachedCooldownBaseColor;
   public static float cachedCooldownAlpha;

   public static void begin() {
      ACTIVE.set(true);
      snapshotSettings();
      ItemOverlayRenderer.prepareBatch();
   }

   public static void end() {
      ACTIVE.set(false);
   }

   public static boolean isActive() {
      return ACTIVE.get();
   }

   private static void snapshotSettings() {
      ItemOverlay io = ItemOverlay.getInstance();
      cachedEnabled = io.isEnabled();
      if (cachedEnabled) {
         cachedScaleEnabled = io.getEnableItemScale().isValue();
         cachedScaleValue = cachedScaleEnabled ? io.getItemScale().getValue() : 1.0F;
         cachedCustomDuraBar = io.getCustomDurabilityBar().isValue();
         if (cachedCustomDuraBar) {
            cachedDuraShadow = io.getDurabilityBarShadow().isValue();
            cachedBarColors = io.getCustomBarColors();
            cachedBarAnimation = io.getBarColorAnimation().getSelected();
         }

         cachedCustomStackCount = io.getCustomStackCount().isValue();
         if (cachedCustomStackCount) {
            cachedUseCustomFont = io.getUseCustomFont().isValue();
            cachedAddPrefix = io.getAddPrefix().isValue();
            cachedEnableCustomColor = io.getEnableCustomColor().isValue();
            cachedStackCountColor = cachedEnableCustomColor ? io.getStackCountColor().getColor() : ColorUtil.getText();
            cachedTextColor = ColorUtil.getText();
         }

         cachedCustomCooldown = io.getCustomCooldown().isValue();
         if (cachedCustomCooldown) {
            cachedCooldownStyle = io.getCooldownStyle().getSelected();
            cachedShowCooldownNumber = io.getShowCooldownNumber().isValue();
            if (io.getCooldownColorMode().isSelected("Custom")) {
               cachedCooldownBaseColor = io.getCooldownColor().getColor();
            } else {
               cachedCooldownBaseColor = ColorUtil.getClientColor();
            }

            cachedCooldownAlpha = io.getCooldownAlpha().getValue();
         }
      }
   }

   private ItemBatchMode() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
