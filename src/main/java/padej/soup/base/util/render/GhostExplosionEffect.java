package padej.soup.base.util.render;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.core.Main;
import padej.soup.implement.features.modules.particles.render.ParticleBatchRenderer;

public class GhostExplosionEffect {
   private static final MinecraftClient mc = Main.mc;
   private static final List<GhostExplosionEffect.ActiveGhostExplosion> activeExplosions = new ArrayList<>();
   private static final float BASE_FLY_SPEED_Y = 0.06F;
   private static final float BASE_DRIFT_SPEED_XZ = 0.03F;
   private static final int CHAIN_ITERATIONS = 5;
   private static final float FLAGELLUM_AMPLITUDE = 0.035F;
   private static final float FLAGELLUM_FREQUENCY = 12.0F;
   private static final float FLAGELLUM_WAVE_SPEED = 10.0F;
   private static final MatrixStack scratchStack = new MatrixStack();

   public static boolean hasActiveExplosions() {
      return !activeExplosions.isEmpty();
   }

   public static void spawn(
      Vec3d position,
      float speed,
      float length,
      float radiusModifier,
      float headSize,
      float tailSize,
      float subdivisions,
      int ghostsCount,
      String trajectory,
      Identifier texture,
      String blendMode,
      int[] customColors,
      float red,
      float speedModifier,
      long lifetimeMs,
      float extraSpeed
   ) {
      Vec3d entityPos;
      float halfHeight;
      float width;
      double interpolate;
      if (TargetRenderer.hasLastGhostRenderPos()) {
         entityPos = TargetRenderer.getLastGhostEntityPos();
         interpolate = TargetRenderer.getLastGhostInterpolate();
         halfHeight = TargetRenderer.getLastGhostHalfHeight();
         width = TargetRenderer.getLastGhostWidth();
      } else {
         entityPos = position.add(0.0, -0.9, 0.0);
         halfHeight = 1.0F;
         width = 0.6F;
         interpolate = MathUtil.interpolate((float)(mc.player.age - 1), (float)mc.player.age);
      }

      spawnInternal(
         entityPos,
         halfHeight,
         width,
         interpolate,
         speed,
         length,
         radiusModifier,
         headSize,
         tailSize,
         subdivisions,
         ghostsCount,
         trajectory,
         texture,
         blendMode,
         customColors,
         red,
         speedModifier,
         lifetimeMs,
         extraSpeed
      );
   }

   public static void spawn(
      LivingEntity target,
      float speed,
      float length,
      float radiusModifier,
      float headSize,
      float tailSize,
      float subdivisions,
      int ghostsCount,
      String trajectory,
      Identifier texture,
      String blendMode,
      int[] customColors,
      float red,
      float speedModifier,
      long lifetimeMs,
      float extraSpeed
   ) {
      Vec3d entityPos;
      float halfHeight;
      float width;
      double interpolate;
      if (TargetRenderer.hasLastGhostRenderPos()) {
         entityPos = TargetRenderer.getLastGhostEntityPos();
         interpolate = TargetRenderer.getLastGhostInterpolate();
         halfHeight = TargetRenderer.getLastGhostHalfHeight();
         width = TargetRenderer.getLastGhostWidth();
      } else {
         entityPos = MathUtil.interpolate(target);
         halfHeight = target.getHeight() / 2.0F + 0.1F;
         width = target.getWidth();
         interpolate = MathUtil.interpolate((float)(mc.player.age - 1), (float)mc.player.age);
      }

      spawnInternal(
         entityPos,
         halfHeight,
         width,
         interpolate,
         speed,
         length,
         radiusModifier,
         headSize,
         tailSize,
         subdivisions,
         ghostsCount,
         trajectory,
         texture,
         blendMode,
         customColors,
         red,
         speedModifier,
         lifetimeMs,
         extraSpeed
      );
   }

