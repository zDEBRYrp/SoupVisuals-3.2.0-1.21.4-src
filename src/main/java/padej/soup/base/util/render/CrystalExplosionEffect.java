package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.joml.Vector4i;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.core.Main;

public class CrystalExplosionEffect {
   private static final MinecraftClient mc = Main.mc;
   private static final List<CrystalExplosionEffect.ExplosionInstance> activeExplosions = new ArrayList<>();
   private static final long LIFETIME_MS = 1500L;
   private static final float EXPLOSION_SPEED = 0.04F;
   private static final float GRAVITY = -0.001F;
   private static final float SPIN_PERTURBATION = 3.0F;
   private static final float TUMBLE_SPEED = 18.0F;
   private static final float SPIN_END_FACTOR = 0.4F;
   private static final float RADIUS_EXPANSION = 0.04F;
   private static final int NUM_SIDES = 4;
   private static final MatrixStack scratchMatrix = new MatrixStack();
   private static final MatrixStack scratchBillboardStack = new MatrixStack();
   private static final Vector3f[] scratchTopVerts = new Vector3f[4];
   private static final Vector3f[] scratchBottomVerts = new Vector3f[4];
   private static final Vector3f scratchVTop = new Vector3f();
   private static final Vector3f scratchVBottom = new Vector3f();
   private static final Vector3f scratchP1 = new Vector3f();
   private static final Vector3f scratchP2 = new Vector3f();
   private static final Vector3f scratchP3 = new Vector3f();
   private static final Vector4i scratchColorVec = new Vector4i();

   public static void spawnFromSnapshots(List<TargetESPCrystals.CrystalSnapshot> snapshots, Vec3d entityCenter, boolean glow, float glowSize) {
      if (snapshots != null && !snapshots.isEmpty()) {
         long now = System.currentTimeMillis();
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         List<CrystalExplosionEffect.ExplodingCrystal> crystals = new ArrayList<>();

         for (TargetESPCrystals.CrystalSnapshot snap : snapshots) {
            CrystalExplosionEffect.ExplodingCrystal ec = new CrystalExplosionEffect.ExplodingCrystal();
            ec.posX = (float)snap.worldPos().x;
            ec.posY = (float)snap.worldPos().y;
            ec.posZ = (float)snap.worldPos().z;
            ec.prevPosX = ec.posX;
            ec.prevPosY = ec.posY;
            ec.prevPosZ = ec.posZ;
            float relX = (float)(snap.worldPos().x - entityCenter.x);
            float relY = (float)(snap.worldPos().y - entityCenter.y);
            float relZ = (float)(snap.worldPos().z - entityCenter.z);
            ec.orbitCenterX = (float)entityCenter.x;
            ec.orbitCenterZ = (float)entityCenter.z;
            ec.orbitAngle = (float)Math.atan2(relX, relZ);
            ec.orbitRadius = (float)Math.sqrt(relX * relX + relZ * relZ);
            ec.orbitAngVel = (float)Math.toRadians(snap.orbitSpinY());
            float forceMult = 0.7F + rng.nextFloat() * 0.6F;
            ec.radiusSpeed = 0.04F * forceMult;
            double dist3D = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
            if (dist3D < 0.01) {
               dist3D = 1.0;
            }

            ec.velY = (float)(relY / dist3D * 0.04F * forceMult) + rng.nextFloat() * 0.01F;
            ec.rotX = snap.pitch();
            ec.rotY = snap.yaw();
            ec.rotZ = 0.0F;
            ec.prevRotX = ec.rotX;
            ec.prevRotY = ec.rotY;
            ec.prevRotZ = ec.rotZ;
            ec.spinX = (0.5F + rng.nextFloat() * 0.5F) * 18.0F * (rng.nextBoolean() ? 1 : -1);
            ec.spinY = snap.orbitSpinY() + (rng.nextFloat() - 0.5F) * 3.0F;
            ec.spinZ = (0.5F + rng.nextFloat() * 0.5F) * 18.0F * (rng.nextBoolean() ? 1 : -1);
            ec.size = snap.size();
            ec.color = snap.color();
            ec.glow = glow;
            ec.glowSize = glowSize;
            crystals.add(ec);
         }

         activeExplosions.add(new CrystalExplosionEffect.ExplosionInstance(crystals, now, 1500L));
      }
   }

