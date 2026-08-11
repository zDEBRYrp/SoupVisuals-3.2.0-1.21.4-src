package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.modules.visuals.Trails;

public final class TrailRenderer implements QuickImports {
   private static final float FIXED_TRAIL_HEIGHT = 1.8F;
   private static final ArrayDeque<TrailRenderer.TrailData> TRAIL_QUEUE = new ArrayDeque<>();
   private static final ArrayDeque<TrailRenderer.DebugLineData> DEBUG_LINE_QUEUE = new ArrayDeque<>();
   private static final ArrayDeque<TrailRenderer.TailData> TAIL_QUEUE = new ArrayDeque<>();
   private static final MatrixStack SCRATCH_MATRIX = new MatrixStack();
   private static long cachedTime = 0L;

   public static void queueTrails(MatrixStack stack, List<Trails.Trail> trails, Trails module, PlayerEntity entity) {
      if (!trails.isEmpty() && trails.size() >= 2) {
         TRAIL_QUEUE.add(new TrailRenderer.TrailData(getRotationOnlyMatrix(stack.peek().getPositionMatrix()), trails, module, entity));
      }
   }

   public static void queueDebugLines(MatrixStack stack, List<Trails.Trail> trails, Trails module, PlayerEntity entity) {
      if (!trails.isEmpty() && trails.size() >= 2) {
         DEBUG_LINE_QUEUE.add(new TrailRenderer.DebugLineData(getRotationOnlyMatrix(stack.peek().getPositionMatrix()), trails, module, entity));
      }
   }

   public static void queueTails(MatrixStack stack, List<Trails.Trail> trails, Trails module) {
      if (!trails.isEmpty()) {
         TAIL_QUEUE.add(new TrailRenderer.TailData(new Matrix4f(stack.peek().getPositionMatrix()), trails, module));
      }
   }

   public static void renderBatches() {
      cachedTime = System.currentTimeMillis();
      if (!TRAIL_QUEUE.isEmpty()) {
         renderTrailBatch();
      }

      if (!DEBUG_LINE_QUEUE.isEmpty()) {
         renderDebugLineBatch();
      }

      if (!TAIL_QUEUE.isEmpty()) {
         renderTailBatch();
      }
   }

