package padej.soup.implement.features.modules.particles.types;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.modules.particles.render.ParticleBatchRenderer;

public class FireFlyParticle implements QuickImports {
   private final List<FireFlyParticle.FireFlyTrail> trails = new ArrayList<>();
   private float prevPosX;
   private float prevPosY;
   private float prevPosZ;
   private float posX;
   private float posY;
   private float posZ;
   private float motionX;
   private float motionY;
   private float motionZ;
   private int age;
   private final int maxAge;
   private final Color color;
   private final int[] customColors;
   private final float size;
   private final int trailLength;
   private boolean visible = true;

   public FireFlyParticle(float posX, float posY, float posZ, Color color, int[] customColors, float size, int trailLength) {
      this.posX = posX;
      this.posY = posY;
      this.posZ = posZ;
      this.prevPosX = posX;
      this.prevPosY = posY;
      this.prevPosZ = posZ;
      ThreadLocalRandom random = ThreadLocalRandom.current();
      this.motionX = (float)((random.nextDouble() * 2.0 - 1.0) * 0.2);
      this.motionY = (float)((random.nextDouble() * 2.0 - 1.0) * 0.1);
      this.motionZ = (float)((random.nextDouble() * 2.0 - 1.0) * 0.2);
      this.age = random.nextInt(100, 301);
      this.maxAge = this.age;
      this.color = color;
      this.customColors = customColors;
      this.size = size;
      this.trailLength = trailLength;
   }

   public boolean tick(Predicate<FireFlyParticle> visibilityChecker) {
      if (mc.player != null && mc.world != null) {
         if (visibilityChecker != null) {
            this.visible = visibilityChecker.test(this);
         }

         double distSq = mc.player.squaredDistanceTo(this.posX, this.posY, this.posZ);
         if (!this.visible) {
            this.age -= 2;
         } else if (distSq > 100.0) {
            this.age -= 4;
         } else if (!mc.world.getBlockState(new BlockPos((int)this.posX, (int)this.posY, (int)this.posZ)).isAir()) {
            this.age -= 8;
         } else {
            this.age--;
         }

         if (this.age < 0) {
            return true;
         } else {
            this.trails.removeIf(FireFlyParticle.FireFlyTrail::update);
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.posX = this.posX + this.motionX;
            this.posY = this.posY + this.motionY;
            this.posZ = this.posZ + this.motionZ;
            float lastX = this.trails.isEmpty() ? this.prevPosX : this.trails.getLast().currentX;
            float lastY = this.trails.isEmpty() ? this.prevPosY : this.trails.getLast().currentY;
            float lastZ = this.trails.isEmpty() ? this.prevPosZ : this.trails.getLast().currentZ;
            this.trails.add(new FireFlyParticle.FireFlyTrail(lastX, lastY, lastZ, this.posX, this.posY, this.posZ, this.trailLength));
            this.motionX *= 0.99F;
            this.motionY *= 0.99F;
            this.motionZ *= 0.99F;
            return false;
         }
      } else {
         return true;
      }
   }

   private int getGradientColor(float trailPosition, float alpha) {
      if (this.customColors != null && this.customColors.length != 0) {
         if (this.customColors.length == 1) {
            return ColorUtil.replAlpha(this.customColors[0], (int)(alpha * 255.0F));
         } else {
            float normalizedPosition = Math.max(0.0F, Math.min(1.0F, trailPosition));
            float totalSegments = this.customColors.length - 1;
            float scaledPosition = normalizedPosition * totalSegments;
            int segmentIndex = Math.min((int)Math.floor(scaledPosition), this.customColors.length - 2);
            float localPosition = scaledPosition - segmentIndex;
            int color1 = this.customColors[segmentIndex];
            int color2 = this.customColors[segmentIndex + 1];
            int interpolated = this.interpolateColor(color1, color2, localPosition);
            return ColorUtil.replAlpha(interpolated, (int)(alpha * 255.0F));
         }
      } else {
         return ColorUtil.replAlpha(this.color.getRGB(), (int)(alpha * 255.0F));
      }
   }