   private static void spawnInternal(
      Vec3d entityPos,
      float halfHeight,
      float width,
      double interpolate,
      float speed,
      float length,
      float radiusModifier,
      float headSize,
      float tailSize,
      float subdivisions,
      int ghostsCount,
      String trajectory,
      Identifier texture,
      String blendMode,
      int[] customColors,
      float red,
      float speedModifier,
      long lifetimeMs,
      float extraSpeed
   ) {
      boolean isSpiral = "Spiral".equals(trajectory);
      boolean isDefault = "Standard".equals(trajectory);
      int baseGhostLength = Math.round(10.0F * length);
      int subdivisionsInt = Math.round(subdivisions);
      int totalPoints = baseGhostLength * subdivisionsInt;
      double phaseShiftStep = Math.toRadians(360.0 / ghostsCount);
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      List<GhostExplosionEffect.FlyingGhost> ghosts = new ArrayList<>();

      for (int j = 0; j < ghostsCount; j++) {
         double phaseShift = phaseShiftStep * j;
         double cosX = 0.0;
         double sinX = 0.0;
         double cosY = 0.0;
         double sinY = 0.0;
         double cosZ = 0.0;
         double sinZ = 0.0;
         if (!isSpiral && !isDefault) {
            double angleX = phaseShiftStep * j + interpolate * 0.02;
            double angleY = phaseShiftStep * j * 1.618 + interpolate * 0.024;
            double angleZ = phaseShiftStep * j * 2.618 + interpolate * 0.016;
            cosX = Math.cos(angleX);
            sinX = Math.sin(angleX);
            cosY = Math.cos(angleY);
            sinY = Math.sin(angleY);
            cosZ = Math.cos(angleZ);
            sinZ = Math.sin(angleZ);
         }

         float[][] trailPoints = new float[totalPoints][3];

         for (int i = 0; i < baseGhostLength; i++) {
            for (int sub = 0; sub < subdivisionsInt; sub++) {
               float t = (float)sub / subdivisionsInt;
               float currentStep = i + t;
               int pointIndex = i * subdivisionsInt + sub;
               double radians;
               double sinQuad;
               if (isSpiral) {
                  radians = ((currentStep / 2.0F + interpolate * speed) * baseGhostLength + Math.toDegrees(phaseShift)) * Math.PI / 180.0;
                  sinQuad = Math.sin((interpolate * 2.5 * speed + currentStep * (j + halfHeight)) * 2.0 * Math.PI / 180.0) / 2.0;
               } else if (isDefault) {
                  int ghostIndexOnOrbit = j / 3;
                  int ghostsPerOrbit = (ghostsCount + 2) / 3;
                  double orbitPhaseShift = ghostIndexOnOrbit * Math.PI * 2.0 / ghostsPerOrbit;
                  radians = (interpolate * speed + currentStep * 0.5) * 0.3 + orbitPhaseShift;
                  sinQuad = 0.0;
               } else {
                  double baseAngle = (interpolate * speed + currentStep * 0.5) * 0.15;
                  radians = baseAngle + phaseShift;
                  sinQuad = 0.0;
               }

               float worldX;
               float worldY;
               float worldZ;
               if (isSpiral) {
                  worldX = (float)(entityPos.x + Math.cos(radians) * width * radiusModifier);
                  worldY = (float)(entityPos.y + halfHeight + sinQuad);
                  worldZ = (float)(entityPos.z + Math.sin(radians) * width * radiusModifier);
               } else if (isDefault) {
                  double radius = width * radiusModifier * 0.7;
                  double s = Math.sin(radians) * radius;
                  double c = Math.cos(radians) * radius;
                  double offsetX = 0.0;
                  double offsetY = 0.0;
                  double offsetZ = 0.0;
                  switch (j % 3) {
                     case 0:
                        offsetX = s;
                        offsetY = c;
                        offsetZ = -c;
                        break;
                     case 1:
                        offsetX = -s;
                        offsetY = s;
                        offsetZ = -c;
                        break;
                     case 2:
                        offsetX = s;
                        offsetY = -c;
                        offsetZ = -c;
                  }

                  worldX = (float)(entityPos.x + offsetX);
                  worldY = (float)(entityPos.y + halfHeight + offsetY);
                  worldZ = (float)(entityPos.z + offsetZ);
               } else {
                  double orbitRadius = width * radiusModifier;
                  double orbitAngle = radians * 2.0 + j * 1.0;
                  double baseX = Math.cos(orbitAngle) * orbitRadius;
                  double baseY = Math.sin(orbitAngle) * orbitRadius;
                  double baseZ = 0.0;
                  double tempY = baseY * cosX - baseZ * sinX;
                  double tempZ = baseY * sinX + baseZ * cosX;
                  double tempX = baseX * cosY + tempZ * sinY;
                  tempZ = -baseX * sinY + tempZ * cosY;
                  double var105 = tempX * cosZ - tempY * sinZ;
                  tempY = tempX * sinZ + tempY * cosZ;
                  worldX = (float)(entityPos.x + var105);
                  worldY = (float)(entityPos.y + halfHeight + tempY);
                  worldZ = (float)(entityPos.z + tempZ);
               }

               trailPoints[pointIndex] = new float[]{worldX, worldY, worldZ};
            }
         }

         float[][] prevTrailPoints = new float[totalPoints][3];

         for (int k = 0; k < totalPoints; k++) {
            prevTrailPoints[k] = new float[]{trailPoints[k][0], trailPoints[k][1], trailPoints[k][2]};
         }

         float[] segmentLengths = new float[totalPoints - 1];

         for (int k = 0; k < totalPoints - 1; k++) {
            float dx = trailPoints[k + 1][0] - trailPoints[k][0];
            float dy = trailPoints[k + 1][1] - trailPoints[k][1];
            float dz = trailPoints[k + 1][2] - trailPoints[k][2];
            segmentLengths[k] = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (segmentLengths[k] < 1.0E-4F) {
               segmentLengths[k] = 1.0E-4F;
            }
         }

         int headIdx = totalPoints - 1;
         float headX = trailPoints[headIdx][0];
         float headY = trailPoints[headIdx][1];
         float headZ = trailPoints[headIdx][2];
         double dx = headX - entityPos.x;
         double dz = headZ - entityPos.z;
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist < 0.01) {
            dist = 1.0;
         }

         GhostExplosionEffect.FlyingGhost ghost = new GhostExplosionEffect.FlyingGhost();
         ghost.trailPoints = trailPoints;
         ghost.prevTrailPoints = prevTrailPoints;
         ghost.segmentLengths = segmentLengths;
         ghost.totalPoints = totalPoints;
         ghost.baseGhostLength = baseGhostLength;
         ghost.subdivisionsInt = subdivisionsInt;
         float flySpeedY = 0.06F * speedModifier * radiusModifier * extraSpeed;
         float driftSpeedXZ = 0.03F * speedModifier * radiusModifier * extraSpeed;
         float outFactor = 0.7F + rng.nextFloat() * 0.6F;
         ghost.velX = (float)(dx / dist * driftSpeedXZ * outFactor) + (rng.nextFloat() - 0.5F) * 0.01F;
         ghost.velY = flySpeedY * (0.8F + rng.nextFloat() * 0.4F);
         ghost.velZ = (float)(dz / dist * driftSpeedXZ * outFactor) + (rng.nextFloat() - 0.5F) * 0.01F;
         ghost.swayPhase = rng.nextFloat() * (float) (Math.PI * 2);
         ghost.swayAmplitude = 0.01F + rng.nextFloat() * 0.015F;
         ghost.texture = texture;
         ghost.blendMode = blendMode;
         ghost.headSize = headSize;
         ghost.tailSize = tailSize;
         ghost.customColors = customColors;
         ghost.red = red;
         ghosts.add(ghost);
      }

