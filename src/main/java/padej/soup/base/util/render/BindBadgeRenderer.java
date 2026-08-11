package padej.soup.base.util.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;

public final class BindBadgeRenderer implements QuickImports {
   private static final int BIND_TEXT_COLOR = ColorHelper.getArgb(255, 135, 136, 148);

   public static void draw(MatrixStack matrix, String name, float rightX, float centerY) {
      FontRenderer font = Fonts.getSize(12, Fonts.Type.INTER_BOLD);
      float stringWidth = font.getStringWidth(name);
      float badgeX = rightX - stringWidth - 15.0F;
      float badgeY = centerY - 4.5F;
      rectangle.render(
         ShapeProperties.create(matrix, badgeX, badgeY, stringWidth + 6.0F, 9.0)
            .round(2.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getGuiRectColor(1.0F))
            .build()
      );
      font.drawString(matrix, name, badgeX + 3.0F, centerY - 1.0F, BIND_TEXT_COLOR);
   }

   public static float getBadgeWidth(String name) {
      return Fonts.getSize(12, Fonts.Type.INTER_BOLD).getStringWidth(name) + 6.0F;
   }

   public static float getBadgeX(String name, float rightX) {
      return rightX - Fonts.getSize(12, Fonts.Type.INTER_BOLD).getStringWidth(name) - 15.0F;
   }

   private BindBadgeRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