   public static void spawnComputed(
      Vec3d entityPos,
      float entityHeight,
      float entityWidth,
      float crystalDistance,
      float crystalSize,
      boolean glow,
      float glowSize,
      int baseColor,
      int[] customColors,
      float speedModifier,
      long lifetimeMs,
      float extraSpeed
   ) {
      float circleStep = TargetRenderer.getInterpolatedCircleStep();
      long now = System.currentTimeMillis();
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      Vec3d entityCenter = entityPos.add(0.0, entityHeight * 0.5, 0.0);
      List<CrystalExplosionEffect.ExplodingCrystal> crystals = new ArrayList<>();
      float speed = 3.0F;
      double cs = circleStep * speed * 0.5;
      float orbitCos = (float)Math.cos(-cs);
      float orbitSin = (float)Math.sin(-cs);
      int totalCrystals = 19;

      for (int ci = 0; ci < totalCrystals; ci++) {
         float relX;
         float relY;
         float relZ;
         if (ci == 0) {
            relX = 0.0F;
            relY = entityHeight * 0.6F;
            relZ = 0.0F;
         } else if (ci == 1) {
            relX = 0.0F;
            relY = -entityHeight * 0.1F;
            relZ = 0.0F;
         } else {
            float angle = (float)(ci * 2 * Math.PI / totalCrystals);
            float radius = crystalDistance * (entityWidth / 0.6F);
            float baseX = (float)(Math.cos(angle) * radius);
            float baseZ = (float)(Math.sin(angle) * radius);
            relX = baseX * orbitCos - baseZ * orbitSin;
            relZ = baseX * orbitSin + baseZ * orbitCos;
            relY = entityHeight * 0.5F + (float)Math.sin(angle * 3.0F) * (entityHeight * 0.45F);
         }

         double worldX = entityPos.x + relX;
         double worldY = entityPos.y + relY;
         double worldZ = entityPos.z + relZ;
         double dx = worldX - entityCenter.x;
         double dy = worldY - entityCenter.y;
         double dz = worldZ - entityCenter.z;
         double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (dist < 0.01) {
            dx = rng.nextDouble() - 0.5;
            dy = rng.nextDouble() - 0.5;
            dz = rng.nextDouble() - 0.5;
            dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
         }

         dx /= dist;
         dy /= dist;
         dz /= dist;
         float forceMult = 0.7F + rng.nextFloat() * 0.6F;
         int color = getColorForCrystal(ci, totalCrystals, baseColor, customColors, circleStep);
         CrystalExplosionEffect.ExplodingCrystal ec = new CrystalExplosionEffect.ExplodingCrystal();
         ec.posX = (float)worldX;
         ec.posY = (float)worldY;
         ec.posZ = (float)worldZ;
         ec.prevPosX = ec.posX;
         ec.prevPosY = ec.posY;
         ec.prevPosZ = ec.posZ;
         ec.orbitCenterX = (float)entityPos.x;
         ec.orbitCenterZ = (float)entityPos.z;
         ec.orbitAngle = (float)Math.atan2(relX, relZ);
         double sqrted = Math.sqrt(relX * relX + relZ * relZ);
         ec.orbitRadius = (float)sqrted;
         ec.orbitAngVel = 0.15F * speedModifier * 3.0F * 0.5F;
         ec.radiusSpeed = 0.04F * forceMult * extraSpeed;
         ec.velY = (float)(dy * 0.04F * forceMult + rng.nextFloat() * 0.01F) * extraSpeed;
         float yaw = (float)Math.toDegrees(Math.atan2(relX, relZ));
         float distFC = (float)sqrted;
         ec.rotX = -((float)Math.toDegrees(Math.atan2(relY, distFC)));
         ec.rotY = yaw;
         ec.rotZ = 0.0F;
         ec.prevRotX = ec.rotX;
         ec.prevRotY = ec.rotY;
         ec.prevRotZ = ec.rotZ;
         float orbitSpinDeg = (float)Math.toDegrees(ec.orbitAngVel);
         ec.spinX = (0.5F + rng.nextFloat() * 0.5F) * 18.0F * (rng.nextBoolean() ? 1 : -1);
         ec.spinY = orbitSpinDeg + (rng.nextFloat() - 0.5F) * 3.0F;
         ec.spinZ = (0.5F + rng.nextFloat() * 0.5F) * 18.0F * (rng.nextBoolean() ? 1 : -1);
         ec.size = crystalSize;
         ec.color = color;
         ec.glow = glow;
         ec.glowSize = glowSize;
         crystals.add(ec);
      }

      activeExplosions.add(new CrystalExplosionEffect.ExplosionInstance(crystals, now, lifetimeMs));
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      Iterator<CrystalExplosionEffect.ExplosionInstance> it = activeExplosions.iterator();

      while (it.hasNext()) {
         CrystalExplosionEffect.ExplosionInstance explosion = it.next();
         if (now - explosion.spawnTime > explosion.lifetimeMs + 200L) {
            it.remove();
         } else {
            float progress = Math.min((float)(now - explosion.spawnTime) / (float)explosion.lifetimeMs, 1.0F);
            float eased = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
            float spinFactor = 1.0F - eased * 0.6F;

            for (CrystalExplosionEffect.ExplodingCrystal c : explosion.crystals) {
               c.prevPosX = c.posX;
               c.prevPosY = c.posY;
               c.prevPosZ = c.posZ;
               c.prevRotX = c.rotX;
               c.prevRotY = c.rotY;
               c.prevRotZ = c.rotZ;
               c.orbitAngle = c.orbitAngle + c.orbitAngVel * spinFactor;
               c.orbitRadius = c.orbitRadius + c.radiusSpeed;
               c.radiusSpeed *= 0.985F;
               c.posX = c.orbitCenterX + (float)Math.sin(c.orbitAngle) * c.orbitRadius;
               c.posZ = c.orbitCenterZ + (float)Math.cos(c.orbitAngle) * c.orbitRadius;
               c.posY = c.posY + c.velY;
               c.velY += -0.001F;
               c.rotX = c.rotX + c.spinX * spinFactor;
               c.rotY = c.rotY + c.spinY * spinFactor;
               c.rotZ = c.rotZ + c.spinZ * spinFactor;
            }
         }
      }
   }

