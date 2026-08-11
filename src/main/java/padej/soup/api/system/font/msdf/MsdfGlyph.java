package padej.soup.api.system.font.msdf;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

public final class MsdfGlyph {
   private final int code;
   private final float minU;
   private final float maxU;
   private final float minV;
   private final float maxV;
   private final float advance;
   private final float topPosition;
   private final float bottomPosition;
   private final float leftBearing;
   private final float width;
   private final float height;

   public MsdfGlyph(FontData.GlyphData data, float atlasWidth, float atlasHeight) {
      this.code = data.unicode();
      this.advance = data.advance();
      FontData.BoundsData atlasBounds = data.atlasBounds();
      if (atlasBounds != null) {
         this.minU = atlasBounds.left() / atlasWidth;
         this.maxU = atlasBounds.right() / atlasWidth;
         this.minV = 1.0F - atlasBounds.top() / atlasHeight;
         this.maxV = 1.0F - atlasBounds.bottom() / atlasHeight;
      } else {
         this.minU = this.maxU = this.minV = this.maxV = 0.0F;
      }

      FontData.BoundsData planeBounds = data.planeBounds();
      if (planeBounds != null) {
         this.width = planeBounds.right() - planeBounds.left();
         this.height = planeBounds.top() - planeBounds.bottom();
         this.topPosition = planeBounds.top();
         this.bottomPosition = planeBounds.bottom();
         this.leftBearing = planeBounds.left();
      } else {
         this.width = this.height = this.topPosition = this.bottomPosition = this.leftBearing = 0.0F;
      }
   }

   public float apply(Matrix4f matrix, VertexConsumer consumer, float size, float x, float y, float z, int color) {
      x += this.leftBearing * size;
      y -= this.topPosition * size;
      y--;
      float w = this.width * size;
      float h = this.height * size;
      consumer.vertex(matrix, x, y, z).texture(this.minU, this.minV).color(color);
      consumer.vertex(matrix, x, y + h, z).texture(this.minU, this.maxV).color(color);
      consumer.vertex(matrix, x + w, y + h, z).texture(this.maxU, this.maxV).color(color);
      consumer.vertex(matrix, x + w, y, z).texture(this.maxU, this.minV).color(color);
      return this.advance * size;
   }

   public float emit(Matrix4f matrix, VertexConsumer consumer, float size, float x, float baseline, float z, int color) {
      float qx = x + this.leftBearing * size;
      float qy = baseline - this.topPosition * size;
      float w = this.width * size;
      float h = this.height * size;
      consumer.vertex(matrix, qx, qy, z).texture(this.minU, this.minV).color(color);
      consumer.vertex(matrix, qx, qy + h, z).texture(this.minU, this.maxV).color(color);
      consumer.vertex(matrix, qx + w, qy + h, z).texture(this.maxU, this.maxV).color(color);
      consumer.vertex(matrix, qx + w, qy, z).texture(this.maxU, this.minV).color(color);
      return this.advance * size;
   }

   public float getAdvance() {
      return this.advance;
   }

   public float getWidth(float size) {
      return this.advance * size;
   }

   public float getVisualHeight(float size) {
      return this.height * size;
   }

   public float getTopPosition() {
      return this.topPosition;
   }

   public float getBottomPosition() {
      return this.bottomPosition;
   }

   public float getLeftBearing() {
      return this.leftBearing;
   }

   public int getCharCode() {
      return this.code;
   }

   public float getMinU() {
      return this.minU;
   }

   public float getMaxU() {
      return this.maxU;
   }

   public float getMinV() {
      return this.minV;
   }

   public float getMaxV() {
      return this.maxV;
   }

   public float getPlaneWidth() {
      return this.width;
   }

   public float getPlaneHeight() {
      return this.height;
   }

   public void emitRot90CCW(Matrix4f matrix, VertexConsumer consumer, float size, float centerX, float centerY, float z, int color) {
      float screenW = this.height * size;
      float screenH = this.width * size;
      float qx = centerX - screenW * 0.5F;
      float qy = centerY - screenH * 0.5F;
      consumer.vertex(matrix, qx, qy, z).texture(this.maxU, this.minV).color(color);
      consumer.vertex(matrix, qx, qy + screenH, z).texture(this.minU, this.minV).color(color);
      consumer.vertex(matrix, qx + screenW, qy + screenH, z).texture(this.minU, this.maxV).color(color);
      consumer.vertex(matrix, qx + screenW, qy, z).texture(this.maxU, this.maxV).color(color);
   }
}
