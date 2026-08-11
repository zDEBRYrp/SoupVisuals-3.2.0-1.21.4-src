package padej.soup.api.system.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.joml.Matrix4f;
import padej.soup.base.QuickImports;

public class DrawEngineImpl implements DrawEngine, QuickImports {
   @Override
   public void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height) {
      buffer.vertex(matrix4f, x, y, 0.0F);
      buffer.vertex(matrix4f, x, y + height, 0.0F);
      buffer.vertex(matrix4f, x + width, y + height, 0.0F);
      buffer.vertex(matrix4f, x + width, y, 0.0F);
   }

   @Override
   public void quad(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height, int color) {
      buffer.vertex(matrix4f, x, y, 0.0F).color(color);
      buffer.vertex(matrix4f, x, y + height, 0.0F).color(color);
      buffer.vertex(matrix4f, x + width, y + height, 0.0F).color(color);
      buffer.vertex(matrix4f, x + width, y, 0.0F).color(color);
   }

   @Override
   public void quad(Matrix4f matrix4f, float x, float y, float width, float height, int color) {
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      buffer.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 0.0F).color(color);
      buffer.vertex(matrix4f, x + width, y + height, 0.0F).texture(0.0F, 1.0F).color(color);
      buffer.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 1.0F).color(color);
      buffer.vertex(matrix4f, x, y, 0.0F).texture(1.0F, 0.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }
}
