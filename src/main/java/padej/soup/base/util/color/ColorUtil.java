package padej.soup.base.util.color;

import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import java.awt.Color;
import java.util.regex.Pattern;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4i;
import padej.soup.implement.features.modules.client.Theme;

public final class ColorUtil {
   public static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)§[0-9a-f-or]");
   private static final float[] HSB_SCRATCH = new float[3];
   private static final Vector4i ROUND_CLIENT_COLOR_SCRATCH = new Vector4i();
   public static Char2IntArrayMap colorCodes = new Char2IntArrayMap() {
      {
         this.put('0', 0);
         this.put('1', 170);
         this.put('2', 43520);
         this.put('3', 43690);
         this.put('4', 11141120);
         this.put('5', 11141290);
         this.put('6', 16755200);
         this.put('7', 11184810);
         this.put('8', 5592405);
         this.put('9', 5592575);
         this.put('A', 5635925);
         this.put('B', 5636095);
         this.put('C', 16733525);
         this.put('D', 16733695);
         this.put('E', 16777045);
         this.put('F', 16777215);
      }
   };
   public static final int RED = getColor(255, 0, 0);
   public static final int GREEN = getColor(0, 255, 0);
   public static final int BLUE = getColor(0, 0, 255);
   public static final int YELLOW = getColor(255, 255, 0);
   public static final int WHITE = getColor(255);
   public static final int BLACK = getColor(0);
   public static final int HALF_BLACK = getColor(0, 0.5F);
   public static final int LIGHT_RED = getColor(255, 85, 85);
   private static volatile ColorPalette customLivePalette = null;

   public static int red(int c) {
      return c >> 16 & 0xFF;
   }

   public static int green(int c) {
      return c >> 8 & 0xFF;
   }

   public static int blue(int c) {
      return c & 0xFF;
   }

   public static int alpha(int c) {
      return c >> 24 & 0xFF;
   }

   public static float redf(int c) {
      return red(c) / 255.0F;
   }

   public static float greenf(int c) {
      return green(c) / 255.0F;
   }

   public static float bluef(int c) {
      return blue(c) / 255.0F;
   }

   public static float alphaf(int c) {
      return alpha(c) / 255.0F;
   }

   public static int[] getRGBA(int c) {
      return new int[]{red(c), green(c), blue(c), alpha(c)};
   }

   public static int[] getRGB(int c) {
      return new int[]{red(c), green(c), blue(c)};
   }

   public static float[] getRGBAf(int c) {
      return new float[]{redf(c), greenf(c), bluef(c), alphaf(c)};
   }

   public static float[] getRGBf(int c) {
      return new float[]{redf(c), greenf(c), bluef(c)};
   }

   public static int getColor(float red, float green, float blue, float alpha) {
      return getColor(Math.round(red * 255.0F), Math.round(green * 255.0F), Math.round(blue * 255.0F), Math.round(alpha * 255.0F));
   }

   public static int getColor(int red, int green, int blue, float alpha) {
      return getColor(red, green, blue, Math.round(alpha * 255.0F));
   }

   public static int getColor(float red, float green, float blue) {
      return getColor(red, green, blue, 1.0F);
   }

   public static int getColor(int brightness, int alpha) {
      return getColor(brightness, brightness, brightness, alpha);
   }

   public static int getColor(int brightness, float alpha) {
      return getColor(brightness, Math.round(alpha * 255.0F));
   }

   public static int getColor(int brightness) {
      return getColor(brightness, brightness, brightness);
   }

   public static int replAlpha(int color, int alpha) {
      return getColor(red(color), green(color), blue(color), alpha);
   }

   public static int replAlpha(int color, float alpha) {
      return getColor(red(color), green(color), blue(color), alpha);
   }

   public static int multAlpha(int color, float percent01) {
      return getColor(red(color), green(color), blue(color), Math.round(alpha(color) * percent01));
   }

   public static int multColor(int colorStart, int colorEnd, float progress) {
      return getColor(
         Math.round(red(colorStart) * (redf(colorEnd) * progress)),
         Math.round(green(colorStart) * (greenf(colorEnd) * progress)),
         Math.round(blue(colorStart) * (bluef(colorEnd) * progress)),
         Math.round(alpha(colorStart) * (alphaf(colorEnd) * progress))
      );
   }

   public static int multRed(int colorStart, int colorEnd, float progress) {
      return getColor(
         Math.round(red(colorStart) * (redf(colorEnd) * progress)),
         Math.round(green(colorStart) * (greenf(colorEnd) * progress)),
         Math.round(blue(colorStart) * (bluef(colorEnd) * progress)),
         Math.round(alpha(colorStart) * (alphaf(colorEnd) * progress))
      );
   }

   public static int multDark(int color, float percent01) {
      return getColor(Math.round(red(color) * percent01), Math.round(green(color) * percent01), Math.round(blue(color) * percent01), alpha(color));
   }

   public static int multBright(int color, float percent01) {
      return getColor(
         Math.min(255, Math.round(red(color) / percent01)),
         Math.min(255, Math.round(green(color) / percent01)),
         Math.min(255, Math.round(blue(color) / percent01)),
         alpha(color)
      );
   }

   public static int overCol(int color1, int color2, float percent01) {
      float percent = MathHelper.clamp(percent01, 0.0F, 1.0F);
      return getColor(
         MathHelper.lerp(percent, red(color1), red(color2)),
         MathHelper.lerp(percent, green(color1), green(color2)),
         MathHelper.lerp(percent, blue(color1), blue(color2)),
         MathHelper.lerp(percent, alpha(color1), alpha(color2))
      );
   }

   public static Vector4i multRedAndAlpha(Vector4i color, float red, float alpha) {
      return color.set(
         multRedAndAlpha(color.x, red, alpha), multRedAndAlpha(color.y, red, alpha), multRedAndAlpha(color.w, red, alpha), multRedAndAlpha(color.z, red, alpha)
      );
   }

   public static int multRedAndAlpha(int color, float red, float alpha) {
      return getColor(red(color), Math.min(255, Math.round(green(color) / red)), Math.min(255, Math.round(blue(color) / red)), Math.round(alpha(color) * alpha));
   }

   public static int multRed(int color, float percent01) {
      return getColor(red(color), Math.min(255, Math.round(green(color) / percent01)), Math.min(255, Math.round(blue(color) / percent01)), alpha(color));
   }

   public static int multGreen(int color, float percent01) {
      return getColor(Math.min(255, Math.round(green(color) / percent01)), green(color), Math.min(255, Math.round(blue(color) / percent01)), alpha(color));
   }

   public static int gradientToRed(int originalColor, float damageIntensity) {
      damageIntensity = MathHelper.clamp(damageIntensity, 0.0F, 1.0F);
      int originalRed = red(originalColor);
      int originalGreen = green(originalColor);
      int originalBlue = blue(originalColor);
      int originalAlpha = alpha(originalColor);
      int targetRed = 255;
      int targetGreen = 0;
      int targetBlue = 0;
      int newRed = Math.round((float)MathHelper.lerp(damageIntensity, originalRed, targetRed));
      int newGreen = Math.round((float)MathHelper.lerp(damageIntensity, originalGreen, targetGreen));
      int newBlue = Math.round((float)MathHelper.lerp(damageIntensity, originalBlue, targetBlue));
      return getColor(newRed, newGreen, newBlue, originalAlpha);
   }

   public static int[] genGradientForText(int color1, int color2, int length) {
      int[] gradient = new int[length];

      for (int i = 0; i < length; i++) {
         float pc = (float)i / (length - 1);
         gradient[i] = overCol(color1, color2, pc);
      }

      return gradient;
   }

   public static int rainbow(int speed, int index, float saturation, float brightness, float opacity) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      float hue = angle / 360.0F;
      int color = Color.HSBtoRGB(hue, saturation, brightness);
      return getColor(red(color), green(color), blue(color), Math.round(opacity * 255.0F));
   }

   public static int fade(int speed, int index, int first, int second) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      angle = angle >= 180 ? 360 - angle : angle;
      return overCol(first, second, angle / 180.0F);
   }

   public static int fade(int index) {
      int cc = getClientColor();
      return fade(8, index, multBright(cc, 0.7F), multDark(cc, 0.7F));
   }

   public static int fade(int index, int indexMultiplier) {
      int cc = getClientColor();
      return fade(8, index * indexMultiplier, multBright(cc, 0.7F), multDark(cc, 0.7F));
   }

   public static int invertFade(int index) {
      int cc = getClientColor();
      return fade(8, index, multDark(cc, 0.7F), multBright(cc, 0.7F));
   }

   public static int multiColorFade(int index) {
      int[] colors = getClientColors();
      if (colors.length == 0) {
         return getColor(255, 255, 255);
      } else if (colors.length == 1) {
         return colors[0];
      } else {
         float speedMultiplier = Theme.getInstance().getFadeSpeed();
         long currentTime = System.currentTimeMillis();
         if (colors.length == 2) {
            double adjustedSpeed = 8.0 / speedMultiplier;
            int angle = (int)((currentTime / adjustedSpeed + index) % 360.0);
            angle = angle >= 180 ? 360 - angle : angle;
            return overCol(colors[0], colors[1], angle / 180.0F);
         } else {
            double adjustedSpeed = 10.0 / speedMultiplier;
            float timeProgress = (float)((currentTime / adjustedSpeed + index * 100) % (colors.length * 360L)) / 360.0F;
            int index1 = (int)Math.floor(timeProgress) % colors.length;
            int index2 = (index1 + 1) % colors.length;
            float lerp = timeProgress - (int)Math.floor(timeProgress);
            return overCol(colors[index1], colors[index2], lerp);
         }
      }
   }

   public static Vector4i roundClientColor(float alpha) {
      return ROUND_CLIENT_COLOR_SCRATCH.set(
         multAlpha(multiColorFade(270), alpha),
         multAlpha(multiColorFade(0), alpha),
         multAlpha(multiColorFade(180), alpha),
         multAlpha(multiColorFade(90), alpha)
      );
   }

   private static int clamp(int v) {
      return v < 0 ? 0 : Math.min(v, 255);
   }

   public static int getColor(int red, int green, int blue, int alpha) {
      return clamp(alpha) << 24 | clamp(red) << 16 | clamp(green) << 8 | clamp(blue);
   }

   public static int getColor(int red, int green, int blue) {
      return getColor(red, green, blue, 255);
   }

   public static String formatting(int color) {
      return "⏏" + color + "⏏";
   }

   public static int lighter(int hex) {
      return lighter(hex, 1);
   }

   public static int lighter(int hex, int steps) {
      return adjustShade(hex, steps, true);
   }

   public static int darker(int hex) {
      return darker(hex, 1);
   }

   public static int darker(int hex, int steps) {
      return adjustShade(hex, steps, false);
   }

   private static int adjustShade(int hex, int steps, boolean makeLighter) {
      int safeSteps = Math.max(1, steps);
      int a = hex >> 24 & 0xFF;
      int r = hex >> 16 & 0xFF;
      int g = hex >> 8 & 0xFF;
      int b = hex & 0xFF;
      Color.RGBtoHSB(r, g, b, HSB_SCRATCH);
      float hue = HSB_SCRATCH[0];
      float saturation = HSB_SCRATCH[1];
      float brightness = HSB_SCRATCH[2];
      float brightnessShift = Math.min(0.45F, 0.12F * safeSteps);
      float newBrightness = makeLighter
         ? MathHelper.clamp(brightness + brightnessShift, 0.0F, 1.0F)
         : MathHelper.clamp(brightness - brightnessShift, 0.0F, 1.0F);
      float satShift = Math.min(0.18F, 0.04F * safeSteps);
      float newSaturation = makeLighter
         ? MathHelper.clamp(saturation * (1.0F - satShift), 0.0F, 1.0F)
         : MathHelper.clamp(saturation * (1.0F + satShift), 0.0F, 1.0F);
      int rgb = Color.HSBtoRGB(hue, newSaturation, newBrightness) & 16777215;
      return a << 24 | rgb;
   }

   public static String removeFormatting(String text) {
      return text != null && !text.isEmpty() ? FORMATTING_CODE_PATTERN.matcher(text).replaceAll("") : null;
   }

   private static ColorPalette getPalette() {
      Theme theme = Theme.getInstance();
      if (theme.themeMode.isSelected("Custom")) {
         ColorPalette p = customLivePalette;
         if (p == null) {
            p = createCustomLivePalette(theme);
            customLivePalette = p;
         }

         return p;
      } else {
         return theme.getCurrentPalette();
      }
   }

   private static ColorPalette createCustomLivePalette(Theme theme) {
      return new ColorPalette() {
         @Override
         public int mainGuiColor() {
            return theme.mainGuiColor.getColor();
         }

         @Override
         public int guiRectColor() {
            return theme.guiRectColor.getColor();
         }

         @Override
         public int guiRectColor2() {
            return theme.guiRectColor2.getColor();
         }

         @Override
         public int rectColor() {
            return theme.rectColor.getColor();
         }

         @Override
         public int rectDarkerColor() {
            return theme.rectDarkerColor.getColor();
         }

         @Override
         public int textColor() {
            return theme.textColor.getColor();
         }

         @Override
         public int descriptionColor() {
            return theme.descriptionColor.getColor();
         }

         @Override
         public int blurRectColor() {
            return theme.blurRectColor.getColor();
         }

         @Override
         public int outlineColor() {
            return -12961214;
         }

         @Override
         public int friendColor() {
            return -11870592;
         }

         @Override
         public String getName() {
            return "Custom";
         }

         @Override
         public boolean isDark() {
            int color = this.mainGuiColor();
            int brightness = (ColorUtil.red(color) + ColorUtil.green(color) + ColorUtil.blue(color)) / 3;
            return brightness < 128;
         }
      };
   }

   public static int getMainGuiColor() {
      return getPalette().mainGuiColor();
   }

   public static int getGuiRectColor(float alpha) {
      return multAlpha(getPalette().guiRectColor(), alpha);
   }

   public static int getGuiRectColor2(float alpha) {
      return multAlpha(getPalette().guiRectColor2(), alpha);
   }

   public static int getRect(float alpha) {
      return multAlpha(getPalette().rectColor(), alpha);
   }

   public static int getRectDarker(float alpha) {
      return multAlpha(getPalette().rectDarkerColor(), alpha);
   }

   public static int getBlurRect(float alpha) {
      return multAlpha(getPalette().blurRectColor(), alpha);
   }

   public static int getText(float alpha) {
      return multAlpha(getText(), alpha);
   }

   public static int getText() {
      return getPalette().textColor();
   }

   public static int getDescription() {
      return getPalette().descriptionColor();
   }

   public static int getDescription(float alpha) {
      return multAlpha(getDescription(), alpha);
   }

   public static int getClientColorByIndex(int index) {
      return Theme.getInstance().colorSetting.getColor(index).getColor();
   }

   public static int getClientColor() {
      int[] colors = getClientColors();
      if (colors.length == 0) {
         return getColor(255, 255, 255);
      } else {
         return colors.length == 1 ? colors[0] : multiColorFade(0);
      }
   }

   public static int getClientColorFade(int offset) {
      int[] colors = getClientColors();
      if (colors.length == 0) {
         return getColor(255, 255, 255);
      } else {
         return colors.length == 1 ? colors[0] : multiColorFade(offset);
      }
   }

   public static int getClientColor(float alpha) {
      return multAlpha(getClientColor(), alpha);
   }

   public static int getClientColorFade(int offset, float alpha) {
      return multAlpha(getClientColorFade(offset), alpha);
   }

   public static int[] getClientColors() {
      return Theme.getInstance().getClientColors();
   }

   public static int getFriendColor() {
      return getPalette().friendColor();
   }

   public static int getOutline(float alpha, float bright) {
      return multBright(multAlpha(getOutline(), alpha), bright);
   }

   public static int getOutline(float alpha) {
      return multAlpha(getOutline(), alpha);
   }

   public static int getOutline() {
      return getPalette().outlineColor();
   }

   public static boolean isDarkBackground() {
      int backgroundColor = getRect(1.0F);
      int r = red(backgroundColor);
      int g = green(backgroundColor);
      int b = blue(backgroundColor);
      double luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
      return luminance < 0.5;
   }

   public static int[] getBadgeColors(String role) {
      String var1 = role.toUpperCase();

      return switch (var1) {
         case "YOUTUBE" -> new int[]{parseColor("#de0000"), parseColor("#ffffff")};
         case "DEVELOPER" -> new int[]{parseColor("#ff0000"), parseColor("#7a0000")};
         case "TESTER" -> new int[]{parseColor("#2ecc71"), parseColor("#1abc9c")};
         case "PASTER" -> new int[]{parseColor("#fcff5e"), parseColor("#fbff00")};
         case "CROW" -> new int[]{parseColor("#72eeff"), parseColor("#27b3ff")};
         default -> new int[]{parseColor("#d7cee7"), parseColor("#a497b3")};
      };
   }

   private static int parseColor(String hex) {
      if (hex.startsWith("#")) {
         hex = hex.substring(1);
      }

      return getColor(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16), 255);
   }

   private ColorUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
