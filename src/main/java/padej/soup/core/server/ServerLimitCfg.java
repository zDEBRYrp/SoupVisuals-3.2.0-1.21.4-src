package padej.soup.core.server;

import net.minecraft.entity.LivingEntity;
import padej.soup.base.util.entity.VisibleUtils;
import padej.soup.implement.features.modules.mctiers.Balancer;

public class ServerLimitCfg {
   private static boolean notBalancerRestricted() {
      Balancer balancer = Balancer.getInstance();
      return balancer != null && balancer.isEnabled()
         ? !balancer.isVanilla() && !balancer.isUHC() && !balancer.isSMP() && !balancer.isNethOP() && !balancer.isPot()
         : true;
   }

   private static boolean getServerAllowance(String key) {
      return !ServerConfigManager.isOnManagedServer() ? true : ServerConfigManager.getConfigValue(key, false);
   }

   public static boolean showHp() {
      boolean serverAllows = getServerAllowance("showHp");
      return serverAllows && notBalancerRestricted();
   }

   public static boolean showHp(LivingEntity entity) {
      if (entity == null) {
         return showHp();
      } else {
         return !showHp() ? false : VisibleUtils.shouldShowHp(entity);
      }
   }

   public static boolean showItemUsingProgress() {
      boolean serverAllows = getServerAllowance("showItemUsingProgress");
      return serverAllows && notBalancerRestricted();
   }

   public static boolean showItemsOverlay() {
      boolean serverAllows = getServerAllowance("showItemsOverlay");
      return serverAllows && notBalancerRestricted();
   }

   public static boolean showItems() {
      boolean serverAllows = getServerAllowance("showItems");
      return serverAllows && notBalancerRestricted();
   }

   public static boolean showFriendsTargetRender() {
      return getServerAllowance("showFriendsTargetRender");
   }

   public static boolean showElytraTrail() {
      return getServerAllowance("showElytraTrail");
   }

   public static void refresh() {
      ServerConfigManager.refresh();
   }

   public static void refreshAll() {
      ServerConfigManager.refreshAll();
   }

   public static boolean isOnManagedServer() {
      return ServerConfigManager.isOnManagedServer();
   }

   public static String getCurrentServerName() {
      return ServerConfigManager.getCurrentServerDisplayName();
   }
}
