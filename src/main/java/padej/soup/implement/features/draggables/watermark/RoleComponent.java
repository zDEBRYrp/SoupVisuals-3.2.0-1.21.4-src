package padej.soup.implement.features.draggables.watermark;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.other.RoleCache;

public class RoleComponent implements WatermarkComponent, QuickImports {
   private static final long CACHE_TTL_MS = 1000L;
   private String cachedUsername;
   private String cachedRole;
   private int cachedIcon;
   private long cachedAtMs = 0L;

   private void refreshCacheIfStale() {
      String username = mc.player != null ? mc.player.getName().getString() : mc.getSession().getUsername();
      long now = System.currentTimeMillis();
      if (!username.equals(this.cachedUsername) || now - this.cachedAtMs >= 1000L) {
         this.cachedUsername = username;
         this.cachedRole = RoleCache.getUserRole(username);
         this.cachedIcon = getRoleIcon(this.cachedRole);
         this.cachedAtMs = now;
      }
   }

   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      this.refreshCacheIfStale();
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         image.setIcon(this.cachedIcon)
            .render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, this.cachedRole, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(this.cachedRole);
   }

   private static int getRoleIcon(String role) {
      return switch (role) {
         case "YOUTUBE" -> 61471;
         case "DEVELOPER" -> 61467;
         case "TESTER" -> 61469;
         case "PASTER" -> 61468;
         case "CROW" -> 61466;
         case "USER" -> 61470;
         default -> 61470;
      };
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      this.refreshCacheIfStale();
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      return iconOffset + font.getStringWidth(this.cachedRole);
   }

   @Override
   public String getName() {
      return "Role";
   }
}
