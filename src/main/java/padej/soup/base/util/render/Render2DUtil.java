package padej.soup.base.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40C;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;

public final class Render2DUtil implements QuickImports {
   private static final List<Render2DUtil.Quad> QUAD = new ArrayList<>();

   public static void onRender(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      if (!QUAD.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         QUAD.forEach(quad -> drawEngine.quad(matrix4f, buffer, quad.x, quad.y, quad.width, quad.height, quad.color));
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.disableBlend();
         QUAD.clear();
      }
   }

   public static void defaultDrawStack(
      @NonNull DrawContext context, @NonNull ItemStack stack, float x, float y, boolean rect, boolean drawItemInSlot, float scale
   ) {
      if (context == null) {
         throw new NullPointerException("context is marked non-null but is null");
      } else if (stack == null) {
         throw new NullPointerException("stack is marked non-null but is null");
      } else {
         MatrixStack matrix = context.getMatrices();
         if (rect) {
            blur.render(ShapeProperties.create(matrix, x, y, 16.0F * scale + 2.0F, 16.0F * scale + 2.0F).round(2.0F).color(ColorUtil.HALF_BLACK).build());
         }

         matrix.push();
         matrix.translate(x + 1.0F, y + 1.0F, 0.0F);
         matrix.scale(scale, scale, 1.0F);
         context.drawItem(stack, 0, 0);
         if (drawItemInSlot) {
            context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
         }

         matrix.pop();
      }
   }

   public static void drawHead(@NonNull DrawContext context, Identifier id, float x, float y, float size, float round, int backgroundColor, int color) {
      if (context == null) {
         throw new NullPointerException("context is marked non-null but is null");
      } else {
         MatrixStack matrix = context.getMatrices();
         rectangle.render(ShapeProperties.create(matrix, x, y, size, size).round(round).color(backgroundColor).build());
         if (id != null) {
            matrix.push();
            matrix.translate(x, y, 0.0F);
            matrix.scale(size, size, 1.0F);
            RenderSystem.setShaderTexture(0, id);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(772, 773);
            GL40C.glTexParameteri(3553, 10241, 9728);
            GL40C.glTexParameteri(3553, 10240, 9728);
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Matrix4f matrix4f = matrix.peek().getPositionMatrix();
            float u1_base = 0.125F;
            float u2_base = 0.25F;
            float v1_base = 0.125F;
            float v2_base = 0.25F;
            float u1_overlay = 0.625F;
            float u2_overlay = 0.75F;
            float v1_overlay = 0.125F;
            float v2_overlay = 0.25F;
            buffer.vertex(matrix4f, 0.0F, 0.0F, 0.0F).texture(u1_base, v1_base).color(color);
            buffer.vertex(matrix4f, 0.0F, 1.0F, 0.0F).texture(u1_base, v2_base).color(color);
            buffer.vertex(matrix4f, 1.0F, 1.0F, 0.0F).texture(u2_base, v2_base).color(color);
            buffer.vertex(matrix4f, 1.0F, 0.0F, 0.0F).texture(u2_base, v1_base).color(color);
            buffer.vertex(matrix4f, 0.0F, 0.0F, 0.0F).texture(u1_overlay, v1_overlay).color(color);
            buffer.vertex(matrix4f, 0.0F, 1.0F, 0.0F).texture(u1_overlay, v2_overlay).color(color);
            buffer.vertex(matrix4f, 1.0F, 1.0F, 0.0F).texture(u2_overlay, v2_overlay).color(color);
            buffer.vertex(matrix4f, 1.0F, 0.0F, 0.0F).texture(u2_overlay, v1_overlay).color(color);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.defaultBlendFunc();
            matrix.translate(-x, -y, 0.0F);
            matrix.pop();
         }
      }
   }

   public static void drawDSAvatar(@NonNull DrawContext context, Identifier id, float x, float y, float size, float round, int backgroundColor, int color) {
      if (context == null) {
         throw new NullPointerException("context is marked non-null but is null");
      } else {
         MatrixStack matrix = context.getMatrices();
         rectangle.render(ShapeProperties.create(matrix, x, y, size, size).round(round).color(backgroundColor).build());
         if (id != null) {
            matrix.push();
            matrix.translate(x, y, 0.0F);
            matrix.scale(size, size, 1.0F);
            RenderSystem.setShaderTexture(0, id);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(772, 773);
            GL40C.glTexParameteri(3553, 10241, 9728);
            GL40C.glTexParameteri(3553, 10240, 9728);
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Matrix4f matrix4f = matrix.peek().getPositionMatrix();
            float u1_base = 0.125F;
            float u2_base = 0.25F;
            float v1_base = 0.125F;
            float v2_base = 0.25F;
            float u1_overlay = 0.625F;
            float u2_overlay = 0.75F;
            float v1_overlay = 0.125F;
            float v2_overlay = 0.25F;
            buffer.vertex(matrix4f, 0.0F, 0.0F, 0.0F).texture(u1_base, v1_base).color(color);
            buffer.vertex(matrix4f, 0.0F, 1.0F, 0.0F).texture(u1_base, v2_base).color(color);
            buffer.vertex(matrix4f, 1.0F, 1.0F, 0.0F).texture(u2_base, v2_base).color(color);
            buffer.vertex(matrix4f, 1.0F, 0.0F, 0.0F).texture(u2_base, v1_base).color(color);
            buffer.vertex(matrix4f, 0.0F, 0.0F, 0.0F).texture(u1_overlay, v1_overlay).color(color);
            buffer.vertex(matrix4f, 0.0F, 1.0F, 0.0F).texture(u1_overlay, v2_overlay).color(color);
            buffer.vertex(matrix4f, 1.0F, 1.0F, 0.0F).texture(u2_overlay, v2_overlay).color(color);
            buffer.vertex(matrix4f, 1.0F, 0.0F, 0.0F).texture(u2_overlay, v1_overlay).color(color);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            GL20.glTexParameteri(3553, 10241, 9986);
            GL20.glTexParameteri(3553, 10240, 9728);
            RenderSystem.defaultBlendFunc();
            matrix.translate(-x, -y, 0.0F);
            matrix.pop();
         }
      }
   }