      GhostExplosionEffect.ActiveGhostExplosion newExplosion = new GhostExplosionEffect.ActiveGhostExplosion(System.currentTimeMillis(), ghosts, lifetimeMs);
      activeExplosions.add(newExplosion);
   }

   public static void tick() {
      if (!activeExplosions.isEmpty()) {
         long now = System.currentTimeMillis();
         activeExplosions.removeIf(e -> now - e.startTime > e.lifetimeMs + 200L);
         long currentTick = mc.world != null ? mc.world.getTime() : 0L;

         for (GhostExplosionEffect.ActiveGhostExplosion explosion : activeExplosions) {
            if (explosion.lastTickedTick != currentTick) {
               explosion.lastTickedTick = currentTick;
               float progress = Math.min((float)(now - explosion.startTime) / (float)explosion.lifetimeMs, 1.0F);

               for (GhostExplosionEffect.FlyingGhost g : explosion.ghosts) {
                  for (int k = 0; k < g.totalPoints; k++) {
                     g.prevTrailPoints[k][0] = g.trailPoints[k][0];
                     g.prevTrailPoints[k][1] = g.trailPoints[k][1];
                     g.prevTrailPoints[k][2] = g.trailPoints[k][2];
                  }

                  int headIdx = g.totalPoints - 1;
                  g.trailPoints[headIdx][0] = g.trailPoints[headIdx][0] + g.velX;
                  g.trailPoints[headIdx][1] = g.trailPoints[headIdx][1] + g.velY;
                  g.trailPoints[headIdx][2] = g.trailPoints[headIdx][2] + g.velZ;
                  g.trailPoints[headIdx][0] = g.trailPoints[headIdx][0] + (float)Math.sin(g.swayPhase + progress * 8.0F) * g.swayAmplitude;
                  g.trailPoints[headIdx][2] = g.trailPoints[headIdx][2] + (float)Math.cos(g.swayPhase + progress * 8.0F) * g.swayAmplitude;
                  g.velX *= 0.985F;
                  g.velZ *= 0.985F;
                  g.velY *= 0.995F;

                  for (int iter = 0; iter < 5; iter++) {
                     for (int k = headIdx - 1; k >= 0; k--) {
                        float cdx = g.trailPoints[k + 1][0] - g.trailPoints[k][0];
                        float cdy = g.trailPoints[k + 1][1] - g.trailPoints[k][1];
                        float cdz = g.trailPoints[k + 1][2] - g.trailPoints[k][2];
                        float dist = (float)Math.sqrt(cdx * cdx + cdy * cdy + cdz * cdz);
                        float targetDist = g.segmentLengths[k];
                        if (dist > targetDist && dist > 1.0E-4F) {
                           float excess = (dist - targetDist) / dist;
                           g.trailPoints[k][0] = g.trailPoints[k][0] + cdx * excess;
                           g.trailPoints[k][1] = g.trailPoints[k][1] + cdy * excess;
                           g.trailPoints[k][2] = g.trailPoints[k][2] + cdz * excess;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void render() {
      if (!activeExplosions.isEmpty()) {
         long now = System.currentTimeMillis();
         Camera camera = mc.getEntityRenderDispatcher().camera;
         float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
         float timeSeconds = (float)(now % 100000L) / 1000.0F;

         for (GhostExplosionEffect.ActiveGhostExplosion explosion : activeExplosions) {
            float age = (float)(now - explosion.startTime) / (float)explosion.lifetimeMs;
            float life = Math.max(0.0F, 1.0F - age);

            for (GhostExplosionEffect.FlyingGhost g : explosion.ghosts) {
               if (!(life < 0.01F)) {
                  for (int i = 0; i < g.baseGhostLength; i++) {
                     for (int sub = 0; sub < g.subdivisionsInt; sub++) {
                        int pointIndex = i * g.subdivisionsInt + sub;
                        if (pointIndex >= g.totalPoints) {
                           break;
                        }

                        float currentStep = i + (float)sub / g.subdivisionsInt;
                        float ghostPosition = 1.0F - currentStep / g.baseGhostLength;
                        float px = MathHelper.lerp(tickDelta, g.prevTrailPoints[pointIndex][0], g.trailPoints[pointIndex][0]);
                        float py = MathHelper.lerp(tickDelta, g.prevTrailPoints[pointIndex][1], g.trailPoints[pointIndex][1]);
                        float pz = MathHelper.lerp(tickDelta, g.prevTrailPoints[pointIndex][2], g.trailPoints[pointIndex][2]);
                        float dirX;
                        float dirY;
                        float dirZ;
                        if (pointIndex < g.totalPoints - 1) {
                           dirX = g.trailPoints[pointIndex + 1][0] - g.trailPoints[pointIndex][0];
                           dirY = g.trailPoints[pointIndex + 1][1] - g.trailPoints[pointIndex][1];
                           dirZ = g.trailPoints[pointIndex + 1][2] - g.trailPoints[pointIndex][2];
                        } else {
                           dirX = g.velX;
                           dirY = g.velY;
                           dirZ = g.velZ;
                        }

                        float dirLen = (float)Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                        if (dirLen > 1.0E-4F) {
                           dirX /= dirLen;
                           dirY /= dirLen;
                           dirZ /= dirLen;
                        } else {
                           dirX = 0.0F;
                           dirY = 1.0F;
                           dirZ = 0.0F;
                        }

                        float upX = 0.0F;
                        float upY = 1.0F;
                        float upZ = 0.0F;
                        if (Math.abs(dirY) > 0.9F) {
                           upX = 1.0F;
                           upY = 0.0F;
                           upZ = 0.0F;
                        }

                        float perpX = dirY * upZ - dirZ * upY;
                        float perpY = dirZ * upX - dirX * upZ;
                        float perpZ = dirX * upY - dirY * upX;
                        float pLen = (float)Math.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);
                        if (pLen > 1.0E-4F) {
                           perpX /= pLen;
                           perpY /= pLen;
                           perpZ /= pLen;
                        }

                        float wiggleAmplitude = 0.035F * ghostPosition * ghostPosition;
                        float wigglePhase = currentStep * 12.0F / g.baseGhostLength * (float) Math.PI * 2.0F - timeSeconds * 10.0F + g.swayPhase;
                        float wiggle = (float)Math.sin(wigglePhase) * wiggleAmplitude;
                        px += perpX * wiggle;
                        py += perpY * wiggle;
                        pz += perpZ * wiggle;
                        float fadeAnim;
                        if ("Smoke".equals(g.blendMode)) {
                           long remainingMs = explosion.lifetimeMs - (now - explosion.startTime);
                           fadeAnim = remainingMs < 500L ? Math.max(0.0F, (float)remainingMs / 500.0F) : 1.0F;
                        } else {
                           fadeAnim = life;
                        }

                        float trailGradient = currentStep / g.baseGhostLength;
                        int color = computeGhostColor(ghostPosition, g.customColors, g.red);
                        color = ColorUtil.multAlpha(color, trailGradient * fadeAnim);
                        float scale = g.headSize + (g.tailSize - g.headSize) * ghostPosition;
                        scale *= fadeAnim;
                        double camRelX = px - camera.getPos().x;
                        double camRelY = py - camera.getPos().y;
                        double camRelZ = pz - camera.getPos().z;
                        MatrixStack matrices = scratchStack;
                        matrices.loadIdentity();
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                        matrices.translate(camRelX, camRelY, camRelZ);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                        ParticleBatchRenderer.queueGhost(matrices.peek().getPositionMatrix(), g.texture, scale, color, g.blendMode);
                     }
                  }
               }
            }
         }
      }
   }

   public static void clear() {
      activeExplosions.clear();
   }

   private static int computeGhostColor(float ghostPosition, int[] customColors, float red) {
      if (customColors != null && customColors.length > 0) {
         if (customColors.length == 1) {
            return ColorUtil.gradientToRed(customColors[0], red);
         } else {
            float normalizedPos = Math.max(0.0F, Math.min(1.0F, ghostPosition));
            float totalSeg = customColors.length - 1;
            float scaledPos = normalizedPos * totalSeg;
            int idx1 = Math.min((int)Math.floor(scaledPos), customColors.length - 2);
            float localPos = scaledPos - idx1;
            int c1 = customColors[idx1];
            int c2 = customColors[idx1 + 1];
            int interp = lerpColor(c1, c2, localPos);
            return ColorUtil.gradientToRed(interp, red);
         }
      } else {
         float colorIndex = ghostPosition * 180.0F;
         return ColorUtil.gradientToRed(ColorUtil.fade((int)colorIndex), red);
      }
   }

   private static int lerpColor(int color1, int color2, float t) {
      int r1 = color1 >> 16 & 0xFF;
      int g1 = color1 >> 8 & 0xFF;
      int b1 = color1 & 0xFF;
      int r2 = color2 >> 16 & 0xFF;
      int g2 = color2 >> 8 & 0xFF;
      int b2 = color2 & 0xFF;
      int r = (int)(r1 + (r2 - r1) * t);
      int g = (int)(g1 + (g2 - g1) * t);
      int b = (int)(b1 + (b2 - b1) * t);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private static class ActiveGhostExplosion {
      final long startTime;
      final long lifetimeMs;
      final List<GhostExplosionEffect.FlyingGhost> ghosts;
      long lastTickedTick = -1L;

      ActiveGhostExplosion(long startTime, List<GhostExplosionEffect.FlyingGhost> ghosts, long lifetimeMs) {
         this.startTime = startTime;
         this.ghosts = ghosts;
         this.lifetimeMs = lifetimeMs;
      }
   }

   private static class FlyingGhost {
      float[][] trailPoints;
      float[][] prevTrailPoints;
      float[] segmentLengths;
      int totalPoints;
      int baseGhostLength;
      int subdivisionsInt;
      float velX;
      float velY;
      float velZ;
      float swayPhase;
      float swayAmplitude;
      Identifier texture;
      String blendMode;
      float headSize;
      float tailSize;
      int[] customColors;
      float red;
   }
}
