package padej.soup.implement.features.modules.particles.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import padej.soup.api.feature.module.IParticleModule;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.particle.ParticleColorUtil;

public class ParticleBatchRenderer implements QuickImports {
   private static final int MATRIX_POOL_SIZE = 256;
   private static final Queue<Matrix4f> MATRIX_POOL = new ArrayBlockingQueue<>(256);
   private static final ArrayDeque<ParticleBatchRenderer.TexturedParticleData> TEXTURED_PARTICLES;
   private static final ArrayDeque<ParticleBatchRenderer.CubeParticleData> CUBE_PARTICLES;
   private static final ArrayDeque<ParticleBatchRenderer.PyramidParticleData> PYRAMID_PARTICLES;
   private static final ArrayDeque<ParticleBatchRenderer.TextParticleData> TEXT_PARTICLES;
   private static final ArrayDeque<ParticleBatchRenderer.BloomParticleData> BLOOM_PARTICLES;
   private static final ArrayDeque<ParticleBatchRenderer.FireflyTrailData> FIREFLY_TRAILS;
   private static final ArrayDeque<ParticleBatchRenderer.BubbleParticleData> BUBBLE_PARTICLES;
   private static final ArrayDeque<ParticleBatchRenderer.GhostParticleData> GHOST_PARTICLES;
   private static final ArrayDeque<ParticleBatchRenderer.AmbientTexturedParticleData> AMBIENT_TEXTURED_PARTICLES;
   private static final Map<Identifier, List<ParticleBatchRenderer.TexturedParticleData>> TEXTURED_GROUPS;
   private static final Map<Identifier, List<ParticleBatchRenderer.AmbientTexturedParticleData>> AMBIENT_GROUPS;
   private static final Map<Identifier, List<ParticleBatchRenderer.BubbleParticleData>> BUBBLE_GROUPS;
   private static final Map<ParticleBatchRenderer.GhostGroupKey, List<ParticleBatchRenderer.GhostParticleData>> GHOST_GROUPS;
   private static final float SQRT_3;
   private static final float SQRT_6;

   private static Matrix4f acquireMatrix(Matrix4f source) {
      Matrix4f matrix = MATRIX_POOL.poll();
      if (matrix == null) {
         matrix = new Matrix4f();
      }

      return matrix.set(source);
   }

   private static void releaseMatrix(Matrix4f matrix) {
      if (MATRIX_POOL.size() < 256) {
         MATRIX_POOL.offer(matrix);
      }
   }

   public static void queueTexturedParticle(
      MatrixStack matrices, Identifier texture, float size, float alpha, int color, boolean useVertexGradient, int[] gradientColors, IParticleModule module
   ) {
      TEXTURED_PARTICLES.add(
         new ParticleBatchRenderer.TexturedParticleData(
            acquireMatrix(matrices.peek().getPositionMatrix()), texture, size, alpha, color, useVertexGradient, gradientColors, module
         )
      );
   }

   public static void queueCubeParticle(MatrixStack matrices, float size, float alpha, int color) {
      CUBE_PARTICLES.add(new ParticleBatchRenderer.CubeParticleData(acquireMatrix(matrices.peek().getPositionMatrix()), size, alpha, color));
   }

   public static void queuePyramidParticle(MatrixStack matrices, float size, float alpha, int color) {
      PYRAMID_PARTICLES.add(new ParticleBatchRenderer.PyramidParticleData(acquireMatrix(matrices.peek().getPositionMatrix()), size, alpha, color));
   }

   public static void queueTextParticle(MatrixStack matrices, String text, int color) {
      TEXT_PARTICLES.add(new ParticleBatchRenderer.TextParticleData(acquireMatrix(matrices.peek().getPositionMatrix()), text, color));
   }

   public static void queueBloomParticle(
      MatrixStack matrices, float size, float alpha, boolean useVertexGradient, int[] gradientColors, IParticleModule module, int colorOffset
   ) {
      BLOOM_PARTICLES.add(
         new ParticleBatchRenderer.BloomParticleData(
            acquireMatrix(matrices.peek().getPositionMatrix()), size, alpha, useVertexGradient, gradientColors, module, colorOffset
         )
      );
   }

