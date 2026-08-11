package padej.soup.base.util.compat;

import net.fabricmc.loader.api.FabricLoader;
import padej.soup.base.util.logger.LoggerUtil;

public class ModCompatibility {
   private static final boolean HOLD_MY_ITEMS_LOADED = FabricLoader.getInstance().isModLoaded("hold-my-items");
   private static final boolean MIXIN_EXTRAS_LOADED = FabricLoader.getInstance().isModLoaded("mixinextras");

   public static boolean isHoldMyItemsLoaded() {
      return HOLD_MY_ITEMS_LOADED;
   }

   public static boolean isMixinExtrasLoaded() {
      return MIXIN_EXTRAS_LOADED;
   }

   public static void printCompatibilityInfo() {
      if (HOLD_MY_ITEMS_LOADED) {
         LoggerUtil.info("hold-my-items loaded - comat active");
      }

      if (MIXIN_EXTRAS_LOADED) {
         LoggerUtil.info("mixinextras - detected");
      }
   }
}
