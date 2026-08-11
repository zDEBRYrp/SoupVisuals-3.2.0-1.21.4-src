package padej.soup.base.util.math;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import padej.soup.base.QuickImports;

public final class MathUtil implements QuickImports {
   public static double PI2 = Math.PI * 2;

   public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public static Vec3d closestPointToBox(Box box) {
      Vec3d eye = mc.player.getCameraPosVec(mc.getRenderTickCounter().getTickDelta(true));
      return new Vec3d(
         Math.min(Math.max(eye.x, box.minX), box.maxX), Math.min(Math.max(eye.y, box.minY), box.maxY), Math.min(Math.max(eye.z, box.minZ), box.maxZ)
      );
   }

   public static Vec3d closestPointToEntity(Entity entity) {
      Vec3d eye = mc.player.getCameraPosVec(mc.getRenderTickCounter().getTickDelta(true));
      Box prevBox = entity.getBoundingBox().offset(entity.prevX - entity.getX(), entity.prevY - entity.getY(), entity.prevZ - entity.getZ());
      Box currentBox = entity.getBoundingBox();
      double prevX = Math.min(Math.max(eye.x, prevBox.minX), prevBox.maxX);
      double prevY = Math.min(Math.max(eye.y, prevBox.minY), prevBox.maxY);
      double prevZ = Math.min(Math.max(eye.z, prevBox.minZ), prevBox.maxZ);
      double curX = Math.min(Math.max(eye.x, currentBox.minX), currentBox.maxX);
      double curY = Math.min(Math.max(eye.y, currentBox.minY), currentBox.maxY);
      double curZ = Math.min(Math.max(eye.z, currentBox.minZ), currentBox.maxZ);
      return new Vec3d(interpolate(prevX, curX), interpolate(prevY, curY), interpolate(prevZ, curZ));
   }

   public static void scale(MatrixStack stack, float x, float y, float scale, Runnable data) {
      if (scale != 1.0F) {
         float scaleFactor = 0.5F + scale / 2.0F;
         stack.push();
         stack.translate(x, y, 0.0F);
         stack.scale(scaleFactor, scaleFactor, 1.0F);
         stack.translate(-x, -y, 0.0F);
         setAlpha(scale, data);
         stack.pop();
      } else {
         data.run();
      }
   }

   public static void scale(MatrixStack stack, float x, float y, float scaleX, float scaleY, Runnable data) {
      float sumScale = scaleX * scaleY;
      if (sumScale != 1.0F) {
         stack.push();
         stack.translate(x, y, 0.0F);
         stack.scale(scaleX, scaleY, 1.0F);
         stack.translate(-x, -y, 0.0F);
         setAlpha(sumScale, data);
         stack.pop();
      } else {
         data.run();
      }
   }

   public static float blinking(double speed, float f) {
      float red = (float)(System.currentTimeMillis() % speed / (speed / f));
      if (red > f / 2.0F) {
         red = f - red;
      }

      return red;
   }

   public static float textScrolling(float textWidth) {
      int speed = (int)(textWidth * 75.0F);
      return (float)MathHelper.clamp(System.currentTimeMillis() % speed * Math.PI / speed, 0.0, 1.0) * textWidth;
   }

   public static void setAlpha(float alpha, Runnable data) {
      setColor(1.0F, 1.0F, 1.0F, alpha, data);
   }

   public static void setColor(float red, float green, float blue, float alpha, Runnable data) {
      RenderSystem.setShaderColor(
         MathHelper.clamp(red, 0.0F, 1.0F), MathHelper.clamp(green, 0.0F, 1.0F), MathHelper.clamp(blue, 0.0F, 1.0F), MathHelper.clamp(alpha, 0.0F, 1.0F)
      );
      data.run();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static double round(double num, double increment) {
      double rounded = Math.round(num / increment) * increment;
      return Math.round(rounded * 100.0) / 100.0;
   }

   public static int floorNearestMulN(int x, int n) {
      return n * (int)Math.floor((double)x / n);
   }

   public static int getRed(int hex) {
      return hex >> 16 & 0xFF;
   }

   public static int getGreen(int hex) {
      return hex >> 8 & 0xFF;
   }

   public static int getBlue(int hex) {
      return hex & 0xFF;
   }

   public static int getAlpha(int hex) {
      return hex >> 24 & 0xFF;
   }

   public static int applyOpacity(int color, float opacity) {
      return ColorHelper.getArgb((int)(getAlpha(color) * opacity / 255.0F), getRed(color), getGreen(color), getBlue(color));
   }

   public static Vec3d cosSin(int i, int size, double width) {
      int index = Math.min(i, size);
      float cos = (float)(Math.cos(index * PI2 / size) * width);
      float sin = (float)(-Math.sin(index * PI2 / size) * width);
      return new Vec3d(cos, 0.0, sin);
   }

   public static double absSinAnimation(double input) {
      return Math.abs(1.0 + Math.sin(input)) / 2.0;
   }

   public static Vector3d interpolate(Vector3d prevPos, Vector3d pos) {
      return new Vector3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
   }

   public static Vec3d interpolate(Vec3d prevPos, Vec3d pos) {
      return new Vec3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
   }

   public static Vec3d interpolate(Entity entity) {
      return entity == null
         ? Vec3d.ZERO
         : new Vec3d(interpolate(entity.prevX, entity.getX()), interpolate(entity.prevY, entity.getY()), interpolate(entity.prevZ, entity.getZ()));
   }

   public static float interpolate(float prev, float orig) {
      return MathHelper.lerp(tickCounter.getTickDelta(false), prev, orig);
   }

   public static double interpolate(double prev, double orig) {
      return MathHelper.lerp(tickCounter.getTickDelta(false), prev, orig);
   }

   public static float interpolateSmooth(double smooth, float prev, float orig) {
      return (float)MathHelper.lerp(tickCounter.getLastDuration() / smooth, prev, orig);
   }

   public static double interpolateSmooth(double smooth, double prev, double orig) {
      return MathHelper.lerp(tickCounter.getLastDuration() / smooth, prev, orig);
   }

   private MathUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
