package padej.soup.api.system.shape.implement;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;

public final class ModulesOutline implements QuickImports {
   public static final int MAX_ROWS = 32;
   private static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
      Identifier.of("minecraft", "core/modules_outline"), VertexFormats.POSITION, Defines.EMPTY
   );
   private final float[] rowsData = new float[128];
   private final float[] radiiData = new float[128];
   private static final Vector3f SCRATCH_SCALE = new Vector3f();
   private static final Vector3f SCRATCH_POS = new Vector3f();

   public void render(MatrixStack matrix, List<ModulesOutline.Row> rows, float outlineWidth, int color) {
      if (!rows.isEmpty()) {
         int count = Math.min(rows.size(), 32);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         float scale = (float)mc.getWindow().getScaleFactor();
         float alpha = RenderSystem.getShaderColor()[3];
         Matrix4f matrix4f = matrix.peek().getPositionMatrix();
         Vector3f s = matrix4f.getScale(SCRATCH_SCALE).mul(scale);
         float minX = Float.POSITIVE_INFINITY;
         float minY = Float.POSITIVE_INFINITY;
         float maxX = Float.NEGATIVE_INFINITY;
         float maxY = Float.NEGATIVE_INFINITY;

         for (int i = 0; i < count; i++) {
            ModulesOutline.Row r = rows.get(i);
            if (r.x < minX) {
               minX = r.x;
            }

            if (r.y < minY) {
               minY = r.y;
            }

            if (r.x + r.w > maxX) {
               maxX = r.x + r.w;
            }

            if (r.y + r.h > maxY) {
               maxY = r.y + r.h;
            }
         }

         float pad = outlineWidth + 2.0F;
         minX -= pad;
         minY -= pad;
         maxX += pad;
         maxY += pad;
         BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION);
         drawEngine.quad(matrix4f, buffer, minX, minY, maxX - minX, maxY - minY);

         for (int i = 0; i < count; i++) {
            ModulesOutline.Row rx = rows.get(i);
            Vector3f pos = matrix4f.transformPosition(rx.x, rx.y, 0.0F, SCRATCH_POS).mul(scale);
            float rwPx = rx.w * s.x;
            float rhPx = rx.h * s.y;
            int idx = i * 4;
            this.rowsData[idx + 0] = pos.x;
            this.rowsData[idx + 1] = window.getHeight() - rhPx - pos.y;
            this.rowsData[idx + 2] = rwPx;
            this.rowsData[idx + 3] = rhPx;
            this.radiiData[idx + 0] = rx.rTR * s.y;
            this.radiiData[idx + 1] = rx.rBR * s.y;
            this.radiiData[idx + 2] = rx.rTL * s.y;
            this.radiiData[idx + 3] = rx.rBL * s.y;
         }

         for (int i = count; i < 32; i++) {
            int idx = i * 4;
            this.rowsData[idx] = 0.0F;
            this.rowsData[idx + 1] = 0.0F;
            this.rowsData[idx + 2] = 0.0F;
            this.rowsData[idx + 3] = 0.0F;
            this.radiiData[idx] = 0.0F;
            this.radiiData[idx + 1] = 0.0F;
            this.radiiData[idx + 2] = 0.0F;
            this.radiiData[idx + 3] = 0.0F;
         }

         ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
         if (shader != null) {
            shader.getUniformOrDefault("rowCount").set(count);
            shader.getUniformOrDefault("outlineWidth").set(outlineWidth);
            shader.getUniformOrDefault("outlineColor")
               .set(ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color), ColorUtil.alphaf(ColorUtil.multAlpha(color, alpha)));
            shader.getUniformOrDefault("rows").set(this.rowsData);
            shader.getUniformOrDefault("radii").set(this.radiiData);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.disableBlend();
         }
      }
   }

   public static final class Row {
      public final float x;
      public final float y;
      public final float w;
      public final float h;
      public final float rTR;
      public final float rBR;
      public final float rTL;
      public final float rBL;

      public Row(float x, float y, float w, float h, float rTR, float rBR, float rTL, float rBL) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.rTR = rTR;
         this.rBR = rBR;
         this.rTL = rTL;
         this.rBL = rBL;
      }
   }
}
