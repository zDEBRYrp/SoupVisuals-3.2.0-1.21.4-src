package padej.soup.api.system.shape;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import org.joml.Vector4i;

public class ShapeProperties {
   private static final Vector4f DEFAULT_OUTLINE_SIDES = new Vector4f(1.0F);
   private static final int VEC_POOL_SIZE = 16;
   private static final Vector4f[] ROUND_POOL = new Vector4f[16];
   private static final Vector4i[] COLOR_POOL = new Vector4i[16];
   private static int roundPoolIdx = 0;
   private static int colorPoolIdx = 0;
   private MatrixStack matrix;
   private float x;
   private float y;
   private float width;
   private float height;
   private float softness;
   private float thickness;
   private float start;
   private float end;
   private float quality;
   private float rotation;
   private Vector4f round;
   private int outlineColor;
   private Vector4i color;
   private Vector4f outlineSides;

   private static Vector4f nextRoundVec() {
      Vector4f v = ROUND_POOL[roundPoolIdx];
      roundPoolIdx = roundPoolIdx + 1 & 15;
      return v;
   }

   private static Vector4i nextColorVec() {
      Vector4i v = COLOR_POOL[colorPoolIdx];
      colorPoolIdx = colorPoolIdx + 1 & 15;
      return v;
   }

   private ShapeProperties(
      MatrixStack matrix,
      float x,
      float y,
      float width,
      float height,
      float softness,
      float thickness,
      float start,
      float end,
      float quality,
      float rotation,
      Vector4f round,
      int outlineColor,
      Vector4i color,
      Vector4f outlineSides
   ) {
      this.matrix = matrix;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.softness = softness;
      this.thickness = thickness;
      this.round = round != null ? round : new Vector4f(0.0F);
      this.outlineColor = outlineColor;
      this.color = color != null ? color : new Vector4i(-1);
      this.outlineSides = outlineSides != null ? outlineSides : DEFAULT_OUTLINE_SIDES;
      this.start = start;
      this.end = end;
      this.quality = quality;
      this.rotation = rotation;
   }

   public static ShapeProperties.ShapePropertiesBuilder create(MatrixStack matrix, double x, double y, double width, double height) {
      return builder().matrix(matrix).x((float)x).y((float)y).width((float)width).height((float)height);
   }

   private static float $default$quality() {
      return 20.0F;
   }

   private static float $default$rotation() {
      return 90.0F;
   }

   private static int $default$outlineColor() {
      return -1;
   }

   public static ShapeProperties.ShapePropertiesBuilder builder() {
      return new ShapeProperties.ShapePropertiesBuilder();
   }

   public ShapeProperties.ShapePropertiesBuilder toBuilder() {
      return new ShapeProperties.ShapePropertiesBuilder()
         .matrix(this.matrix)
         .x(this.x)
         .y(this.y)
         .width(this.width)
         .height(this.height)
         .softness(this.softness)
         .thickness(this.thickness)
         .start(this.start)
         .end(this.end)
         .quality(this.quality)
         .rotation(this.rotation)
         .round(this.round)
         .outlineColor(this.outlineColor)
         .color(this.color)
         .outlineSides(this.outlineSides);
   }