   private int interpolateColor(int color1, int color2, float factor) {
      int a1 = color1 >> 24 & 0xFF;
      int r1 = color1 >> 16 & 0xFF;
      int g1 = color1 >> 8 & 0xFF;
      int b1 = color1 & 0xFF;
      int a2 = color2 >> 24 & 0xFF;
      int r2 = color2 >> 16 & 0xFF;
      int g2 = color2 >> 8 & 0xFF;
      int b2 = color2 & 0xFF;
      int a = (int)(a1 + (a2 - a1) * factor);
      int r = (int)(r1 + (r2 - r1) * factor);
      int g = (int)(g1 + (g2 - g1) * factor);
      int b = (int)(b1 + (b2 - b1) * factor);
      return a << 24 | r << 16 | g << 8 | b;
   }

   public void render(MatrixStack stack) {
      if (!this.trails.isEmpty()) {
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cameraPos = camera.getPos();
         float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
         float pitchRad = (float)Math.toRadians(camera.getPitch());
         float yawRad = (float)Math.toRadians(camera.getYaw());
         Matrix4f preRotation = new Matrix4f().rotationX(pitchRad).rotateY(yawRad + (float) Math.PI);
         Matrix4f postRotation = new Matrix4f().rotationY(-yawRad).rotateX(pitchRad);
         Matrix4f workMatrix = new Matrix4f();
         int totalTrails = this.trails.size();

         for (int i = 0; i < totalTrails; i++) {
            FireFlyParticle.FireFlyTrail trail = this.trails.get(i);
            float interpX = trail.interpolateX(tickDelta);
            float interpY = trail.interpolateY(tickDelta);
            float interpZ = trail.interpolateZ(tickDelta);
            workMatrix.set(preRotation).translate(interpX - (float)cameraPos.x, interpY - (float)cameraPos.y, interpZ - (float)cameraPos.z).mul(postRotation);
            float particleAlpha = (float)this.age / this.maxAge;
            float trailAlpha = trail.animation(tickDelta);
            float finalAlpha = particleAlpha * trailAlpha;
            float trailPosition = 1.0F - (float)i / totalTrails;
            int colorWithAlpha;
            if (this.customColors != null && this.customColors.length > 1) {
               colorWithAlpha = this.getGradientColor(trailPosition, finalAlpha);
            } else {
               colorWithAlpha = ColorUtil.replAlpha(this.color.getRGB(), (int)(finalAlpha * 255.0F));
            }

            ParticleBatchRenderer.queueFireflyTrail(workMatrix, this.size, colorWithAlpha);
         }
      }
   }

   public List<FireFlyParticle.FireFlyTrail> getTrails() {
      return this.trails;
   }

   public float getPosX() {
      return this.posX;
   }

   public float getPosY() {
      return this.posY;
   }

   public float getPosZ() {
      return this.posZ;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public static class FireFlyTrail {
      private float prevX;
      private float prevY;
      private float prevZ;
      public float currentX;
      public float currentY;
      public float currentZ;
      private int ticks;
      private int prevTicks;
      private final int maxLifetime;

      public FireFlyTrail(float fromX, float fromY, float fromZ, float toX, float toY, float toZ, int trailLength) {
         this.prevX = fromX;
         this.prevY = fromY;
         this.prevZ = fromZ;
         this.currentX = toX;
         this.currentY = toY;
         this.currentZ = toZ;
         this.ticks = trailLength;
         this.maxLifetime = trailLength;
      }

      public float interpolateX(float pt) {
         return this.prevX + (this.currentX - this.prevX) * pt;
      }

      public float interpolateY(float pt) {
         return this.prevY + (this.currentY - this.prevY) * pt;
      }

      public float interpolateZ(float pt) {
         return this.prevZ + (this.currentZ - this.prevZ) * pt;
      }

      public float animation(float pt) {
         float currentTicks = this.prevTicks + (this.ticks - this.prevTicks) * pt;
         return Math.max(0.0F, currentTicks / this.maxLifetime);
      }

      public boolean update() {
         this.prevTicks = this.ticks;
         this.prevX = this.currentX;
         this.prevY = this.currentY;
         this.prevZ = this.currentZ;
         return this.ticks-- <= 0;
      }
   }
}