   public static void queueFireflyTrail(Matrix4f matrix, float size, int color) {
      FIREFLY_TRAILS.add(new ParticleBatchRenderer.FireflyTrailData(acquireMatrix(matrix), size, color));
   }

   public static void queueBubble(MatrixStack matrices, Identifier texture, float scale, int color1, int color2, int color3, int color4) {
      BUBBLE_PARTICLES.add(
         new ParticleBatchRenderer.BubbleParticleData(acquireMatrix(matrices.peek().getPositionMatrix()), texture, scale, color1, color2, color3, color4)
      );
   }

   public static void queueGhost(Matrix4f matrix, Identifier texture, float scale, int color, String blendMode) {
      GHOST_PARTICLES.add(new ParticleBatchRenderer.GhostParticleData(acquireMatrix(matrix), texture, scale, color, blendMode));
   }

   public static void renderBatches() {
      if (!BLOOM_PARTICLES.isEmpty()) {
         renderBloomBatch();
      }

      if (!TEXTURED_PARTICLES.isEmpty()) {
         renderTexturedBatch();
      }

      if (!AMBIENT_TEXTURED_PARTICLES.isEmpty()) {
         renderAmbientTexturedBatch();
      }

      if (!FIREFLY_TRAILS.isEmpty()) {
         renderFireflyBatch();
      }

      if (!CUBE_PARTICLES.isEmpty()) {
         renderCubeBatch();
      }

      if (!PYRAMID_PARTICLES.isEmpty()) {
         renderPyramidBatch();
      }

      if (!TEXT_PARTICLES.isEmpty()) {
         renderTextBatch();
      }

      if (!BUBBLE_PARTICLES.isEmpty()) {
         renderBubbleBatch();
      }

      if (!GHOST_PARTICLES.isEmpty()) {
         renderGhostBatch();
      }
   }

