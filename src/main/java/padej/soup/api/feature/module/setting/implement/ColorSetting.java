package padej.soup.api.feature.module.setting.implement;

import java.util.function.Supplier;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.base.util.math.MathUtil;

public class ColorSetting extends Setting {
   private static final int[] DEFAULT_PRESETS = new int[]{
      -6969946,
      -1499549,
      -1618884,
      -1671646,
      -932849,
      -13710223,
      -15024996,
      -13273872,
      -13330213,
      -6596170,
      -5930259,
      -8943479,
      -4581296,
      -4571856,
      -4561637,
      -4088564,
      -14310566,
      -15362435,
      -13934912,
      -13993296,
      -8566895,
      -8034114,
      -10853018,
      -7597508,
      -7590364,
      -7582700,
      -7244278,
      -14910908,
      -15699618,
      -14661744,
      -14721915,
      -10602899,
      -10202993
   };
   private float hue = 0.0F;
   private float saturation = 1.0F;
   private float brightness = 1.0F;
   private float alpha = 1.0F;
   private int[] presets = DEFAULT_PRESETS;
   private Integer defaultColor;

   public ColorSetting(String name, String description) {
      super(name, description);
   }

   public ColorSetting value(int value) {
      if (this.defaultColor == null) {
         this.defaultColor = value;
      }

      this.setColorInternal(value);
      return this;
   }

   public ColorSetting presets(int... presets) {
      this.presets = presets;
      return this;
   }

   public ColorSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public int getColor() {
      return this.getColorWithAlpha() & 16777215 | Math.round(this.alpha * 255.0F) << 24;
   }

   public int getColorWithAlpha() {
      return HSBtoRGB(this.hue, this.saturation, this.brightness);
   }

   public ColorSetting setColor(int color) {
      if (this.defaultColor == null) {
         this.defaultColor = color;
      }

      this.setColorInternal(color);
      return this;
   }

   private void setColorInternal(int color) {
      float[] hsb = RGBtoHSB(getRed(color), getGreen(color), getBlue(color));
      this.hue = hsb[0];
      this.saturation = hsb[1];
      this.brightness = hsb[2];
      this.alpha = MathUtil.getAlpha(color) / 255.0F;
      this.notifyChange();
   }

   @Override
   public boolean isModified() {
      return this.defaultColor == null ? false : this.getColor() != this.defaultColor;
   }

   @Override
   public void reset() {
      if (this.defaultColor != null) {
         this.setColorInternal(this.defaultColor);
      }
   }

   private static int getRed(int color) {
      return color >> 16 & 0xFF;
   }

   private static int getGreen(int color) {
      return color >> 8 & 0xFF;
   }

   private static int getBlue(int color) {
      return color & 0xFF;
   }

   private static float[] RGBtoHSB(int r, int g, int b) {
      float[] hsb = new float[3];
      float rf = r / 255.0F;
      float gf = g / 255.0F;
      float bf = b / 255.0F;
      float max = Math.max(rf, Math.max(gf, bf));
      float min = Math.min(rf, Math.min(gf, bf));
      float delta = max - min;
      hsb[2] = max;
      if (max != 0.0F) {
         hsb[1] = delta / max;
      } else {
         hsb[1] = 0.0F;
      }

      if (delta == 0.0F) {
         hsb[0] = 0.0F;
      } else {
         if (rf == max) {
            hsb[0] = (gf - bf) / delta;
         } else if (gf == max) {
            hsb[0] = 2.0F + (bf - rf) / delta;
         } else {
            hsb[0] = 4.0F + (rf - gf) / delta;
         }

         hsb[0] /= 6.0F;
         if (hsb[0] < 0.0F) {
            hsb[0]++;
         }
      }

      return hsb;
   }

   private static int HSBtoRGB(float hue, float saturation, float brightness) {
      int r = 0;
      int g = 0;
      int b = 0;
      if (saturation == 0.0F) {
         r = g = b = (int)(brightness * 255.0F + 0.5F);
      } else {
         float h = (hue - (float)Math.floor(hue)) * 6.0F;
         float f = h - (float)Math.floor(h);
         float p = brightness * (1.0F - saturation);
         float q = brightness * (1.0F - saturation * f);
         float t = brightness * (1.0F - saturation * (1.0F - f));
         switch ((int)h) {
            case 0:
               r = (int)(brightness * 255.0F + 0.5F);
               g = (int)(t * 255.0F + 0.5F);
               b = (int)(p * 255.0F + 0.5F);
               break;
            case 1:
               r = (int)(q * 255.0F + 0.5F);
               g = (int)(brightness * 255.0F + 0.5F);
               b = (int)(p * 255.0F + 0.5F);
               break;
            case 2:
               r = (int)(p * 255.0F + 0.5F);
               g = (int)(brightness * 255.0F + 0.5F);
               b = (int)(t * 255.0F + 0.5F);
               break;
            case 3:
               r = (int)(p * 255.0F + 0.5F);
               g = (int)(q * 255.0F + 0.5F);
               b = (int)(brightness * 255.0F + 0.5F);
               break;
            case 4:
               r = (int)(t * 255.0F + 0.5F);
               g = (int)(p * 255.0F + 0.5F);
               b = (int)(brightness * 255.0F + 0.5F);
               break;
            case 5:
               r = (int)(brightness * 255.0F + 0.5F);
               g = (int)(p * 255.0F + 0.5F);
               b = (int)(q * 255.0F + 0.5F);
         }
      }

      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   public float getHue() {
      return this.hue;
   }

   public float getSaturation() {
      return this.saturation;
   }

   public float getBrightness() {
      return this.brightness;
   }

   public float getAlpha() {
      return this.alpha;
   }

   public int[] getPresets() {
      return this.presets;
   }

   public Integer getDefaultColor() {
      return this.defaultColor;
   }

   public void setHue(float hue) {
      this.hue = hue;
   }

   public void setSaturation(float saturation) {
      this.saturation = saturation;
   }

   public void setBrightness(float brightness) {
      this.brightness = brightness;
   }

   public void setAlpha(float alpha) {
      this.alpha = alpha;
   }

   public void setPresets(int[] presets) {
      this.presets = presets;
   }

   public void setDefaultColor(Integer defaultColor) {
      this.defaultColor = defaultColor;
   }
}
