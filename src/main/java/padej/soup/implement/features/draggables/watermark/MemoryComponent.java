package padej.soup.implement.features.draggables.watermark;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;

public class MemoryComponent implements WatermarkComponent, QuickImports {
   private float smoothedWidth = 0.0F;
   private String cachedText = "0%";
   private String cachedWidestPattern = "8%";

   @Override
   public void tick() {
      Runtime runtime = Runtime.getRuntime();
      long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
      long maxMemory = runtime.maxMemory() / 1024L / 1024L;
      int percentage = (int)(usedMemory * 100L / Math.max(1L, maxMemory));
      this.cachedText = percentage + "%";
      int digitCount = String.valueOf(percentage).length();
      this.cachedWidestPattern = "8".repeat(digitCount) + "%";
   }

   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         image.setIcon(61454).render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, this.cachedText, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(this.cachedText);
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      return iconOffset + font.getStringWidth(this.cachedWidestPattern);
   }

   @Override
   public float getSmoothedWidth(FontRenderer font, float height) {
      return this.getWidth(font, height);
   }

   @Override
   public String getName() {
      return "Memory";
   }
}
