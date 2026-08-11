package padej.soup.implement.features.draggables.watermark;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.modules.hud.Watermark;

public class ClientIconComponent implements WatermarkComponent, QuickImports {
   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      float imgScale = height - 7.0F;
      float offset = (height - imgScale) / 2.0F;
      if (Watermark.getInstance().getShowBackground().isValue()) {
         blur.render(
            ShapeProperties.create(matrix, x, y, height, height)
               .round(3.0F)
               .softness(1.0F)
               .thickness(2.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getBlurRect(0.7F))
               .build()
         );
      }

      int color = ColorUtil.isDarkBackground() ? ColorUtil.fade(50) : ColorUtil.getText();
      image.setIcon(61473).render(ShapeProperties.create(matrix, x + offset, y + offset, imgScale, imgScale).color(color).build());
      return height;
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      return height;
   }

   @Override
   public String getName() {
      return "Icon";
   }
}
