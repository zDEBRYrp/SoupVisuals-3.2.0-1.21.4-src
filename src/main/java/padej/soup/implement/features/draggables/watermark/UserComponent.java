package padej.soup.implement.features.draggables.watermark;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;

public class UserComponent implements WatermarkComponent, QuickImports {
   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      String username = mc.player != null ? mc.player.getName().getString() : mc.getSession().getUsername();
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         image.setIcon(61460).render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, username, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(username);
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      String username = mc.player != null ? mc.player.getName().getString() : mc.getSession().getUsername();
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      return iconOffset + font.getStringWidth(username);
   }

   @Override
   public String getName() {
      return "User";
   }
}