   public static void render(MatrixStack eventStack) {
      if (!activeExplosions.isEmpty()) {
         if (RenderSystem.isOnRenderThread()) {
            long now = System.currentTimeMillis();
            Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
            float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

            for (CrystalExplosionEffect.ExplosionInstance explosion : activeExplosions) {
               float age = (float)(now - explosion.spawnTime) / (float)explosion.lifetimeMs;
               if (!(age > 1.0F)) {
                  float life = 1.0F - age;
                  float smoothAlpha = smoothEaseInOut(life);
                  float brightness = 0.3F + 0.7F * smoothAlpha;
                  float scaleMult = 0.8F + 0.2F * life;

                  for (CrystalExplosionEffect.ExplodingCrystal c : explosion.crystals) {
                     emitCrystalVertices(eventStack, cameraPos, c, smoothAlpha, brightness, scaleMult, tickDelta, bufferBuilder);
                  }
               }
            }

            try {
               BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            } catch (Exception var18) {
            }

            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();

            for (CrystalExplosionEffect.ExplosionInstance explosionx : activeExplosions) {
               float age = (float)(now - explosionx.spawnTime) / (float)explosionx.lifetimeMs;
               if (!(age > 1.0F)) {
                  float life = 1.0F - age;
                  float smoothAlpha = smoothEaseInOut(life);
                  float scaleMult = 0.8F + 0.2F * life;

                  for (CrystalExplosionEffect.ExplodingCrystal c : explosionx.crystals) {
                     if (c.glow && smoothAlpha > 0.1F) {
                        float lerpX = MathHelper.lerp(tickDelta, c.prevPosX, c.posX);
                        float lerpY = MathHelper.lerp(tickDelta, c.prevPosY, c.posY);
                        float lerpZ = MathHelper.lerp(tickDelta, c.prevPosZ, c.posZ);
                        drawGlow(
                           ColorUtil.multDark(c.color, 0.3F + 0.7F * smoothAlpha),
                           new Vec3d(lerpX, lerpY, lerpZ),
                           c.size * scaleMult,
                           c.glowSize,
                           smoothAlpha * 0.6F
                        );
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean hasActiveExplosions() {
      return !activeExplosions.isEmpty();
   }

   public static void clear() {
      activeExplosions.clear();
   }

   private static void emitCrystalVertices(
      MatrixStack eventStack,
      Vec3d cameraPos,
      CrystalExplosionEffect.ExplodingCrystal c,
      float alpha,
      float brightness,
      float scaleMult,
      float tickDelta,
      BufferBuilder bufferBuilder
   ) {
      if (!(alpha < 0.01F)) {
         float lerpX = MathHelper.lerp(tickDelta, c.prevPosX, c.posX);
         float lerpY = MathHelper.lerp(tickDelta, c.prevPosY, c.posY);
         float lerpZ = MathHelper.lerp(tickDelta, c.prevPosZ, c.posZ);
         float lerpRotX = MathHelper.lerp(tickDelta, c.prevRotX, c.rotX);
         float lerpRotY = MathHelper.lerp(tickDelta, c.prevRotY, c.rotY);
         float lerpRotZ = MathHelper.lerp(tickDelta, c.prevRotZ, c.rotZ);
         MatrixStack matrixStack = scratchMatrix;
         matrixStack.loadIdentity();
         matrixStack.peek().getPositionMatrix().set(eventStack.peek().getPositionMatrix());
         matrixStack.peek().getPositionMatrix().setTranslation(0.0F, 0.0F, 0.0F);
         matrixStack.peek().getNormalMatrix().set(eventStack.peek().getNormalMatrix());
         matrixStack.push();
         matrixStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(lerpRotY));
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lerpRotX));
         matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(lerpRotZ));
         float size = c.size * scaleMult;
         matrixStack.scale(size, size, size);
         int color = ColorUtil.multDark(c.color, brightness);
         int alphaInt = (int)(255.0F * alpha);
         color = color & 16777215 | alphaInt << 24;
         emitCrystalGeometry(matrixStack, bufferBuilder, color, alpha, c.size);
         matrixStack.pop();
      }
   }

   private static void emitCrystalGeometry(MatrixStack matrixStack, BufferBuilder bufferBuilder, int color, float alpha, float crystalSize) {
      float hPyramid = crystalSize * 1.2F;
      float baseRadius = crystalSize * 0.8F;

      for (int i = 0; i < 4; i++) {
         float angle = (float)((Math.PI * 2) * i / 4.0);
         scratchTopVerts[i].set((float)(baseRadius * Math.cos(angle)), 0.0F, (float)(baseRadius * Math.sin(angle)));
         scratchBottomVerts[i].set(scratchTopVerts[i]);
      }

      scratchVTop.set(0.0F, hPyramid, 0.0F);
      scratchVBottom.set(0.0F, -hPyramid, 0.0F);

      for (int i = 0; i < 4; i++) {
         Vector3f v1 = scratchTopVerts[i];
         Vector3f v2 = scratchTopVerts[(i + 1) % 4];
         float gf = i / 4.0F;
         drawTriangle(bufferBuilder, matrixStack, scratchVTop, v1, v2, getFaceColor(color, gf, 0), alpha);
         Vector3f v3 = scratchBottomVerts[i];
         Vector3f v4 = scratchBottomVerts[(i + 1) % 4];
         drawTriangle(bufferBuilder, matrixStack, scratchVBottom, v4, v3, getFaceColor(color, gf, 1), alpha);
      }

      for (int i = 0; i < 4; i++) {
         Vector3f v1 = scratchTopVerts[i];
         Vector3f v2 = scratchTopVerts[(i + 1) % 4];
         Vector3f v3 = scratchBottomVerts[i];
         Vector3f v4 = scratchBottomVerts[(i + 1) % 4];
         float gf = i / 4.0F;
         int sc = getFaceColor(color, gf, 2);
         drawTriangle(bufferBuilder, matrixStack, v1, v2, v3, sc, alpha);
         drawTriangle(bufferBuilder, matrixStack, v2, v4, v3, sc, alpha);
      }
   }

   private static void drawTriangle(BufferBuilder buf, MatrixStack ms, Vector3f v1, Vector3f v2, Vector3f v3, int color, float alpha) {
      Entry entry = ms.peek();
      entry.getPositionMatrix().transformPosition(v1.x, v1.y, v1.z, scratchP1);
      entry.getPositionMatrix().transformPosition(v2.x, v2.y, v2.z, scratchP2);
      entry.getPositionMatrix().transformPosition(v3.x, v3.y, v3.z, scratchP3);
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      buf.vertex(scratchP1.x, scratchP1.y, scratchP1.z).color(r, g, b, alpha);
      buf.vertex(scratchP2.x, scratchP2.y, scratchP2.z).color(r, g, b, alpha);
      buf.vertex(scratchP3.x, scratchP3.y, scratchP3.z).color(r, g, b, alpha);
   }

   private static void drawGlow(int color, Vec3d pos, float crystalSize, float glowSize, float alpha) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      Vec3d vec = pos.subtract(camera.getPos());
      MatrixStack bs = scratchBillboardStack;
      bs.loadIdentity();
      bs.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      bs.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      bs.translate(vec.x, vec.y, vec.z);
      bs.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      bs.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      Entry entry = bs.peek().copy();
      float size = crystalSize * glowSize;
      int glowColor = ColorUtil.multAlpha(color, alpha);
      scratchColorVec.set(glowColor, glowColor, glowColor, glowColor);
      Render3DUtil.drawTexture(entry, Identifier.of("textures/particles/bloom/bloom_soft.png"), -size / 2.0F, -size / 2.0F, size, size, scratchColorVec, true);
   }

   private static float smoothEaseInOut(float t) {
      return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
   }

   private static int getFaceColor(int baseColor, float gradientFactor, int faceType) {
      int r = baseColor >> 16 & 0xFF;
      int g = baseColor >> 8 & 0xFF;
      int b = baseColor & 0xFF;
      if (faceType == 0) {
         r = Math.min(255, (int)(r * (1.5F + gradientFactor * 0.8F)));
         g = Math.max(0, (int)(g * (0.6F + gradientFactor * 0.4F)));
         b = Math.max(0, (int)(b * (0.4F - gradientFactor * 0.2F)));
      } else if (faceType == 1) {
         r = Math.max(0, (int)(r * (0.4F - gradientFactor * 0.2F)));
         g = Math.max(0, (int)(g * (0.5F + gradientFactor * 0.3F)));
         b = Math.min(255, (int)(b * (1.5F + gradientFactor * 0.8F)));
      } else {
         r = Math.max(0, (int)(r * (0.7F + Math.sin(gradientFactor * Math.PI * 2.0) * 0.3F)));
         g = Math.min(255, (int)(g * (1.3F + Math.cos(gradientFactor * Math.PI * 2.0) * 0.5)));
         b = Math.max(0, (int)(b * (0.6F + Math.sin(gradientFactor * Math.PI * 3.0) * 0.2F)));
      }

      float brightness = 0.7F + 0.3F * (float)Math.sin(gradientFactor * Math.PI * 4.0);
      r = MathHelper.clamp((int)(r * brightness), 0, 255);
      g = MathHelper.clamp((int)(g * brightness), 0, 255);
      b = MathHelper.clamp((int)(b * brightness), 0, 255);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private static int getColorForCrystal(int idx, int total, int baseColor, int[] cc, float cs) {
      if (cc != null && cc.length != 0) {
         float ap = (float)idx / total;
         float gp = (ap + cs * 0.3F % 1.0F) % 1.0F;
         if (cc.length == 1) {
            return cc[0];
         } else {
            float sp = gp * cc.length;
            int i1 = (int)sp % cc.length;
            int i2 = (i1 + 1) % cc.length;
            float bl = (float)(Math.sin((sp - (int)sp - 0.5F) * Math.PI) * 0.5 + 0.5);
            return lerpColor(cc[i1], cc[i2], bl);
         }
      } else {
         return baseColor;
      }
   }

   private static int lerpColor(int c1, int c2, float t) {
      int a1 = c1 >> 24 & 0xFF;
      int r1 = c1 >> 16 & 0xFF;
      int g1 = c1 >> 8 & 0xFF;
      int b1 = c1 & 0xFF;
      int a2 = c2 >> 24 & 0xFF;
      int r2 = c2 >> 16 & 0xFF;
      int g2 = c2 >> 8 & 0xFF;
      int b2 = c2 & 0xFF;
      return (int)(a1 + (a2 - a1) * t) << 24 | (int)(r1 + (r2 - r1) * t) << 16 | (int)(g1 + (g2 - g1) * t) << 8 | (int)(b1 + (b2 - b1) * t);
   }

   static {
      for (int i = 0; i < 4; i++) {
         scratchTopVerts[i] = new Vector3f();
         scratchBottomVerts[i] = new Vector3f();
      }
   }

   private static class ExplodingCrystal {
      float orbitCenterX;
      float orbitCenterZ;
      float orbitAngle;
      float orbitRadius;
      float orbitAngVel;
      float radiusSpeed;
      float posX;
      float posY;
      float posZ;
      float velY;
      float prevPosX;
      float prevPosY;
      float prevPosZ;
      float rotX;
      float rotY;
      float rotZ;
      float prevRotX;
      float prevRotY;
      float prevRotZ;
      float spinX;
      float spinY;
      float spinZ;
      float size;
      int color;
      boolean glow;
      float glowSize;
   }

   private static class ExplosionInstance {
      final List<CrystalExplosionEffect.ExplodingCrystal> crystals;
      final long spawnTime;
      final long lifetimeMs;

      ExplosionInstance(List<CrystalExplosionEffect.ExplodingCrystal> crystals, long spawnTime, long lifetimeMs) {
         this.crystals = crystals;
         this.spawnTime = spawnTime;
         this.lifetimeMs = lifetimeMs;
      }
   }
}