   public static void drawSprite(@NonNull MatrixStack matrix, @NonNull Sprite sprite, float x, float y, float width, int height) {
      if (matrix == null) {
         throw new NullPointerException("matrix is marked non-null but is null");
      } else if (sprite == null) {
         throw new NullPointerException("sprite is marked non-null but is null");
      } else {
         drawSprite(matrix, sprite, x, y, width, height, -1);
      }
   }

   public static void drawSprite(@NonNull MatrixStack matrix, @NonNull Sprite sprite, float x, float y, float width, int height, int color) {
      if (matrix == null) {
         throw new NullPointerException("matrix is marked non-null but is null");
      } else if (sprite == null) {
         throw new NullPointerException("sprite is marked non-null but is null");
      } else {
         if (width != 0.0F && height != 0) {
            drawTexturedQuad(
               matrix, sprite.getAtlasId(), x, x + width, y, y + height, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(), color
            );
         }
      }
   }

   public static void drawTexture(
      @NonNull DrawContext context, Identifier id, float x, float y, float size, float round, int uvSize, int regionSize, int textureSize, int backgroundColor
   ) {
      if (context == null) {
         throw new NullPointerException("context is marked non-null but is null");
      } else {
         drawTexture(context, id, x, y, size, round, uvSize, regionSize, textureSize, backgroundColor, -1);
      }
   }

   public static void drawTexture(
      @NonNull DrawContext context,
      Identifier id,
      float x,
      float y,
      float size,
      float round,
      int uvSize,
      int regionSize,
      int textureSize,
      int backgroundColor,
      int color
   ) {
      if (context == null) {
         throw new NullPointerException("context is marked non-null but is null");
      } else {
         MatrixStack matrix = context.getMatrices();
         rectangle.render(ShapeProperties.create(matrix, x, y, size, size).round(round).color(backgroundColor).build());
         if (id != null) {
            matrix.push();
            matrix.translate(x, y, 0.0F);
            matrix.scale(size, size, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(772, 773);
            drawTexture(matrix, id, 0, 0, 1.0F, 1.0F, uvSize, uvSize, regionSize, regionSize, textureSize, textureSize, color);
            RenderSystem.disableBlend();
            matrix.translate(-x, -y, 0.0F);
            matrix.pop();
         }
      }
   }

   public static void drawTexture(
      @NonNull MatrixStack matrix,
      @NonNull Identifier texture,
      int x,
      int y,
      float width,
      float height,
      float u,
      float v,
      int regionWidth,
      int regionHeight,
      int textureWidth,
      int textureHeight,
      int color
   ) {
      if (matrix == null) {
         throw new NullPointerException("matrix is marked non-null but is null");
      } else if (texture == null) {
         throw new NullPointerException("texture is marked non-null but is null");
      } else {
         drawTexture(matrix, texture, x, x + width, y, y + height, 0.0F, regionWidth, regionHeight, u, v, textureWidth, textureHeight, color);
      }
   }

   public static void drawTexture(
      @NonNull MatrixStack matrix,
      @NonNull Identifier texture,
      float x1,
      float x2,
      float y1,
      float y2,
      float z,
      int regionWidth,
      int regionHeight,
      float u,
      float v,
      int textureWidth,
      int textureHeight,
      int color
   ) {
      if (matrix == null) {
         throw new NullPointerException("matrix is marked non-null but is null");
      } else if (texture == null) {
         throw new NullPointerException("texture is marked non-null but is null");
      } else {
         drawTexturedQuad(
            matrix,
            texture,
            x1,
            x2,
            y1,
            y2,
            (u + 0.0F) / textureWidth,
            (u + regionWidth) / textureWidth,
            (v + 0.0F) / textureHeight,
            (v + regionHeight) / textureHeight,
            color
         );
      }
   }

   public static void drawTexturedQuad(
      @NonNull MatrixStack matrix, @NonNull Identifier texture, float x1, float x2, float y1, float y2, float u1, float u2, float v1, float v2, int color
   ) {
      if (matrix == null) {
         throw new NullPointerException("matrix is marked non-null but is null");
      } else if (texture == null) {
         throw new NullPointerException("texture is marked non-null but is null");
      } else {
         RenderSystem.setShaderTexture(0, texture);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         Matrix4f matrix4f = matrix.peek().getPositionMatrix();
         buffer.vertex(matrix4f, x1, y1, 0.0F).texture(u1, v1).color(color);
         buffer.vertex(matrix4f, x1, y2, 0.0F).texture(u1, v2).color(color);
         buffer.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
         buffer.vertex(matrix4f, x2, y1, 0.0F).texture(u2, v1).color(color);
         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
   }

   public static void drawQuad(float x, float y, float width, float height, int color) {
      QUAD.add(new Render2DUtil.Quad(x, y, width, height, ColorUtil.multAlpha(color, RenderSystem.getShaderColor()[3])));
   }

   private Render2DUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public record Quad(float x, float y, float width, float height, int color) {
   }
}