   public MatrixStack getMatrix() {
      return this.matrix;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHeight() {
      return this.height;
   }

   public float getSoftness() {
      return this.softness;
   }

   public float getThickness() {
      return this.thickness;
   }

   public float getStart() {
      return this.start;
   }

   public float getEnd() {
      return this.end;
   }

   public float getQuality() {
      return this.quality;
   }

   public float getRotation() {
      return this.rotation;
   }

   public Vector4f getRound() {
      return this.round;
   }

   public int getOutlineColor() {
      return this.outlineColor;
   }

   public Vector4i getColor() {
      return this.color;
   }

   public Vector4f getOutlineSides() {
      return this.outlineSides;
   }

   public void setMatrix(MatrixStack matrix) {
      this.matrix = matrix;
   }

   public void setX(float x) {
      this.x = x;
   }

   public void setY(float y) {
      this.y = y;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public void setSoftness(float softness) {
      this.softness = softness;
   }

   public void setThickness(float thickness) {
      this.thickness = thickness;
   }

   public void setStart(float start) {
      this.start = start;
   }

   public void setEnd(float end) {
      this.end = end;
   }

   public void setQuality(float quality) {
      this.quality = quality;
   }

   public void setRotation(float rotation) {
      this.rotation = rotation;
   }

   public void setRound(Vector4f round) {
      this.round = round;
   }

   public void setOutlineColor(int outlineColor) {
      this.outlineColor = outlineColor;
   }

   public void setColor(Vector4i color) {
      this.color = color;
   }

   public void setOutlineSides(Vector4f outlineSides) {
      this.outlineSides = outlineSides;
   }

   static {
      for (int i = 0; i < 16; i++) {
         ROUND_POOL[i] = new Vector4f();
         COLOR_POOL[i] = new Vector4i();
      }
   }

   public static class ShapePropertiesBuilder {
      private MatrixStack matrix;
      private float x;
      private float y;
      private float width;
      private float height;
      private float softness;
      private float thickness;
      private float start;
      private float end;
      private boolean quality$set;
      private float quality$value;
      private boolean rotation$set;
      private float rotation$value;
      private Vector4f round;
      private boolean outlineColor$set;
      private int outlineColor$value;
      private Vector4i color;
      private Vector4f outlineSides;
      private float quality;
      private float rotation;
      private int outlineColor;

      public ShapeProperties.ShapePropertiesBuilder color(int color) {
         Vector4i v = ShapeProperties.nextColorVec();
         v.set(color, color, color, color);
         this.color = v;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder color(Vector4i color) {
         this.color = color;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder color(int... color) {
         Vector4i v = ShapeProperties.nextColorVec();
         if (color.length >= 4) {
            v.set(color[0], color[1], color[2], color[3]);
         } else if (color.length > 0) {
            int c = color[0];
            v.set(c, c, c, c);
         } else {
            v.set(-1, -1, -1, -1);
         }

         this.color = v;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder round(float round) {
         Vector4f v = ShapeProperties.nextRoundVec();
         v.set(round, round, round, round);
         this.round = v;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder round(Vector4f round) {
         this.round = round;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder round(float... round) {
         Vector4f v = ShapeProperties.nextRoundVec();
         if (round.length >= 4) {
            v.set(round[0], round[1], round[2], round[3]);
         } else if (round.length > 0) {
            float r = round[0];
            v.set(r, r, r, r);
         } else {
            v.set(0.0F, 0.0F, 0.0F, 0.0F);
         }

         this.round = v;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder outlineSides(float top, float right, float bottom, float left) {
         this.outlineSides = new Vector4f(top, right, bottom, left);
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder outlineSides(Vector4f outlineSides) {
         this.outlineSides = outlineSides;
         return this;
      }

      ShapePropertiesBuilder() {
      }

      public ShapeProperties.ShapePropertiesBuilder matrix(MatrixStack matrix) {
         this.matrix = matrix;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder x(float x) {
         this.x = x;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder y(float y) {
         this.y = y;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder width(float width) {
         this.width = width;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder height(float height) {
         this.height = height;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder softness(float softness) {
         this.softness = softness;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder thickness(float thickness) {
         this.thickness = thickness;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder start(float start) {
         this.start = start;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder end(float end) {
         this.end = end;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder quality(float quality) {
         this.quality$value = quality;
         this.quality$set = true;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder rotation(float rotation) {
         this.rotation$value = rotation;
         this.rotation$set = true;
         return this;
      }

      public ShapeProperties.ShapePropertiesBuilder outlineColor(int outlineColor) {
         this.outlineColor$value = outlineColor;
         this.outlineColor$set = true;
         return this;
      }

      public ShapeProperties build() {
         float quality$value = this.quality$value;
         if (!this.quality$set) {
            quality$value = ShapeProperties.$default$quality();
         }

         float rotation$value = this.rotation$value;
         if (!this.rotation$set) {
            rotation$value = ShapeProperties.$default$rotation();
         }

         int outlineColor$value = this.outlineColor$value;
         if (!this.outlineColor$set) {
            outlineColor$value = ShapeProperties.$default$outlineColor();
         }

         return new ShapeProperties(
            this.matrix,
            this.x,
            this.y,
            this.width,
            this.height,
            this.softness,
            this.thickness,
            this.start,
            this.end,
            quality$value,
            rotation$value,
            this.round,
            outlineColor$value,
            this.color,
            this.outlineSides
         );
      }

      @Override
      public String toString() {
         return "ShapeProperties.ShapePropertiesBuilder(matrix="
            + this.matrix
            + ", x="
            + this.x
            + ", y="
            + this.y
            + ", width="
            + this.width
            + ", height="
            + this.height
            + ", softness="
            + this.softness
            + ", thickness="
            + this.thickness
            + ", start="
            + this.start
            + ", end="
            + this.end
            + ", quality$value="
            + this.quality$value
            + ", rotation$value="
            + this.rotation$value
            + ", round="
            + this.round
            + ", outlineColor$value="
            + this.outlineColor$value
            + ", color="
            + this.color
            + ", outlineSides="
            + this.outlineSides
            + ")";
      }
   }
}
