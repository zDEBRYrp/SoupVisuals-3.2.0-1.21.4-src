package padej.soup.api.system.shape.implement;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL20;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.Shape;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;

public class Image implements Shape, QuickImports {
   private String texture;
   private Identifier cachedTextureId;
   private int iconCodepoint = -1;

   public Image setIcon(int codepoint) {
      this.iconCodepoint = codepoint;
      this.texture = null;
      this.cachedTextureId = null;
      return this;
   }

   public Image setTexture(String texture) {
      if (texture != this.texture && (texture == null || !texture.equals(this.texture))) {
         this.cachedTextureId = null;
      }

      this.texture = texture;
      this.iconCodepoint = -1;
      return this;
   }

   @Override
   public void render(ShapeProperties shape) {
      if (this.iconCodepoint >= 0) {
         this.renderIcon(shape);
      } else {
         this.renderTexture(shape);
      }
   }

   private void renderIcon(ShapeProperties shape) {
      MatrixStack matrix = shape.getMatrix();
      float size = Math.min(shape.getWidth(), shape.getHeight());
      FontRenderer iconFont = Fonts.getSize((int)size, Fonts.Type.ICONS);
      if (iconFont.getPixelSize() != size) {
         iconFont = new FontRenderer(iconFont.getMsdfFont(), size);
      }

      int color = shape.getColor().x;
      float rotation = shape.getRotation();
      if (rotation != 0.0F) {
         float cx = shape.getX() + size * 0.5F;
         float cy = shape.getY() + size * 0.5F;
         matrix.push();
         matrix.translate(cx, cy, 0.0F);
         matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
         matrix.translate(-cx, -cy, 0.0F);
         iconFont.drawIcon(matrix, this.iconCodepoint, shape.getX(), shape.getY(), size, color);
         matrix.pop();
      } else {
         iconFont.drawIcon(matrix, this.iconCodepoint, shape.getX(), shape.getY(), size, color);
      }
   }

   private void renderTexture(ShapeProperties shape) {
      MatrixStack matrix = shape.getMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      Identifier id = this.cachedTextureId;
      if (id == null) {
         id = Identifier.of(this.texture);
         this.cachedTextureId = id;
      }

      RenderSystem.setShaderTexture(0, id);
      AbstractTexture tex = mc.getTextureManager().getTexture(id);
      if (tex != null) {
         int glId = tex.getGlId();
         GlStateManager._activeTexture(33984);
         GlStateManager._bindTexture(glId);
         GL20.glTexParameteri(3553, 10241, 9729);
         GL20.glTexParameteri(3553, 10240, 9729);
      }

      float width = shape.getWidth();
      float x = shape.getX() + width;
      float y = shape.getY();
      matrix.push();
      matrix.translate(x, y, 0.0F);
      matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(shape.getRotation()));
      matrix.translate(-x, -y, 0.0F);
      drawEngine.quad(matrix.peek().getPositionMatrix(), x, y, shape.getHeight(), width, shape.getColor().x);
      matrix.pop();
      RenderSystem.disableBlend();
   }
}
