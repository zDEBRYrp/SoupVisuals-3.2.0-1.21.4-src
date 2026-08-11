package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;

public class ChinaHatBatchRenderer implements QuickImports {
   private static final ConcurrentLinkedQueue<ChinaHatBatchRenderer.HatRenderData> HAT_QUEUE = new ConcurrentLinkedQueue<>();
   private static final ConcurrentLinkedQueue<ChinaHatBatchRenderer.OutlineLineData> OUTLINE_QUEUE = new ConcurrentLinkedQueue<>();
   private static final MatrixStack SCRATCH_MATRIX = new MatrixStack();
   public static final RenderLayer HAT_LAYER = RenderLayer.of(
      "china_hat_batch",
      VertexFormats.POSITION_COLOR,
      DrawMode.TRIANGLES,
      1536,
      MultiPhaseParameters.builder().program(RenderPhase.POSITION_COLOR_PROGRAM).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).build(false)
   );

   public static void queueHat(
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int step,
      float centerY,
      float brimY,
      float radius,
      float centerAlpha,
      float edgeAlpha,
      int[] gradientColors
   ) {
      Matrix4f positionMatrix = new Matrix4f(matrices.peek().getPositionMatrix());
      HAT_QUEUE.add(
         new ChinaHatBatchRenderer.HatRenderData(positionMatrix, vertexConsumers, step, centerY, brimY, radius, centerAlpha, edgeAlpha, gradientColors)
      );
   }

   public static void renderBatches() {
      if (!HAT_QUEUE.isEmpty()) {
         while (!HAT_QUEUE.isEmpty()) {
            ChinaHatBatchRenderer.HatRenderData data = HAT_QUEUE.poll();
            renderSingleHat(data);
         }

         renderBatchedOutlines();
      }
   }

   private static void renderSingleHat(ChinaHatBatchRenderer.HatRenderData data) {
      Matrix4f matrix = data.positionMatrix;
      VertexConsumer vertexConsumer = data.vertexConsumers.getBuffer(HAT_LAYER);

      for (int i = 0; i < 360; i += data.step) {
         float angle1 = i * (float) (Math.PI / 180.0);
         float angle2 = (i + data.step) * (float) (Math.PI / 180.0);
         float x1 = MathHelper.sin(angle1) * data.radius;
         float z1 = -MathHelper.cos(angle1) * data.radius;
         float x2 = MathHelper.sin(angle2) * data.radius;
         float z2 = -MathHelper.cos(angle2) * data.radius;
         int color1 = getGradientColor(i, data.gradientColors);
         int color2 = getGradientColor(i + data.step, data.gradientColors);
         int centerColor = getGradientColor(i, data.gradientColors);
         int alphaColor1 = color1 & 16777215 | (int)(data.edgeAlpha * 255.0F) << 24;
         int alphaColor2 = color2 & 16777215 | (int)(data.edgeAlpha * 255.0F) << 24;
         int alphaCenterColor = centerColor & 16777215 | (int)(data.centerAlpha * 255.0F) << 24;
         vertexConsumer.vertex(matrix, 0.0F, data.centerY, 0.0F).color(alphaCenterColor);
         vertexConsumer.vertex(matrix, x1, data.brimY, z1).color(alphaColor1);
         vertexConsumer.vertex(matrix, x2, data.brimY, z2).color(alphaColor2);
      }

      for (int i = 0; i < 360; i += data.step) {
         float angle1 = i * (float) (Math.PI / 180.0);
         float angle2 = (i + data.step) * (float) (Math.PI / 180.0);
         float x1 = MathHelper.sin(angle1) * data.radius;
         float z1 = -MathHelper.cos(angle1) * data.radius;
         float x2 = MathHelper.sin(angle2) * data.radius;
         float z2 = -MathHelper.cos(angle2) * data.radius;
         int color1 = ColorUtil.darker(getGradientColor(i, data.gradientColors), 4);
         int color2 = ColorUtil.darker(getGradientColor(i + data.step, data.gradientColors), 4);
         int centerColor = ColorUtil.darker(getGradientColor(i, data.gradientColors));
         int alphaColor1 = color1 & 16777215 | (int)(data.edgeAlpha * 255.0F) << 24;
         int alphaColor2 = color2 & 16777215 | (int)(data.edgeAlpha * 255.0F) << 24;
         int alphaCenterColor = centerColor & 16777215 | (int)(data.centerAlpha * 255.0F) << 24;
         vertexConsumer.vertex(matrix, x2, data.brimY, z2).color(alphaColor2);
         vertexConsumer.vertex(matrix, x1, data.brimY, z1).color(alphaColor1);
         vertexConsumer.vertex(matrix, 0.0F, data.centerY, 0.0F).color(alphaCenterColor);
      }

      queueOutlineLines(data);
   }

   private static void queueOutlineLines(ChinaHatBatchRenderer.HatRenderData data) {
      if (!(mc.currentScreen instanceof CreativeInventoryScreen) && !(mc.currentScreen instanceof InventoryScreen)) {
         int i = 0;

         while (i < 360) {
            float x1 = MathHelper.sin(i * (float) (Math.PI / 180.0)) * data.radius;
            float z1 = -MathHelper.cos(i * (float) (Math.PI / 180.0)) * data.radius;
            float x2 = MathHelper.sin((i + data.step) * (float) (Math.PI / 180.0)) * data.radius;
            float z2 = -MathHelper.cos((i + data.step) * (float) (Math.PI / 180.0)) * data.radius;
            Vector3f start = new Vector3f(x1, data.brimY, z1);
            Vector3f end = new Vector3f(x2, data.brimY, z2);
            int color1 = getGradientColor(i, data.gradientColors);
            int color2 = getGradientColor(i + data.step, data.gradientColors);
            int darkerColor1 = ColorUtil.darker(color1) | 0xFF000000;
            int darkerColor2 = ColorUtil.darker(color2) | 0xFF000000;
            OUTLINE_QUEUE.add(new ChinaHatBatchRenderer.OutlineLineData(data.positionMatrix, start, end, darkerColor1, darkerColor2));
            i += data.step;
         }
      }
   }

   private static void renderBatchedOutlines() {
      if (!OUTLINE_QUEUE.isEmpty()) {
         GL11.glHint(3154, 4354);
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
         RenderSystem.lineWidth(4.0F);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);

         while (!OUTLINE_QUEUE.isEmpty()) {
            ChinaHatBatchRenderer.OutlineLineData lineData = OUTLINE_QUEUE.poll();
            SCRATCH_MATRIX.loadIdentity();
            SCRATCH_MATRIX.peek().getPositionMatrix().set(lineData.positionMatrix);
            Entry entry = SCRATCH_MATRIX.peek();
            Render3DUtil.vertexLine(entry, buffer, lineData.start, lineData.end, lineData.colorStart, lineData.colorEnd);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.lineWidth(1.0F);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   private static int getGradientColor(float angle, int[] customColors) {
      if (customColors != null && customColors.length > 0) {
         float normalizedAngle = angle % 360.0F;
         if (normalizedAngle < 0.0F) {
            normalizedAngle += 360.0F;
         }

         float degreesPerColor = 360.0F / customColors.length;
         int colorIndex1 = (int)(normalizedAngle / degreesPerColor) % customColors.length;
         int colorIndex2 = (colorIndex1 + 1) % customColors.length;
         float factor = normalizedAngle % degreesPerColor / degreesPerColor;
         int color1 = customColors[colorIndex1];
         int color2 = customColors[colorIndex2];
         return interpolateColor(color1, color2, factor);
      } else {
         return ColorUtil.getClientColor();
      }
   }

   private static int interpolateColor(int color1, int color2, float factor) {
      int a1 = color1 >> 24 & 0xFF;
      int r1 = color1 >> 16 & 0xFF;
      int g1 = color1 >> 8 & 0xFF;
      int b1 = color1 & 0xFF;
      int a2 = color2 >> 24 & 0xFF;
      int r2 = color2 >> 16 & 0xFF;
      int g2 = color2 >> 8 & 0xFF;
      int b2 = color2 & 0xFF;
      int a = (int)(a1 + (a2 - a1) * factor);
      int r = (int)(r1 + (r2 - r1) * factor);
      int g = (int)(g1 + (g2 - g1) * factor);
      int b = (int)(b1 + (b2 - b1) * factor);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private record HatRenderData(
      Matrix4f positionMatrix,
      VertexConsumerProvider vertexConsumers,
      int step,
      float centerY,
      float brimY,
      float radius,
      float centerAlpha,
      float edgeAlpha,
      int[] gradientColors
   ) {
   }

   private record OutlineLineData(Matrix4f positionMatrix, Vector3f start, Vector3f end, int colorStart, int colorEnd) {
   }
}
