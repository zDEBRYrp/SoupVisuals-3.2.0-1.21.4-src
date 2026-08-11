package padej.soup.implement.features.draggables.watermark;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.base.util.color.ColorUtil;

public class ClientNameComponent implements WatermarkComponent {
   private static final String CLIENT_NAME = "SOUP VISUALS";

   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      int baseColor = ColorUtil.getClientColor();
      font.drawWaveGradientString(matrix, "SOUP VISUALS", x, y + 6.5F, ColorUtil.darker(baseColor), ColorUtil.lighter(baseColor));
      return font.getStringWidth("SOUP VISUALS");
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      return font.getStringWidth("SOUP VISUALS");
   }

   @Override
   public String getName() {
      return "Client Name";
   }
}