   private static void renderTexturedBatch() {
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.depthMask(false);
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      TEXTURED_GROUPS.values().forEach(List::clear);

      while (!TEXTURED_PARTICLES.isEmpty()) {
         ParticleBatchRenderer.TexturedParticleData data = TEXTURED_PARTICLES.poll();
         TEXTURED_GROUPS.computeIfAbsent(data.texture, k -> new ArrayList<>()).add(data);
      }

      TEXTURED_GROUPS.forEach((texture, particles) -> {
         if (!particles.isEmpty()) {
            RenderSystem.setShaderTexture(0, texture);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (ParticleBatchRenderer.TexturedParticleData datax : particles) {
               if (datax.useVertexGradient && datax.gradientColors != null) {
                  int col1 = ParticleColorUtil.getVertexGradientColor(0, datax.gradientColors, datax.alpha);
                  int col2 = ParticleColorUtil.getVertexGradientColor(90, datax.gradientColors, datax.alpha);
                  int col3 = ParticleColorUtil.getVertexGradientColor(180, datax.gradientColors, datax.alpha);
                  int col4 = ParticleColorUtil.getVertexGradientColor(270, datax.gradientColors, datax.alpha);
                  bufferBuilder.vertex(datax.matrix, 0.0F, datax.size, 0.0F).texture(0.0F, 1.0F).color(col1);
                  bufferBuilder.vertex(datax.matrix, datax.size, datax.size, 0.0F).texture(1.0F, 1.0F).color(col2);
                  bufferBuilder.vertex(datax.matrix, datax.size, 0.0F, 0.0F).texture(1.0F, 0.0F).color(col3);
                  bufferBuilder.vertex(datax.matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(col4);
               } else {
                  bufferBuilder.vertex(datax.matrix, 0.0F, datax.size, 0.0F).texture(0.0F, 1.0F).color(datax.color);
                  bufferBuilder.vertex(datax.matrix, datax.size, datax.size, 0.0F).texture(1.0F, 1.0F).color(datax.color);
                  bufferBuilder.vertex(datax.matrix, datax.size, 0.0F, 0.0F).texture(1.0F, 0.0F).color(datax.color);
                  bufferBuilder.vertex(datax.matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(datax.color);
               }

               releaseMatrix(datax.matrix);
            }

            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }
      });
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.disableDepthTest();
   }

   private static void renderCubeBatch() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      RenderSystem.enableDepthTest();
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      while (!CUBE_PARTICLES.isEmpty()) {
         ParticleBatchRenderer.CubeParticleData data = CUBE_PARTICLES.poll();
         float half = data.size / 2.0F;
         int color = data.color;
         bufferBuilder.vertex(data.matrix, -half, -half, -half).color(color);
         bufferBuilder.vertex(data.matrix, half, -half, -half).color(color);
         bufferBuilder.vertex(data.matrix, half, -half, -half).color(color);
         bufferBuilder.vertex(data.matrix, half, -half, half).color(color);
         bufferBuilder.vertex(data.matrix, half, -half, half).color(color);
         bufferBuilder.vertex(data.matrix, -half, -half, half).color(color);
         bufferBuilder.vertex(data.matrix, -half, -half, half).color(color);
         bufferBuilder.vertex(data.matrix, -half, -half, -half).color(color);
         bufferBuilder.vertex(data.matrix, -half, half, -half).color(color);
         bufferBuilder.vertex(data.matrix, half, half, -half).color(color);
         bufferBuilder.vertex(data.matrix, half, half, -half).color(color);
         bufferBuilder.vertex(data.matrix, half, half, half).color(color);
         bufferBuilder.vertex(data.matrix, half, half, half).color(color);
         bufferBuilder.vertex(data.matrix, -half, half, half).color(color);
         bufferBuilder.vertex(data.matrix, -half, half, half).color(color);
         bufferBuilder.vertex(data.matrix, -half, half, -half).color(color);
         bufferBuilder.vertex(data.matrix, -half, -half, -half).color(color);
         bufferBuilder.vertex(data.matrix, -half, half, -half).color(color);
         bufferBuilder.vertex(data.matrix, half, -half, -half).color(color);
         bufferBuilder.vertex(data.matrix, half, half, -half).color(color);
         bufferBuilder.vertex(data.matrix, half, -half, half).color(color);
         bufferBuilder.vertex(data.matrix, half, half, half).color(color);
         bufferBuilder.vertex(data.matrix, -half, -half, half).color(color);
         bufferBuilder.vertex(data.matrix, -half, half, half).color(color);
         bufferBuilder.vertex(data.matrix, -half, -half, -half).color(color);
         bufferBuilder.vertex(data.matrix, half, half, half).color(color);
         bufferBuilder.vertex(data.matrix, half, -half, -half).color(color);
         bufferBuilder.vertex(data.matrix, -half, half, half).color(color);
         bufferBuilder.vertex(data.matrix, half, -half, half).color(color);
         bufferBuilder.vertex(data.matrix, -half, half, -half).color(color);
         bufferBuilder.vertex(data.matrix, -half, -half, half).color(color);
         bufferBuilder.vertex(data.matrix, half, half, -half).color(color);
         releaseMatrix(data.matrix);
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
      RenderSystem.disableDepthTest();
   }

   private static void renderPyramidBatch() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      RenderSystem.enableDepthTest();
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      while (!PYRAMID_PARTICLES.isEmpty()) {
         ParticleBatchRenderer.PyramidParticleData data = PYRAMID_PARTICLES.poll();
         int color = data.color;
         float v1x = 0.0F;
         float v1y = 0.0F;
         float v1z = data.size * SQRT_6 / 4.0F;
         float v2x = -data.size / 2.0F;
         float v2y = -data.size * SQRT_3 / 6.0F;
         float v2z = -data.size * SQRT_6 / 12.0F;
         float v3x = data.size / 2.0F;
         float v3y = -data.size * SQRT_3 / 6.0F;
         float v3z = -data.size * SQRT_6 / 12.0F;
         float v4x = 0.0F;
         float v4y = data.size * SQRT_3 / 3.0F;
         float v4z = -data.size * SQRT_6 / 12.0F;
         bufferBuilder.vertex(data.matrix, v2x, v2y, v2z).color(color);
         bufferBuilder.vertex(data.matrix, v3x, v3y, v3z).color(color);
         bufferBuilder.vertex(data.matrix, v3x, v3y, v3z).color(color);
         bufferBuilder.vertex(data.matrix, v4x, v4y, v4z).color(color);
         bufferBuilder.vertex(data.matrix, v4x, v4y, v4z).color(color);
         bufferBuilder.vertex(data.matrix, v2x, v2y, v2z).color(color);
         bufferBuilder.vertex(data.matrix, v1x, v1y, v1z).color(color);
         bufferBuilder.vertex(data.matrix, v2x, v2y, v2z).color(color);
         bufferBuilder.vertex(data.matrix, v1x, v1y, v1z).color(color);
         bufferBuilder.vertex(data.matrix, v3x, v3y, v3z).color(color);
         bufferBuilder.vertex(data.matrix, v1x, v1y, v1z).color(color);
         bufferBuilder.vertex(data.matrix, v4x, v4y, v4z).color(color);
         releaseMatrix(data.matrix);
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
      RenderSystem.disableDepthTest();
   }

   private static void renderTextBatch() {
      while (!TEXT_PARTICLES.isEmpty()) {
         ParticleBatchRenderer.TextParticleData data = TEXT_PARTICLES.poll();
         float textX = -mc.textRenderer.getWidth(data.text) / 2.0F;
         mc.textRenderer
            .draw(data.text, textX, 0.0F, data.color, false, data.matrix, mc.getBufferBuilders().getEntityVertexConsumers(), TextLayerType.NORMAL, 0, 15728880);
         releaseMatrix(data.matrix);
      }
   }

   private static void renderBloomBatch() {
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.depthMask(false);
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.setShaderTexture(0, Identifier.of("textures/particles/bloom/bloom_small.png"));
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      while (!BLOOM_PARTICLES.isEmpty()) {
         ParticleBatchRenderer.BloomParticleData data = BLOOM_PARTICLES.poll();
         if (data.useVertexGradient && data.gradientColors != null) {
            int col1 = ParticleColorUtil.getVertexGradientColor(data.colorOffset + 0, data.gradientColors, data.alpha);
            int col2 = ParticleColorUtil.getVertexGradientColor(data.colorOffset + 90, data.gradientColors, data.alpha);
            int col3 = ParticleColorUtil.getVertexGradientColor(data.colorOffset + 180, data.gradientColors, data.alpha);
            int col4 = ParticleColorUtil.getVertexGradientColor(data.colorOffset + 270, data.gradientColors, data.alpha);
            bufferBuilder.vertex(data.matrix, 0.0F, data.size, 0.0F).texture(0.0F, 1.0F).color(col1);
            bufferBuilder.vertex(data.matrix, data.size, data.size, 0.0F).texture(1.0F, 1.0F).color(col2);
            bufferBuilder.vertex(data.matrix, data.size, 0.0F, 0.0F).texture(1.0F, 0.0F).color(col3);
            bufferBuilder.vertex(data.matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(col4);
         } else {
            int color;
            if (data.module.getColorMode().isSelected("Sync")) {
               color = ColorUtil.multAlpha(ColorUtil.getClientColor(), data.alpha);
            } else {
               int[] colors = data.module.getCustomColors();
               if (colors != null && colors.length > 0) {
                  color = ParticleColorUtil.getWaveColor(colors, data.alpha, data.colorOffset);
               } else {
                  color = ColorUtil.multAlpha(-1, data.alpha);
               }
            }

            bufferBuilder.vertex(data.matrix, 0.0F, data.size, 0.0F).texture(0.0F, 1.0F).color(color);
            bufferBuilder.vertex(data.matrix, data.size, data.size, 0.0F).texture(1.0F, 1.0F).color(color);
            bufferBuilder.vertex(data.matrix, data.size, 0.0F, 0.0F).texture(1.0F, 0.0F).color(color);
            bufferBuilder.vertex(data.matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(color);
         }

         releaseMatrix(data.matrix);
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.disableDepthTest();
   }

   private static void renderAmbientTexturedBatch() {
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.depthMask(false);
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      AMBIENT_GROUPS.values().forEach(List::clear);

      while (!AMBIENT_TEXTURED_PARTICLES.isEmpty()) {
         ParticleBatchRenderer.AmbientTexturedParticleData data = AMBIENT_TEXTURED_PARTICLES.poll();
         AMBIENT_GROUPS.computeIfAbsent(data.texture, k -> new ArrayList<>()).add(data);
      }

      AMBIENT_GROUPS.forEach((texture, particles) -> {
         if (!particles.isEmpty()) {
            RenderSystem.setShaderTexture(0, texture);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (ParticleBatchRenderer.AmbientTexturedParticleData datax : particles) {
               if (datax.useVertexGradient && datax.gradientColors != null) {
                  int col1 = ParticleColorUtil.getVertexGradientColor(0, datax.gradientColors, datax.alpha);
                  int col2 = ParticleColorUtil.getVertexGradientColor(90, datax.gradientColors, datax.alpha);
                  int col3 = ParticleColorUtil.getVertexGradientColor(180, datax.gradientColors, datax.alpha);
                  int col4 = ParticleColorUtil.getVertexGradientColor(270, datax.gradientColors, datax.alpha);
                  bufferBuilder.vertex(datax.matrix, 0.0F, datax.size, 0.0F).texture(0.0F, 1.0F).color(col1);
                  bufferBuilder.vertex(datax.matrix, datax.size, datax.size, 0.0F).texture(1.0F, 1.0F).color(col2);
                  bufferBuilder.vertex(datax.matrix, datax.size, 0.0F, 0.0F).texture(1.0F, 0.0F).color(col3);
                  bufferBuilder.vertex(datax.matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(col4);
               } else {
                  bufferBuilder.vertex(datax.matrix, 0.0F, datax.size, 0.0F).texture(0.0F, 1.0F).color(datax.color);
                  bufferBuilder.vertex(datax.matrix, datax.size, datax.size, 0.0F).texture(1.0F, 1.0F).color(datax.color);
                  bufferBuilder.vertex(datax.matrix, datax.size, 0.0F, 0.0F).texture(1.0F, 0.0F).color(datax.color);
                  bufferBuilder.vertex(datax.matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(datax.color);
               }

               releaseMatrix(datax.matrix);
            }

            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }
      });
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.disableDepthTest();
   }

   private static void renderFireflyBatch() {
      RenderSystem.setShaderTexture(0, Identifier.of("textures/particles/firefly.png"));
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      while (!FIREFLY_TRAILS.isEmpty()) {
         ParticleBatchRenderer.FireflyTrailData data = FIREFLY_TRAILS.poll();
         bufferBuilder.vertex(data.matrix, 0.0F, -data.size, 0.0F).texture(0.0F, 1.0F).color(data.color);
         bufferBuilder.vertex(data.matrix, -data.size, -data.size, 0.0F).texture(1.0F, 1.0F).color(data.color);
         bufferBuilder.vertex(data.matrix, -data.size, 0.0F, 0.0F).texture(1.0F, 0.0F).color(data.color);
         bufferBuilder.vertex(data.matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F).color(data.color);
         releaseMatrix(data.matrix);
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.disableDepthTest();
   }

   public static void queueAmbientTexturedParticle(
      MatrixStack matrices, Identifier texture, float size, float alpha, int color, boolean useVertexGradient, int[] gradientColors, IParticleModule module
   ) {
      AMBIENT_TEXTURED_PARTICLES.add(
         new ParticleBatchRenderer.AmbientTexturedParticleData(
            acquireMatrix(matrices.peek().getPositionMatrix()), texture, size, alpha, color, useVertexGradient, gradientColors, module
         )
      );
   }

   private static void renderBubbleBatch() {
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.depthMask(false);
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BUBBLE_GROUPS.values().forEach(List::clear);

      while (!BUBBLE_PARTICLES.isEmpty()) {
         ParticleBatchRenderer.BubbleParticleData data = BUBBLE_PARTICLES.poll();
         BUBBLE_GROUPS.computeIfAbsent(data.texture, k -> new ArrayList<>()).add(data);
      }

      BUBBLE_GROUPS.forEach((texture, bubbles) -> {
         if (!bubbles.isEmpty()) {
            RenderSystem.setShaderTexture(0, texture);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (ParticleBatchRenderer.BubbleParticleData datax : bubbles) {
               float half = datax.scale / 2.0F;
               bufferBuilder.vertex(datax.matrix, -half, half, 0.0F).texture(0.0F, 1.0F).color(datax.color1);
               bufferBuilder.vertex(datax.matrix, half, half, 0.0F).texture(1.0F, 1.0F).color(datax.color2);
               bufferBuilder.vertex(datax.matrix, half, -half, 0.0F).texture(1.0F, 0.0F).color(datax.color3);
               bufferBuilder.vertex(datax.matrix, -half, -half, 0.0F).texture(0.0F, 0.0F).color(datax.color4);
               releaseMatrix(datax.matrix);
            }

            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }
      });
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.disableDepthTest();
   }

   private static void renderGhostBatch() {
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      GHOST_GROUPS.values().forEach(List::clear);

      while (!GHOST_PARTICLES.isEmpty()) {
         ParticleBatchRenderer.GhostParticleData data = GHOST_PARTICLES.poll();
         GHOST_GROUPS.computeIfAbsent(new ParticleBatchRenderer.GhostGroupKey(data.texture, data.blendMode), k -> new ArrayList<>()).add(data);
      }

      GHOST_GROUPS.forEach((key, ghosts) -> {
         if (!ghosts.isEmpty()) {
            ParticleBatchRenderer.GhostParticleData firstGhost = ghosts.getFirst();
            RenderSystem.setShaderTexture(0, firstGhost.texture);
            switch (firstGhost.blendMode) {
               case "Smoke":
                  RenderSystem.blendFunc(SrcFactor.ONE, DstFactor.ONE_MINUS_SRC_COLOR);
                  break;
               case "Plasma":
                  RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
                  break;
               case null:
               default:
                  RenderSystem.defaultBlendFunc();
            }

            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (ParticleBatchRenderer.GhostParticleData datax : ghosts) {
               float half = datax.scale / 2.0F;
               bufferBuilder.vertex(datax.matrix, -half, half, 0.0F).texture(0.0F, 1.0F).color(datax.color);
               bufferBuilder.vertex(datax.matrix, half, half, 0.0F).texture(1.0F, 1.0F).color(datax.color);
               bufferBuilder.vertex(datax.matrix, half, -half, 0.0F).texture(1.0F, 0.0F).color(datax.color);
               bufferBuilder.vertex(datax.matrix, -half, -half, 0.0F).texture(0.0F, 0.0F).color(datax.color);
               releaseMatrix(datax.matrix);
            }

            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }
      });
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.disableDepthTest();
   }

   static {
      for (int i = 0; i < 256; i++) {
         MATRIX_POOL.offer(new Matrix4f());
      }

      TEXTURED_PARTICLES = new ArrayDeque<>();
      CUBE_PARTICLES = new ArrayDeque<>();
      PYRAMID_PARTICLES = new ArrayDeque<>();
      TEXT_PARTICLES = new ArrayDeque<>();
      BLOOM_PARTICLES = new ArrayDeque<>();
      FIREFLY_TRAILS = new ArrayDeque<>();
      BUBBLE_PARTICLES = new ArrayDeque<>();
      GHOST_PARTICLES = new ArrayDeque<>();
      AMBIENT_TEXTURED_PARTICLES = new ArrayDeque<>();
      TEXTURED_GROUPS = new HashMap<>();
      AMBIENT_GROUPS = new HashMap<>();
      BUBBLE_GROUPS = new HashMap<>();
      GHOST_GROUPS = new HashMap<>();
      SQRT_3 = (float)Math.sqrt(3.0);
      SQRT_6 = (float)Math.sqrt(6.0);
   }

   private record AmbientTexturedParticleData(
      Matrix4f matrix, Identifier texture, float size, float alpha, int color, boolean useVertexGradient, int[] gradientColors, IParticleModule module
   ) {
   }

   private record BloomParticleData(
      Matrix4f matrix, float size, float alpha, boolean useVertexGradient, int[] gradientColors, IParticleModule module, int colorOffset
   ) {
   }

   private record BubbleParticleData(Matrix4f matrix, Identifier texture, float scale, int color1, int color2, int color3, int color4) {
   }

   private record CubeParticleData(Matrix4f matrix, float size, float alpha, int color) {
   }

   private record FireflyTrailData(Matrix4f matrix, float size, int color) {
   }

   private record GhostGroupKey(Identifier texture, String blendMode) {
   }

   private record GhostParticleData(Matrix4f matrix, Identifier texture, float scale, int color, String blendMode) {
   }

   private record PyramidParticleData(Matrix4f matrix, float size, float alpha, int color) {
   }

   private record TextParticleData(Matrix4f matrix, String text, int color) {
   }

   private record TexturedParticleData(
      Matrix4f matrix, Identifier texture, float size, float alpha, int color, boolean useVertexGradient, int[] gradientColors, IParticleModule module
   ) {
   }
}
