package padej.soup.base.util.particle;

import padej.soup.base.util.color.ColorUtil;

public class ParticleColorUtil {
   public static int getWaveColor(int[] colors, float alpha, int offset) {
      if (colors != null && colors.length != 0) {
         if (colors.length == 1) {
            return ColorUtil.multAlpha(colors[0], alpha);
         } else if (colors.length == 2) {
            int angle = (int)((System.currentTimeMillis() / 8L + offset) % 360L);
            angle = angle >= 180 ? 360 - angle : angle;
            return ColorUtil.multAlpha(ColorUtil.overCol(colors[0], colors[1], angle / 180.0F), alpha);
         } else {
            float timeProgress = (float)((System.currentTimeMillis() / 10L + offset) % (colors.length * 360L)) / 360.0F;
            int index1 = (int)Math.floor(timeProgress) % colors.length;
            int index2 = (index1 + 1) % colors.length;
            float lerp = timeProgress - (int)Math.floor(timeProgress);
            return ColorUtil.multAlpha(ColorUtil.overCol(colors[index1], colors[index2], lerp), alpha);
         }
      } else {
         return ColorUtil.multAlpha(-1, alpha);
      }
   }

   public static int getWaveColor(int[] colors, float alpha) {
      return getWaveColor(colors, alpha, 0);
   }

   public static int getVertexGradientColor(int angleOffset, int[] colors, float alpha) {
      if (colors != null && colors.length != 0) {
         if (colors.length == 1) {
            return ColorUtil.multAlpha(colors[0], alpha);
         } else {
            int angle = (int)((System.currentTimeMillis() / 8L + angleOffset) % 360L);
            if (colors.length == 2) {
               angle = angle >= 180 ? 360 - angle : angle;
               return ColorUtil.multAlpha(ColorUtil.overCol(colors[0], colors[1], angle / 180.0F), alpha);
            } else {
               float progress = angle / 360.0F;
               float colorIndex = progress * colors.length;
               int index1 = (int)colorIndex % colors.length;
               int index2 = (index1 + 1) % colors.length;
               float lerp = colorIndex - (int)colorIndex;
               return ColorUtil.multAlpha(ColorUtil.overCol(colors[index1], colors[index2], lerp), alpha);
            }
         }
      } else {
         return ColorUtil.multAlpha(-1, alpha);
      }
   }
}
