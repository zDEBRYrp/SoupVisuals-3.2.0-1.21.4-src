package padej.soup.implement.features.draggables.watermark;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;

public class FPSComponent implements WatermarkComponent, QuickImports {
   private static final int INTERPOLATION_TICKS = 20;
   private static final String[] WIDTH_TEMPLATES = new String[]{"8 FPS", "88 FPS", "888 FPS", "8888 FPS", "88888 FPS", "888888 FPS"};
   private int displayedFps = 0;
   private int targetFps = -1;
   private int interpolationStartFps = 0;
   private int interpolationTick = 20;
   private String fpsText = "0 FPS";
   private int fpsDigits = 1;
   private float cachedWidth = -1.0F;
   private FontRenderer cachedWidthFont;
   private boolean cachedIconsEnabled;

   @Override
   public void tick() {
      int keyFps = Math.max(0, mc.getCurrentFps());
      if (this.targetFps < 0) {
         this.targetFps = keyFps;
         this.interpolationStartFps = keyFps;
         this.interpolationTick = 20;
         this.applyDisplayedFps(keyFps);
      } else {
         if (keyFps != this.targetFps) {
            this.interpolationStartFps = this.displayedFps;
            this.targetFps = keyFps;
            this.interpolationTick = 0;
         }

         int nextDisplayed;
         if (this.interpolationTick < 20) {
            this.interpolationTick++;
            nextDisplayed = this.interpolateInt(this.interpolationStartFps, this.targetFps, this.interpolationTick, 20);
         } else {
            nextDisplayed = this.targetFps;
         }

         if (nextDisplayed != this.displayedFps) {
            this.applyDisplayedFps(nextDisplayed);
         }
      }
   }

   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         image.setIcon(61446).render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, this.fpsText, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(this.fpsText);
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      boolean iconsEnabled = this.shouldShowIcons();
      if (this.cachedWidth >= 0.0F && this.cachedWidthFont == font && this.cachedIconsEnabled == iconsEnabled) {
         return this.cachedWidth;
      } else {
         float iconSize = 8.0F;
         float iconOffset = iconsEnabled ? iconSize + 2.0F : 0.0F;
         String widthTemplate = WIDTH_TEMPLATES[Math.min(this.fpsDigits, WIDTH_TEMPLATES.length) - 1];
         this.cachedWidth = iconOffset + font.getStringWidth(widthTemplate) - 1.0F;
         this.cachedWidthFont = font;
         this.cachedIconsEnabled = iconsEnabled;
         return this.cachedWidth;
      }
   }

   @Override
   public float getSmoothedWidth(FontRenderer font, float height) {
      return this.getWidth(font, height);
   }

   @Override
   public String getName() {
      return "FPS";
   }

   private void applyDisplayedFps(int value) {
      this.displayedFps = Math.max(0, value);
      this.fpsText = this.displayedFps + " FPS";
      int newDigits = this.getDigitCount(this.displayedFps);
      if (newDigits != this.fpsDigits) {
         this.fpsDigits = newDigits;
         this.cachedWidth = -1.0F;
      }
   }

   private int getDigitCount(int value) {
      if (value >= 100000) {
         return 6;
      } else if (value >= 10000) {
         return 5;
      } else if (value >= 1000) {
         return 4;
      } else if (value >= 100) {
         return 3;
      } else {
         return value >= 10 ? 2 : 1;
      }
   }

   private int interpolateInt(int from, int to, int step, int maxStep) {
      long delta = (long)to - from;
      long scaled = delta * step;
      return scaled >= 0L ? from + (int)((scaled + maxStep / 2L) / maxStep) : from - (int)((-scaled + maxStep / 2L) / maxStep);
   }
}
