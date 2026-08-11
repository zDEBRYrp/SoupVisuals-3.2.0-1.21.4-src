package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.core.Main;

public class CircleExplosionEffect {
   private static final MinecraftClient mc = Main.mc;
   private static final Tessellator tessellator = Tessellator.getInstance();
   private static final List<CircleExplosionEffect.ActiveShockwave> activeShockwaves = new ArrayList<>();
   private static final int SEGMENTS = 90;
   private static final double PI2_OVER_SEGMENTS = MathUtil.PI2 / 90.0;
   private static final float MAX_RADIUS_MULT = 6.0F;
   private static final float LINE_WIDTH = 3.0F;

   public static void spawn(
      Vec3d centerPos, float entityWidth, float entityHeight, double circleStep, float red, int[] customColors, long lifetimeMs, float extraSpeed
   ) {
      long now = System.currentTimeMillis();
      float startRadius = entityWidth * 0.9F;
      double yAnim = MathUtil.absSinAnimation(circleStep) * entityHeight;
      double yAnim2 = MathUtil.absSinAnimation(circleStep - 0.45) * entityHeight;
      float verticalHeight = (float)Math.abs(yAnim - yAnim2);
      if (verticalHeight < 0.1F) {
         verticalHeight = 0.1F;
      }

      double ringY = centerPos.y + Math.max(yAnim, yAnim2);
      activeShockwaves.add(
         new CircleExplosionEffect.ActiveShockwave(now, centerPos.x, ringY, centerPos.z, startRadius, verticalHeight, red, customColors, lifetimeMs, extraSpeed)
      );
   }

   public static void render(MatrixStack eventMatrix) {
      if (!activeShockwaves.isEmpty()) {
         if (RenderSystem.isOnRenderThread()) {
            long now = System.currentTimeMillis();
            activeShockwaves.removeIf(s -> now - s.startTime > s.lifetimeMs + 100L);
            if (!activeShockwaves.isEmpty()) {
               GL11.glEnable(2881);
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(false);
               RenderSystem.enableBlend();
               RenderSystem.disableCull();
               RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
               RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

               for (CircleExplosionEffect.ActiveShockwave sw : activeShockwaves) {
                  float progress = Math.min((float)(now - sw.startTime) / (float)sw.lifetimeMs, 1.0F);
                  if (!(progress >= 1.0F)) {
                     renderShockwave(eventMatrix, sw, progress);
                  }
               }

               RenderSystem.depthMask(true);
               RenderSystem.disableDepthTest();
               GL11.glDisable(2881);
            }
         }
      }
   }

   public static boolean hasActiveShockwaves() {
      return !activeShockwaves.isEmpty();
   }

   public static void clear() {
      activeShockwaves.clear();
   }

   private static void renderShockwave(MatrixStack eventMatrix, CircleExplosionEffect.ActiveShockwave sw, float progress) {
      float eased = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
      float radius = sw.startRadius * (1.0F + eased * 5.0F * sw.extraSpeed);
      float life = 1.0F - progress;
      float alpha = life * life;
      float vertHeight = sw.verticalHeight * life;
      if (vertHeight < 0.01F) {
         vertHeight = 0.01F;
      }

      if (!(alpha < 0.01F)) {
         BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

         for (int i = 0; i <= 90; i++) {
            int idx = Math.min(i, 90);
            float cosI = (float)(Math.cos(idx * PI2_OVER_SEGMENTS) * radius);
            float sinI = (float)(-Math.sin(idx * PI2_OVER_SEGMENTS) * radius);
            int nextIdx = Math.min(i + 1, 90);
            float cosN = (float)(Math.cos(nextIdx * PI2_OVER_SEGMENTS) * radius);
            float sinN = (float)(-Math.sin(nextIdx * PI2_OVER_SEGMENTS) * radius);
            int color = getCircleGradientColor(i, 90, sw.red, sw.customColors);
            Vec3d top = new Vec3d(sw.centerX + cosI, sw.ringY, sw.centerZ + sinI);
            Vec3d bottom = new Vec3d(sw.centerX + cosI, sw.ringY - vertHeight, sw.centerZ + sinI);
            Render3DUtil.vertexLine(eventMatrix, buffer, top, bottom, ColorUtil.multAlpha(color, 0.6F * alpha), ColorUtil.multAlpha(color, 0.0F));
            Vec3d currentTop = new Vec3d(sw.centerX + cosI, sw.ringY, sw.centerZ + sinI);
            Vec3d nextTop = new Vec3d(sw.centerX + cosN, sw.ringY, sw.centerZ + sinN);
            Render3DUtil.drawLine(currentTop, nextTop, ColorUtil.multAlpha(color, alpha), 3.0F, true);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   private static int getCircleGradientColor(int segmentIndex, int totalSegments, float red, int[] customColors) {
      if (customColors != null && customColors.length > 0) {
         float degreesPerColor = 360.0F / customColors.length;
         float currentDegree = segmentIndex * 360.0F / totalSegments;
         int colorIndex1 = (int)(currentDegree / degreesPerColor) % customColors.length;
         int colorIndex2 = (colorIndex1 + 1) % customColors.length;
         float factor = currentDegree % degreesPerColor / degreesPerColor;
         int color1 = customColors[colorIndex1];
         int color2 = customColors[colorIndex2];
         int interpolated = interpolateColor(color1, color2, factor);
         return ColorUtil.gradientToRed(interpolated, red);
      } else {
         return ColorUtil.gradientToRed(ColorUtil.fade(segmentIndex * 4), red);
      }
   }

   private static int interpolateColor(int c1, int c2, float factor) {
      int a1 = c1 >> 24 & 0xFF;
      int r1 = c1 >> 16 & 0xFF;
      int g1 = c1 >> 8 & 0xFF;
      int b1 = c1 & 0xFF;
      int a2 = c2 >> 24 & 0xFF;
      int r2 = c2 >> 16 & 0xFF;
      int g2 = c2 >> 8 & 0xFF;
      int b2 = c2 & 0xFF;
      return (int)(a1 + (a2 - a1) * factor) << 24 | (int)(r1 + (r2 - r1) * factor) << 16 | (int)(g1 + (g2 - g1) * factor) << 8 | (int)(b1 + (b2 - b1) * factor);
   }

   private static class ActiveShockwave {
      final long startTime;
      final double centerX;
      final double ringY;
      final double centerZ;
      final float startRadius;
      final float verticalHeight;
      final float red;
      final int[] customColors;
      final long lifetimeMs;
      final float extraSpeed;

      ActiveShockwave(
         long startTime,
         double centerX,
         double ringY,
         double centerZ,
         float startRadius,
         float verticalHeight,
         float red,
         int[] customColors,
         long lifetimeMs,
         float extraSpeed
      ) {
         this.startTime = startTime;
         this.centerX = centerX;
         this.ringY = ringY;
         this.centerZ = centerZ;
         this.startRadius = startRadius;
         this.verticalHeight = verticalHeight;
         this.red = red;
         this.customColors = customColors;
         this.lifetimeMs = lifetimeMs;
         this.extraSpeed = extraSpeed;
      }
   }
}
