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
import padej.soup.api.system.shape.Shape;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;

public class InvertedRectangle implements Shape, QuickImports {
   private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/round_inverted"), VertexFormats.POSITION, Defines.EMPTY);
   private static final Vector3f SCRATCH_POS = new Vector3f();
   private static final Vector3f SCRATCH_SIZE = new Vector3f();

   @Override
   public void render(ShapeProperties shape) {
      InvertedRectangleBatch batch = InvertedRectangleBatch.active();
      if (batch != null) {
         batch.submit(shape);
      } else {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SrcFactor.ONE_MINUS_DST_COLOR, DstFactor.ONE_MINUS_SRC_COLOR, SrcFactor.ONE, DstFactor.ZERO);
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         float scale = (float)mc.getWindow().getScaleFactor();
         Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
         Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0.0F, SCRATCH_POS).mul(scale);
         Vector3f size = matrix4f.getScale(SCRATCH_SIZE).mul(scale);
         Vector4f round = shape.getRound().mul(size.y);
         float softness = shape.getSoftness();
         float width = shape.getWidth() * size.x;
         float height = shape.getHeight() * size.y;
         float quadPad = Math.max(1.0F, softness + 1.0F);
         BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION);
         drawEngine.quad(
            matrix4f, buffer, shape.getX() - quadPad / 2.0F, shape.getY() - quadPad / 2.0F, shape.getWidth() + quadPad, shape.getHeight() + quadPad
         );
         ShaderProgram shader = RenderSystem.setShader(this.SHADER_KEY);
         if (shader != null) {
            shader.getUniformOrDefault("size").set(width, height);
            shader.getUniformOrDefault("location").set(pos.x, window.getHeight() - height - pos.y);
            shader.getUniformOrDefault("radius").set(round);
            shader.getUniformOrDefault("softness").set(softness);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
         }
      }
   }
}
