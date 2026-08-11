package padej.soup.api.system.shape.implement;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.core.perftest.HudProfiler;

public final class RectangleBatch implements QuickImports, AutoCloseable {
   public static final int MAX_BATCH = 30;
   public static final int STRIDE = 33;
   private static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
      Identifier.of("minecraft", "core/round_batch"), VertexFormats.POSITION, Defines.EMPTY
   );
   private static final Vector3f SCRATCH_POS = new Vector3f();
   private static final Vector3f SCRATCH_SIZE = new Vector3f();
   private static final Vector3f SCRATCH_VERT = new Vector3f();
   private static final Matrix4f IDENTITY = new Matrix4f();
   private static RectangleBatch ACTIVE = null;
   private static final RectangleBatch INSTANCE = new RectangleBatch();
   private final float[] shapeData = new float[990];
   private final float[] quadCoords = new float[240];
   private int count = 0;
   private boolean closed = false;

   public static RectangleBatch active() {
      return ACTIVE;
   }

   public static RectangleBatch begin() {
      if (ACTIVE != null) {
         ACTIVE.end();
      }

      INSTANCE.count = 0;
      INSTANCE.closed = false;
      ACTIVE = INSTANCE;
      return INSTANCE;
   }

   private RectangleBatch() {
   }

   public void submit(ShapeProperties shape) {
      if (!this.closed) {
         if (this.count >= 30) {
            this.flush();
         }

         float scale = (float)mc.getWindow().getScaleFactor();
         float alpha = RenderSystem.getShaderColor()[3];
         Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
         Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0.0F, SCRATCH_POS).mul(scale);
         Vector3f s = matrix4f.getScale(SCRATCH_SIZE).mul(scale);
         Vector4f round = shape.getRound().mul(s.y);
         float softness = shape.getSoftness();
         float thickness = shape.getThickness();
         float widthPx = shape.getWidth() * s.x;
         float heightPx = shape.getHeight() * s.y;
         float quadPad = Math.max(1.0F, softness + 1.0F);
         float qx = shape.getX() - quadPad / 2.0F;
         float qy = shape.getY() - quadPad / 2.0F;
         float qw = shape.getWidth() + quadPad;
         float qh = shape.getHeight() + quadPad;
         int qb = this.count * 8;
         Vector3f v = SCRATCH_VERT;
         matrix4f.transformPosition(qx, qy, 0.0F, v);
         this.quadCoords[qb + 0] = v.x;
         this.quadCoords[qb + 1] = v.y;
         matrix4f.transformPosition(qx, qy + qh, 0.0F, v);
         this.quadCoords[qb + 2] = v.x;
         this.quadCoords[qb + 3] = v.y;
         matrix4f.transformPosition(qx + qw, qy + qh, 0.0F, v);
         this.quadCoords[qb + 4] = v.x;
         this.quadCoords[qb + 5] = v.y;
         matrix4f.transformPosition(qx + qw, qy, 0.0F, v);
         this.quadCoords[qb + 6] = v.x;
         this.quadCoords[qb + 7] = v.y;
         int b = this.count * 33;
         this.shapeData[b + 0] = pos.x;
         this.shapeData[b + 1] = window.getHeight() - heightPx - pos.y;
         this.shapeData[b + 2] = widthPx;
         this.shapeData[b + 3] = heightPx;
         this.shapeData[b + 4] = round.x;
         this.shapeData[b + 5] = round.y;
         this.shapeData[b + 6] = round.z;
         this.shapeData[b + 7] = round.w;
         this.shapeData[b + 8] = thickness;
         packColor(this.shapeData, b + 9, shape.getColor().x, alpha);
         packColor(this.shapeData, b + 13, shape.getColor().y, alpha);
         packColor(this.shapeData, b + 17, shape.getColor().z, alpha);
         packColor(this.shapeData, b + 21, shape.getColor().w, alpha);
         packColor(this.shapeData, b + 25, shape.getOutlineColor(), alpha);
         Vector4f sides = shape.getOutlineSides();
         this.shapeData[b + 29] = sides.x;
         this.shapeData[b + 30] = sides.y;
         this.shapeData[b + 31] = sides.z;
         this.shapeData[b + 32] = sides.w;
         this.count++;
      }
   }

   private static void packColor(float[] dst, int offset, int rgba, float alpha) {
      dst[offset + 0] = ColorUtil.redf(rgba);
      dst[offset + 1] = ColorUtil.greenf(rgba);
      dst[offset + 2] = ColorUtil.bluef(rgba);
      dst[offset + 3] = ColorUtil.alphaf(ColorUtil.multAlpha(rgba, alpha));
   }

   public void flush() {
      if (this.count != 0) {
         HudProfiler.begin(HudProfiler.Section.RECT_BATCH_FLUSH);
         HudProfiler.countRectFlush(this.count);

         try {
            int usedFloats = this.count * 33;
            int totalFloats = 990;
            if (usedFloats < totalFloats) {
               Arrays.fill(this.shapeData, usedFloats, totalFloats, 0.0F);
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.enableCull();
            BufferBuilder builder = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION);

            for (int i = 0; i < this.count; i++) {
               int qb = i * 8;
               builder.vertex(IDENTITY, this.quadCoords[qb + 0], this.quadCoords[qb + 1], 0.0F);
               builder.vertex(IDENTITY, this.quadCoords[qb + 2], this.quadCoords[qb + 3], 0.0F);
               builder.vertex(IDENTITY, this.quadCoords[qb + 4], this.quadCoords[qb + 5], 0.0F);
               builder.vertex(IDENTITY, this.quadCoords[qb + 6], this.quadCoords[qb + 7], 0.0F);
            }

            ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
            if (shader != null) {
               shader.getUniformOrDefault("shapeData").set(this.shapeData);
               BufferRenderer.drawWithGlobalProgram(builder.end());
               this.count = 0;
               return;
            }

            builder.endNullable();
            this.count = 0;
         } finally {
            HudProfiler.end(HudProfiler.Section.RECT_BATCH_FLUSH);
         }
      }
   }

   @Override
   public void close() {
      this.end();
   }

   public void end() {
      if (!this.closed) {
         this.closed = true;
         if (ACTIVE == this) {
            ACTIVE = null;
         }

         if (this.count > 0) {
            this.flush();
         }

         RenderSystem.disableBlend();
      }
   }
}
