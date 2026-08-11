package padej.soup.base.util.particle;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.base.util.spatial.BlockCollisionGrid;

public abstract class AbstractParticle {
   protected float x;
   protected float y;
   protected float z;
   protected float px;
   protected float py;
   protected float pz;
   protected float motionX;
   protected float motionY;
   protected float motionZ;
   protected float rotationAngle;
   protected float prevRotationAngle;
   protected float rotationSpeed;
   protected long time;
   protected float lifeTime;
   protected int age;
   protected int maxAge;
   protected Color color;
   protected float scale;
   protected float cachedAlpha = 1.0F;
   protected boolean isVisible = true;
   protected int ticksSinceLastUpdate = 0;
   protected int colorOffset;
   protected String physicsMode;
   protected boolean enableCollision;

   public AbstractParticle(
      float x,
      float y,
      float z,
      Color color,
      float rotationAngle,
      float rotationSpeed,
      float lifeTime,
      float scale,
      String physicsMode,
      boolean enableCollision
   ) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.px = x;
      this.py = y;
      this.pz = z;
      this.color = color;
      this.rotationAngle = rotationAngle;
      this.rotationSpeed = rotationSpeed;
      this.time = System.currentTimeMillis();
      this.lifeTime = lifeTime;
      this.scale = scale;
      this.physicsMode = physicsMode;
      this.enableCollision = enableCollision;
      this.colorOffset = ThreadLocalRandom.current().nextInt(3600);
      if (lifeTime > 0.0F) {
         this.maxAge = (int)(lifeTime * 20.0F);
         this.age = this.maxAge + ThreadLocalRandom.current().nextInt(21);
      } else {
         this.age = ThreadLocalRandom.current().nextInt(100, 201);
         this.maxAge = this.age;
      }
   }

   protected void initMotion(float speed) {
      boolean isDrop = "Fall".equals(this.physicsMode);
      boolean isEmerge = "Emerge".equals(this.physicsMode);
      ThreadLocalRandom random = ThreadLocalRandom.current();
      if (isDrop) {
         this.motionX = 0.0F;
         this.motionY = (float)random.nextDouble(-0.2, -0.05);
         this.motionZ = 0.0F;
      } else if (isEmerge) {
         this.motionX = (float)random.nextDouble(-0.2, 0.2);
         this.motionY = (float)random.nextDouble(0.4, 0.7);
         this.motionZ = (float)random.nextDouble(-0.2, 0.2);
      } else {
         this.motionX = (float)random.nextDouble(-0.4, 0.4);
         this.motionY = (float)random.nextDouble(-0.1, 0.1);
         this.motionZ = (float)random.nextDouble(-0.4, 0.4);
      }
   }

   public boolean update(long currentTime, boolean isVisible) {
      return this.update(currentTime, isVisible, null);
   }

   public boolean update(long currentTime, boolean isVisible, BlockCollisionGrid collisionGrid) {
      this.isVisible = isVisible;
      if (!isVisible) {
         this.ticksSinceLastUpdate++;
         if (this.ticksSinceLastUpdate < 20) {
            return false;
         }

         this.age = this.age - this.ticksSinceLastUpdate;
      } else {
         this.age--;
      }

      this.ticksSinceLastUpdate = 0;
      if (this.age < 0) {
         return true;
      } else {
         this.px = this.x;
         this.py = this.y;
         this.pz = this.z;
         if (this.enableCollision && collisionGrid != null) {
            float[] repulsion = collisionGrid.calculateRepulsion(this.x, this.y, this.z);
            this.motionX = this.motionX + repulsion[0];
            this.motionY = this.motionY + repulsion[1];
            this.motionZ = this.motionZ + repulsion[2];
         }

         this.x = this.x + this.motionX;
         this.y = this.y + this.motionY;
         this.z = this.z + this.motionZ;
         this.motionX *= 0.9F;
         this.motionZ *= 0.9F;
         if ("Fly".equals(this.physicsMode)) {
            this.motionY *= 0.9F;
            this.motionY -= 0.001F;
         } else if ("Emerge".equals(this.physicsMode)) {
            this.motionY *= 0.85F;
         } else {
            this.motionY -= 0.001F;
         }

         this.prevRotationAngle = this.rotationAngle;
         this.rotationAngle = this.rotationAngle + this.rotationSpeed;
         this.updateAlpha(currentTime);
         return false;
      }
   }

   protected void updateAlpha(long currentTime) {
      this.cachedAlpha = (float)this.age / this.maxAge;
   }

   public abstract void render(MatrixStack var1, long var2);

   public float getAlpha() {
      return this.cachedAlpha;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getZ() {
      return this.z;
   }

   public float getPx() {
      return this.px;
   }

   public float getPy() {
      return this.py;
   }

   public float getPz() {
      return this.pz;
   }

   public long getTime() {
      return this.time;
   }

   public float getLifeTime() {
      return this.lifeTime;
   }

   public Color getColor() {
      return this.color;
   }

   public float getScale() {
      return this.scale;
   }

   public boolean isVisible() {
      return this.isVisible;
   }

   public int getColorOffset() {
      return this.colorOffset;
   }
}
