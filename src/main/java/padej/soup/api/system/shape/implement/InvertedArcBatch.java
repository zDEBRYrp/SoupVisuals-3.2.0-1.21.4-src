package padej.soup.api.system.shape.implement;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
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

public final class InvertedArcBatch implements QuickImports, AutoCloseable {
   public static final int MAX_BATCH = 32;
   private static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
      Identifier.of("minecraft", "core/arc_inverted_batch"), VertexFormats.POSITION, Defines.EMPTY
   );
   private static final Vector3f SCRATCH_POS = new Vector3f();
   private static final Vector3f SCRATCH_SIZE = new Vector3f();
   private static final Vector3f SCRATCH_VERT = new Vector3f();
   private static final Matrix4f IDENTITY = new Matrix4f();
   private static InvertedArcBatch ACTIVE = null;
   private static final InvertedArcBatch INSTANCE = new InvertedArcBatch();
   private final float[] locations = new float[64];
   private final float[] sizes = new float[64];
   private final float[] radii = new float[32];
   private final float[] thicknesses = new float[32];
   private final float[] starts = new float[32];
   private final float[] ends = new float[32];
   private final float[] quadCoords = new float[256];
   private int count = 0;
   private boolean closed = false;

   public static InvertedArcBatch active() {
      return ACTIVE;
   }

   public static InvertedArcBatch begin() {
      if (ACTIVE != null) {
         ACTIVE.end();
      }

      INSTANCE.count = 0;
      INSTANCE.closed = false;
      ACTIVE = INSTANCE;
      return INSTANCE;
   }

   private InvertedArcBatch() {
   }

   public void submit(ShapeProperties shape) {
      if (!this.closed) {
         if (this.count >= 32) {
            this.flush();
         }

         float scale = (float)mc.getWindow().getScaleFactor();
         Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
         Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0.0F, SCRATCH_POS).mul(scale);
         Vector3f s = matrix4f.getScale(SCRATCH_SIZE).mul(scale);
         Vector4f round = shape.getRound().mul(s.y);
         float widthPx = shape.getWidth() * s.x;
         float heightPx = shape.getHeight() * s.y;
         float qx = shape.getX();
         float qy = shape.getY();
         float qw = shape.getWidth();
         float qh = shape.getHeight();
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
         int i2 = this.count * 2;
         this.locations[i2 + 0] = pos.x;
         this.locations[i2 + 1] = window.getHeight() - heightPx - pos.y;
         this.sizes[i2 + 0] = widthPx;
         this.sizes[i2 + 1] = heightPx;
         this.radii[this.count] = round.x;
         this.thicknesses[this.count] = shape.getThickness();
         this.starts[this.count] = shape.getStart();
         this.ends[this.count] = shape.getEnd();
         this.count++;
      }
   }

   public void flush() {
      if (this.count != 0) {
         for (int i = this.count; i < 32; i++) {
            this.locations[i * 2] = 0.0F;
            this.locations[i * 2 + 1] = 0.0F;
            this.sizes[i * 2] = 0.0F;
            this.sizes[i * 2 + 1] = 0.0F;
            this.radii[i] = 0.0F;
            this.thicknesses[i] = 0.0F;
            this.starts[i] = 0.0F;
            this.ends[i] = 0.0F;
         }

         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SrcFactor.ONE_MINUS_DST_COLOR, DstFactor.ONE_MINUS_SRC_COLOR, SrcFactor.ONE, DstFactor.ZERO);
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
         if (shader == null) {
            builder.endNullable();
            this.count = 0;
         } else {
            shader.getUniformOrDefault("locations").set(this.locations);
            shader.getUniformOrDefault("sizes").set(this.sizes);
            shader.getUniformOrDefault("radii").set(this.radii);
            shader.getUniformOrDefault("thicknesses").set(this.thicknesses);
            shader.getUniformOrDefault("starts").set(this.starts);
            shader.getUniformOrDefault("ends").set(this.ends);
            BufferRenderer.drawWithGlobalProgram(builder.end());
            this.count = 0;
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
         RenderSystem.defaultBlendFunc();
      }
   }
}