   private static void renderTrailBatch() {
      RenderSystem.disableCull();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.blendFunc(770, 771);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

      while (!TRAIL_QUEUE.isEmpty()) {
         TrailRenderer.TrailData data = TRAIL_QUEUE.poll();
         List<Trails.Trail> trails = data.trails;
         Trails module = data.module;
         Matrix4f matrix = data.matrix;
         Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
         float down = module.getDown().getValue();
         boolean renderHalf = module.getRenderHalf().isValue();
         String styleStr = module.getStyle().getSelected();
         boolean isFaded = styleStr.equals("Faded");
         boolean isInvert = styleStr.equals("Invert");
         float alphaFactorValue = module.getAlphaFactor().getValue();

         for (int i = 0; i < trails.size() - 1; i++) {
            Trails.Trail current = trails.get(i);
            Trails.Trail next = trails.get(i + 1);
            Vec3d currentPos = current.currentPos;
            Vec3d nextPos = next.currentPos;
            float currentAlpha = current.getAlpha();
            float nextAlpha = next.getAlpha();
            int currentColor = current.cachedColor;
            int nextColor = next.cachedColor;
            float trailHeight = 1.8F;
            float x1 = (float)(currentPos.x - cameraPos.x);
            float y1 = (float)(currentPos.y - cameraPos.y) + down;
            float z1 = (float)(currentPos.z - cameraPos.z);
            float x2 = (float)(nextPos.x - cameraPos.x);
            float y2 = (float)(nextPos.y - cameraPos.y) + down;
            float z2 = (float)(nextPos.z - cameraPos.z);
            int currentBottomAlpha;
            int currentMidAlpha;
            int currentTopAlpha;
            int nextBottomAlpha;
            int nextMidAlpha;
            int nextTopAlpha;
            if (isFaded) {
               currentBottomAlpha = (int)(computeFadedAlpha(0.0F, trailHeight) * currentAlpha);
               currentMidAlpha = (int)(computeFadedAlpha(trailHeight / 2.0F, trailHeight) * currentAlpha);
               currentTopAlpha = renderHalf ? 0 : (int)(computeFadedAlpha(trailHeight, trailHeight) * currentAlpha);
               nextBottomAlpha = (int)(computeFadedAlpha(0.0F, trailHeight) * nextAlpha);
               nextMidAlpha = (int)(computeFadedAlpha(trailHeight / 2.0F, trailHeight) * nextAlpha);
               nextTopAlpha = renderHalf ? 0 : (int)(computeFadedAlpha(trailHeight, trailHeight) * nextAlpha);
            } else if (isInvert) {
               currentBottomAlpha = (int)(computeFadedAlphaInvert(0.0F, trailHeight, alphaFactorValue) * currentAlpha);
               currentMidAlpha = (int)(computeFadedAlphaInvert(trailHeight / 2.0F, trailHeight, alphaFactorValue) * currentAlpha);
               currentTopAlpha = renderHalf ? 0 : (int)(computeFadedAlphaInvert(trailHeight, trailHeight, alphaFactorValue) * currentAlpha);
               nextBottomAlpha = (int)(computeFadedAlphaInvert(0.0F, trailHeight, alphaFactorValue) * nextAlpha);
               nextMidAlpha = (int)(computeFadedAlphaInvert(trailHeight / 2.0F, trailHeight, alphaFactorValue) * nextAlpha);
               nextTopAlpha = renderHalf ? 0 : (int)(computeFadedAlphaInvert(trailHeight, trailHeight, alphaFactorValue) * nextAlpha);
            } else {
               currentBottomAlpha = (int)(currentAlpha * 255.0F);
               currentMidAlpha = (int)(currentAlpha * 255.0F);
               currentTopAlpha = renderHalf ? 0 : (int)(currentAlpha * 255.0F);
               nextBottomAlpha = (int)(nextAlpha * 255.0F);
               nextMidAlpha = (int)(nextAlpha * 255.0F);
               nextTopAlpha = renderHalf ? 0 : (int)(nextAlpha * 255.0F);
            }

            int currentR = currentColor >> 16 & 0xFF;
            int currentG = currentColor >> 8 & 0xFF;
            int currentB = currentColor & 0xFF;
            int nextR = nextColor >> 16 & 0xFF;
            int nextG = nextColor >> 8 & 0xFF;
            int nextB = nextColor & 0xFF;
            bufferBuilder.vertex(matrix, x1, y1, z1).color(currentR, currentG, currentB, currentBottomAlpha);
            bufferBuilder.vertex(matrix, x2, y2, z2).color(nextR, nextG, nextB, nextBottomAlpha);
            bufferBuilder.vertex(matrix, x2, y2 + trailHeight / 2.0F, z2).color(nextR, nextG, nextB, nextMidAlpha);
            bufferBuilder.vertex(matrix, x1, y1 + trailHeight / 2.0F, z1).color(currentR, currentG, currentB, currentMidAlpha);
            if (!renderHalf) {
               bufferBuilder.vertex(matrix, x1, y1 + trailHeight / 2.0F, z1).color(currentR, currentG, currentB, currentMidAlpha);
               bufferBuilder.vertex(matrix, x2, y2 + trailHeight / 2.0F, z2).color(nextR, nextG, nextB, nextMidAlpha);
               bufferBuilder.vertex(matrix, x2, y2 + trailHeight, z2).color(nextR, nextG, nextB, nextTopAlpha);
               bufferBuilder.vertex(matrix, x1, y1 + trailHeight, z1).color(currentR, currentG, currentB, currentTopAlpha);
            }
         }
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableDepthTest();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
   }

   private static void renderDebugLineBatch() {
      GL11.glHint(3154, 4354);
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
      RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);
      SCRATCH_MATRIX.loadIdentity();

      while (!DEBUG_LINE_QUEUE.isEmpty()) {
         TrailRenderer.DebugLineData data = DEBUG_LINE_QUEUE.poll();
         List<Trails.Trail> trails = data.trails;
         Trails module = data.module;
         Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
         RenderSystem.lineWidth(module.getLineWidth().getValue());
         float down = module.getDown().getValue();
         boolean renderHalf = module.getRenderHalf().isValue();
         SCRATCH_MATRIX.peek().getPositionMatrix().set(data.matrix);
         Entry entry = SCRATCH_MATRIX.peek();

         for (int i = 0; i < trails.size() - 1; i++) {
            Trails.Trail current = trails.get(i);
            Trails.Trail next = trails.get(i + 1);
            Vec3d currentPos = current.currentPos;
            Vec3d nextPos = next.currentPos;
            float currentAlpha = current.getAlpha();
            float nextAlpha = next.getAlpha();
            float currentTopY = 1.8F + down;
            float nextTopY = 1.8F + down;
            int currentColor = current.cachedColor;
            int nextColor = next.cachedColor;
            float avgAlpha = (currentAlpha + nextAlpha) / 2.0F;
            int avgR = ((currentColor >> 16 & 0xFF) + (nextColor >> 16 & 0xFF)) / 2;
            int avgG = ((currentColor >> 8 & 0xFF) + (nextColor >> 8 & 0xFF)) / 2;
            int avgB = ((currentColor & 0xFF) + (nextColor & 0xFF)) / 2;
            int avgA = (int)(avgAlpha * 255.0F);
            int avgColorInt = avgA << 24 | avgR << 16 | avgG << 8 | avgB;
            float bsx = (float)(currentPos.x - cameraPos.x);
            float bsy = (float)(currentPos.y - cameraPos.y) + down;
            float bsz = (float)(currentPos.z - cameraPos.z);
            float bex = (float)(nextPos.x - cameraPos.x);
            float bey = (float)(nextPos.y - cameraPos.y) + down;
            float bez = (float)(nextPos.z - cameraPos.z);
            buffer.vertex(entry.getPositionMatrix(), bsx, bsy, bsz).color(avgColorInt).normal(entry, bex - bsx, bey - bsy, bez - bsz);
            buffer.vertex(entry.getPositionMatrix(), bex, bey, bez).color(avgColorInt).normal(entry, bex - bsx, bey - bsy, bez - bsz);
            if (!renderHalf) {
               float tsx = (float)(currentPos.x - cameraPos.x);
               float tsy = (float)(currentPos.y - cameraPos.y) + currentTopY;
               float tsz = (float)(currentPos.z - cameraPos.z);
               float tex = (float)(nextPos.x - cameraPos.x);
               float tey = (float)(nextPos.y - cameraPos.y) + nextTopY;
               float tez = (float)(nextPos.z - cameraPos.z);
               buffer.vertex(entry.getPositionMatrix(), tsx, tsy, tsz).color(avgColorInt).normal(entry, tex - tsx, tey - tsy, tez - tsz);
               buffer.vertex(entry.getPositionMatrix(), tex, tey, tez).color(avgColorInt).normal(entry, tex - tsx, tey - tsy, tez - tsz);
            }
         }
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableDepthTest();
      RenderSystem.lineWidth(1.0F);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   private static void renderTailBatch() {
      Camera camera = mc.gameRenderer.getCamera();
      RenderSystem.setShaderTexture(0, Identifier.of("textures/particles/firefly.png"));
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      float alpha = 1.0F;
      float cameraPitch = camera.getPitch();
      float cameraYaw = camera.getYaw();
      Vec3d cameraPos = camera.getPos();
      Matrix4f billboardRotation = new Matrix4f().rotateX((float)Math.toRadians(cameraPitch)).rotateY((float)Math.toRadians(cameraYaw + 180.0F));
      Matrix4f inverseBillboard = new Matrix4f().rotateY((float)Math.toRadians(-cameraYaw)).rotateX((float)Math.toRadians(cameraPitch));
      Matrix4f matrix = new Matrix4f();
      float sc = 0.6F;

      while (!TAIL_QUEUE.isEmpty()) {
         TrailRenderer.TailData data = TAIL_QUEUE.poll();
         List<Trails.Trail> trails = data.trails;
         Trails module = data.module;

         for (Trails.Trail ctx : trails) {
            Vec3d pos = ctx.currentPos;
            matrix.set(billboardRotation)
               .translate((float)(pos.x - cameraPos.x), (float)(pos.y - cameraPos.y + 0.9F), (float)(pos.z - cameraPos.z))
               .mul(inverseBillboard);
            float animAlpha = alpha * ctx.getAlpha();
            if (module.getColorMode().isSelected("Custom")) {
               int[] colors = module.getCustomColors();
               if (colors != null && colors.length > 0) {
                  if (module.getColorAnimation().isSelected("Wave")) {
                     int color = getWaveColor(colors, module, animAlpha);
                     bufferBuilder.vertex(matrix, -sc, sc, 0.0F).texture(0.0F, 1.0F).color(color);
                     bufferBuilder.vertex(matrix, sc, sc, 0.0F).texture(1.0F, 1.0F).color(color);
                     bufferBuilder.vertex(matrix, sc, -sc, 0.0F).texture(1.0F, 0.0F).color(color);
                     bufferBuilder.vertex(matrix, -sc, -sc, 0.0F).texture(0.0F, 0.0F).color(color);
                  } else {
                     int color1 = getVertexGradientColor(0, colors, module, animAlpha);
                     int color2 = getVertexGradientColor(90, colors, module, animAlpha);
                     int color3 = getVertexGradientColor(180, colors, module, animAlpha);
                     int color4 = getVertexGradientColor(270, colors, module, animAlpha);
                     bufferBuilder.vertex(matrix, -sc, sc, 0.0F).texture(0.0F, 1.0F).color(color1);
                     bufferBuilder.vertex(matrix, sc, sc, 0.0F).texture(1.0F, 1.0F).color(color2);
                     bufferBuilder.vertex(matrix, sc, -sc, 0.0F).texture(1.0F, 0.0F).color(color3);
                     bufferBuilder.vertex(matrix, -sc, -sc, 0.0F).texture(0.0F, 0.0F).color(color4);
                  }
               } else {
                  int col = ctx.color().getRGB();
                  int animatedAlpha = (int)(animAlpha * 255.0F);
                  int finalColor = ColorUtil.replAlpha(col, animatedAlpha);
                  bufferBuilder.vertex(matrix, -sc, sc, 0.0F).texture(0.0F, 1.0F).color(finalColor);
                  bufferBuilder.vertex(matrix, sc, sc, 0.0F).texture(1.0F, 1.0F).color(finalColor);
                  bufferBuilder.vertex(matrix, sc, -sc, 0.0F).texture(1.0F, 0.0F).color(finalColor);
                  bufferBuilder.vertex(matrix, -sc, -sc, 0.0F).texture(0.0F, 0.0F).color(finalColor);
               }
            } else {
               Color col = ctx.color();
               float colorFactor = ctx.getProgress();
               int interpolated = interpolateColorRgb(col, Color.WHITE, (float)Math.pow(1.0F - colorFactor, 2.0));
               int animatedAlpha = (int)(animAlpha * 255.0F);
               int finalColor = ColorUtil.replAlpha(interpolated, animatedAlpha);
               bufferBuilder.vertex(matrix, -sc, sc, 0.0F).texture(0.0F, 1.0F).color(finalColor);
               bufferBuilder.vertex(matrix, sc, sc, 0.0F).texture(1.0F, 1.0F).color(finalColor);
               bufferBuilder.vertex(matrix, sc, -sc, 0.0F).texture(1.0F, 0.0F).color(finalColor);
               bufferBuilder.vertex(matrix, -sc, -sc, 0.0F).texture(0.0F, 0.0F).color(finalColor);
            }
         }
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
   }

   private static int getWaveColor(int[] colors, Trails module, float alpha) {
      if (colors.length == 1) {
         return ColorUtil.multAlpha(colors[0], alpha);
      } else {
         int angle = (int)(cachedTime / 8L % 360L);
         if (colors.length == 2) {
            angle = angle >= 180 ? 360 - angle : angle;
            return ColorUtil.multAlpha(ColorUtil.overCol(colors[0], colors[1], angle / 180.0F), alpha);
         } else {
            float timeProgress = (float)(cachedTime / 10L % (colors.length * 360L)) / 360.0F;
            int index1 = (int)Math.floor(timeProgress) % colors.length;
            int index2 = (index1 + 1) % colors.length;
            float lerp = timeProgress - (int)Math.floor(timeProgress);
            return ColorUtil.multAlpha(ColorUtil.overCol(colors[index1], colors[index2], lerp), alpha);
         }
      }
   }

   private static int getVertexGradientColor(int angleOffset, int[] colors, Trails module, float alpha) {
      if (colors.length == 1) {
         return ColorUtil.multAlpha(colors[0], alpha);
      } else {
         int angle = (int)((cachedTime / 8L + angleOffset) % 360L);
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
   }

   private static int interpolateColorRgb(Color c1, Color c2, float percent) {
      int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * percent);
      int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * percent);
      int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * percent);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private static int computeFadedAlpha(float yOffset, float height) {
      float yRelative = yOffset / height;
      return yRelative <= 0.5F ? (int)((1.0F - yRelative / 0.5F) * 255.0F) : (int)((yRelative - 0.5F) / 0.5F * 255.0F);
   }

   private static int computeFadedAlphaInvert(float yOffset, float height, float alphaFactorPercent) {
      float yRelative = yOffset / height;
      int alphaFactor = (int)(255.0F * (alphaFactorPercent / 100.0F));
      return yRelative <= 0.5F ? (int)(alphaFactor + yRelative / 0.5F * (255 - alphaFactor)) : (int)(255.0F - (yRelative - 0.5F) / 0.5F * (255 - alphaFactor));
   }

   private static Color getAnimatedColor(Trails.Trail trail, Trails module, float x, float y) {
      if (module.getColorMode().isSelected("Custom")) {
         int[] colors = module.getCustomColors();
         if (colors != null && colors.length > 0) {
            if (module.getColorAnimation().isSelected("Wave")) {
               int color = getWaveColor(colors, module, 1.0F);
               return new Color(color, true);
            } else {
               float colorIndex = x * colors.length;
               int index1 = Math.min((int)colorIndex, colors.length - 1);
               int index2 = Math.min(index1 + 1, colors.length - 1);
               float lerp = colorIndex - (int)colorIndex;
               int color = ColorUtil.overCol(colors[index1], colors[index2], lerp);
               return new Color(color, true);
            }
         } else {
            return trail.color();
         }
      } else {
         Color c1 = TargetRenderer.topLeft;
         Color c2 = TargetRenderer.topRight;
         Color c3 = TargetRenderer.bottomRight;
         Color c4 = TargetRenderer.bottomLeft;
         if (c1 == null) {
            c1 = new Color(ColorUtil.getClientColor());
         }

         if (c2 == null) {
            c2 = c1;
         }

         if (c3 == null) {
            c3 = c1;
         }

         if (c4 == null) {
            c4 = c1;
         }

         Color top = lerpColor(c1, c2, x);
         Color bottom = lerpColor(c4, c3, x);
         return lerpColor(top, bottom, y);
      }
   }

   private static int getAnimatedColorInt(Trails.Trail trail, Trails module, float x, float y) {
      if (module.getColorMode().isSelected("Custom")) {
         int[] colors = module.getCustomColors();
         if (colors != null && colors.length > 0) {
            if (module.getColorAnimation().isSelected("Wave")) {
               return getWaveColor(colors, module, 1.0F);
            } else {
               float colorIndex = x * colors.length;
               int index1 = Math.min((int)colorIndex, colors.length - 1);
               int index2 = Math.min(index1 + 1, colors.length - 1);
               float lerp = colorIndex - (int)colorIndex;
               return ColorUtil.overCol(colors[index1], colors[index2], lerp);
            }
         } else {
            return trail.color().getRGB();
         }
      } else {
         Color c1 = TargetRenderer.topLeft;
         Color c2 = TargetRenderer.topRight;
         Color c3 = TargetRenderer.bottomRight;
         Color c4 = TargetRenderer.bottomLeft;
         if (c1 == null) {
            return ColorUtil.getClientColor();
         } else {
            if (c2 == null) {
               c2 = c1;
            }

            if (c3 == null) {
               c3 = c1;
            }

            if (c4 == null) {
               c4 = c1;
            }

            int topColor = lerpColorInt(c1.getRGB(), c2.getRGB(), x);
            int bottomColor = lerpColorInt(c4.getRGB(), c3.getRGB(), x);
            return lerpColorInt(topColor, bottomColor, y);
         }
      }
   }

   private static Color lerpColor(Color a, Color b, float t) {
      int r = (int)(a.getRed() + (b.getRed() - a.getRed()) * t);
      int g = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
      int b_ = (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
      return new Color(r, g, b_);
   }

   private static int lerpColorInt(int colorA, int colorB, float t) {
      int aR = colorA >> 16 & 0xFF;
      int aG = colorA >> 8 & 0xFF;
      int aB = colorA & 0xFF;
      int bR = colorB >> 16 & 0xFF;
      int bG = colorB >> 8 & 0xFF;
      int bB = colorB & 0xFF;
      int r = (int)(aR + (bR - aR) * t);
      int g = (int)(aG + (bG - aG) * t);
      int b = (int)(aB + (bB - aB) * t);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private static Matrix4f getRotationOnlyMatrix(Matrix4f source) {
      return new Matrix4f(source).setTranslation(0.0F, 0.0F, 0.0F);
   }

   private TrailRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   private record DebugLineData(Matrix4f matrix, List<Trails.Trail> trails, Trails module, PlayerEntity entity) {
   }

   private record TailData(Matrix4f matrix, List<Trails.Trail> trails, Trails module) {
   }

   private record TrailData(Matrix4f matrix, List<Trails.Trail> trails, Trails module, PlayerEntity entity) {
   }
}
