package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.joml.Vector4i;
import padej.soup.api.event.events.render.WorldRenderEvent;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.core.Main;

public class TargetESPCrystals {
   public static final TargetESPCrystals instance = new TargetESPCrystals();
   private final MinecraftClient mc;
   private final List<TargetESPCrystals.Crystal> crystals;
   private float crackAmount;
   private float hitAnimation;
   private float lastHurtTime;
   private long lastUpdateTick;
   private float lastRenderCircleStep;
   private Vec3d lastRenderEntityPos;
   private float lastRenderTargetHeight;
   private float lastRenderTargetWidth;
   private float lastRenderDistance;
   private final List<TargetESPCrystals.ActiveExplosion> activeExplosions;
   private static final long EXPL_LIFETIME_MS = 1500L;
   private static final float EXPL_RADIUS_SPEED = 0.04F;
   private static final float EXPL_GRAVITY = -0.001F;
   private static final float EXPL_TUMBLE = 18.0F;
   private static final float EXPL_SPIN_PERTURB = 3.0F;
   private static final float EXPL_SPIN_END = 0.4F;
   private static final float EXPL_SPEED = 0.04F;
   private static final boolean PULSE = true;
   private static final int NUM_SIDES = 4;
   private static final float SPEED = 3.0F;
   private static final boolean ROTATION = true;
   private static final float TRANSPARENCY = 0.8F;
   private final MatrixStack scratchMatrixStack;
   private final MatrixStack scratchBillboardStack;
   private final Vector3f[] scratchTopVerts;
   private final Vector3f[] scratchBottomVerts;
   private final Vector3f scratchVTop;
   private final Vector3f scratchVBottom;
   private final Vector3f scratchP1;
   private final Vector3f scratchP2;
   private final Vector3f scratchP3;
   private final Vector4i scratchColorVec;

   public TargetESPCrystals() {
      this.mc = Main.mc;
      this.crystals = new ArrayList<>();
      this.crackAmount = 0.0F;
      this.hitAnimation = 0.0F;
      this.lastHurtTime = 0.0F;
      this.lastUpdateTick = -1L;
      this.activeExplosions = new ArrayList<>();
      this.scratchMatrixStack = new MatrixStack();
      this.scratchBillboardStack = new MatrixStack();
      this.scratchTopVerts = new Vector3f[4];
      this.scratchBottomVerts = new Vector3f[4];
      this.scratchVTop = new Vector3f();
      this.scratchVBottom = new Vector3f();
      this.scratchP1 = new Vector3f();
      this.scratchP2 = new Vector3f();
      this.scratchP3 = new Vector3f();
      this.scratchColorVec = new Vector4i();

      for (int i = 0; i < 4; i++) {
         this.scratchTopVerts[i] = new Vector3f();
         this.scratchBottomVerts[i] = new Vector3f();
      }
   }

   public void onRenderWorldEvent(
      WorldRenderEvent event,
      LivingEntity target,
      float distance,
      float size,
      boolean glow,
      float glowSize,
      boolean horizontal,
      float anim,
      float red,
      int[] customColors
   ) {
      if (target != null) {
         if (this.crystals.isEmpty()) {
            this.createCrystals(target, distance, size);
         }

         this.updateAnimations(target);
         this.renderCrystals(event, target, anim, red, distance, size, glow, glowSize, horizontal, customColors);
      }
   }

   public void tickAndRenderExplosions(WorldRenderEvent event) {
      if (!this.activeExplosions.isEmpty()) {
         this.tickAllExplosions();
         this.renderAllExplosions(event);
      }
   }

   private void updateAnimations(LivingEntity target) {
      if (target != null) {
         long currentTick = this.mc.world != null ? this.mc.world.getTime() : 0L;
         if (this.lastUpdateTick != currentTick) {
            this.lastUpdateTick = currentTick;
            if (target.isAlive()) {
               this.crackAmount = 1.0F - target.getHealth() / target.getMaxHealth();
            } else if (this.crackAmount < 1.0F) {
               this.crackAmount += 0.05F;
            }

            this.crackAmount = MathHelper.clamp(this.crackAmount, 0.0F, 1.0F);
            if (target.hurtTime > this.lastHurtTime) {
               this.hitAnimation = 1.0F;
            }

            this.lastHurtTime = target.hurtTime;
            if (this.hitAnimation > 0.0F) {
               this.hitAnimation -= 0.04F;
            }

            this.hitAnimation = MathHelper.clamp(this.hitAnimation, 0.0F, 1.0F);
         }
      }
   }

   private void renderCrystals(
      WorldRenderEvent event,
      LivingEntity target,
      float anim,
      float red,
      float distance,
      float size,
      boolean glow,
      float glowSize,
      boolean horizontal,
      int[] customColors
   ) {
      if (!(anim <= 0.0F) && event != null) {
         if (RenderSystem.isOnRenderThread()) {
            RenderSystem.enableDepthTest();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            Vec3d targetPos = MathUtil.interpolate(target);
            boolean canSee = this.mc.player != null && this.mc.player.canSee(target);
            this.lastRenderCircleStep = TargetRenderer.getInterpolatedCircleStep();
            this.lastRenderEntityPos = targetPos;
            this.lastRenderTargetHeight = target.getHeight();
            this.lastRenderTargetWidth = target.getWidth();
            this.lastRenderDistance = distance;
            if (anim > 0.01F && !this.crystals.isEmpty()) {
               for (int i = 0; i < this.crystals.size(); i++) {
                  TargetESPCrystals.Crystal crystal = this.crystals.get(i);
                  this.renderCrystal(event, crystal, target, targetPos, canSee, i, anim, red, distance, size, glow, glowSize, horizontal, customColors);
               }
            }

            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         }
      }
   }

   private void createCrystals(LivingEntity target, float distance, float size) {
      this.crystals.clear();
      float targetHeight = target.getHeight();
      float targetWidth = target.getWidth();
      float topCrystalY = targetHeight * 0.6F;
      float bottomCrystalY = -targetHeight * 0.1F;
      this.crystals.add(new TargetESPCrystals.Crystal(new Vector3f(0.0F, topCrystalY, 0.0F), new Vector3f(0.0F, 0.0F, 0.0F), size));
      this.crystals.add(new TargetESPCrystals.Crystal(new Vector3f(0.0F, bottomCrystalY, 0.0F), new Vector3f(0.0F, 0.0F, 0.0F), size));

      for (int i = 0; i < 17; i++) {
         float angle = (float)(i * 2 * Math.PI / 17.0);
         float radius = distance * 2.0F * (targetWidth / 0.6F);
         float heightBase = targetHeight * 0.5F;
         float height = heightBase + (float)Math.sin(angle * 3.0F) * (targetHeight * 0.45F);
         Vector3f position = new Vector3f((float)(Math.cos(angle) * radius), height, (float)(Math.sin(angle) * radius));
         Vector3f rotation = new Vector3f((float)(Math.sin(angle) * 30.0), angle * 180.0F / (float) Math.PI, (float)(Math.cos(angle) * 30.0));
         this.crystals.add(new TargetESPCrystals.Crystal(position, rotation, size));
      }
   }

   private void renderCrystal(
      WorldRenderEvent event,
      TargetESPCrystals.Crystal crystal,
      LivingEntity target,
      Vec3d targetPos,
      boolean canSee,
      int crystalIndex,
      float anim,
      float red,
      float distance,
      float size,
      boolean glow,
      float glowSize,
      boolean horizontal,
      int[] customColors
   ) {
      if (event != null && crystal != null) {
         MatrixStack eventStack = event.getStack();
         MatrixStack matrixStack = this.scratchMatrixStack;
         matrixStack.loadIdentity();
         matrixStack.peek().getPositionMatrix().set(eventStack.peek().getPositionMatrix());
         matrixStack.peek().getPositionMatrix().setTranslation(0.0F, 0.0F, 0.0F);
         matrixStack.peek().getNormalMatrix().set(eventStack.peek().getNormalMatrix());
         Vec3d cameraPos = this.mc.getEntityRenderDispatcher().camera.getPos();
         Vec3d localTargetPos = targetPos.subtract(cameraPos);
         float scale = crystal.size * anim;
         if (this.hitAnimation > 0.0F) {
            scale *= 1.0F + this.hitAnimation * 0.5F;
         }

         if (this.crackAmount > 0.0F) {
            scale *= 1.0F - this.crackAmount * 0.3F;
         }

         float sharedCircleStep = TargetRenderer.getInterpolatedCircleStep();
         float pulseAmount = (float)Math.sin(sharedCircleStep * 2.0) * 0.1F;
         scale *= 1.0F + pulseAmount;
         matrixStack.push();
         float targetHeight = target.getHeight();
         float targetWidth = target.getWidth();
         double cs = sharedCircleStep * 3.0F * 0.5F;
         float angle = (float)(crystalIndex * 2 * Math.PI / this.crystals.size());
         float radius = this.getAnimatedDistance(anim, distance) * (targetWidth / 0.6F);
         float heightBase = targetHeight * 0.5F;
         float height = heightBase + (float)Math.sin(angle * 3.0F) * (targetHeight * 0.45F);
         float cos = (float)Math.cos(-cs);
         float sin = (float)Math.sin(-cs);
         float baseX = (float)(Math.cos(angle) * radius);
         float baseZ = (float)(Math.sin(angle) * radius);
         pulseAmount = baseX * cos - baseZ * sin;
         float finalZ = baseX * sin + baseZ * cos;
         matrixStack.translate(localTargetPos.x + pulseAmount, localTargetPos.y + height, localTargetPos.z + finalZ);
         float angleToPlayer = (float)Math.atan2(pulseAmount, finalZ);
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angleToPlayer * 180.0F / (float) Math.PI));
         if (!horizontal) {
            float distanceFromCenter = (float)Math.sqrt(pulseAmount * pulseAmount + finalZ * finalZ);
            angle = (float)Math.atan2(height, distanceFromCenter) * 180.0F / (float) Math.PI;
            radius = targetHeight * 0.5F;
            if (height > radius) {
               matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-angle - 45.0F));
            } else {
               matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-angle));
            }
         } else {
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
         }

         matrixStack.scale(size, size, size);
         int color = this.getColorByPosition(crystal.position, customColors, crystalIndex);
         if (red > 0.0F) {
            color = ColorUtil.gradientToRed(color, red);
         }

         if (anim < 1.0F) {
            angle = this.smoothStepEaseInOut(anim);
            int alpha = (int)(255.0F * angle);
            heightBase = 0.3F + 0.7F * angle;
            color = ColorUtil.multDark(color, heightBase);
            color = color & 16777215 | alpha << 24;
         }

         this.drawCrystal(matrixStack, color, 4, canSee, size, anim);
         if (glow) {
            Vec3d crystalWorldPos = new Vec3d(targetPos.x + pulseAmount, targetPos.y + height, targetPos.z + finalZ);
            this.drawCrystalGlow(matrixStack, color, event, crystalWorldPos, size, glowSize, 0.8F);
         }

         matrixStack.pop();
      }
   }

   private void drawCrystal(MatrixStack matrixStack, int color, int sides, boolean canSee, float crystalSize, float anim) {
      if (canSee) {
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
      } else {
         RenderSystem.disableDepthTest();
      }

      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      float h_pyramid = crystalSize * 1.2F;
      float base_radius = crystalSize * 0.8F;

      for (int i = 0; i < sides; i++) {
         float angle = (float)((Math.PI * 2) * i / sides);
         float x = (float)(base_radius * Math.cos(angle));
         float z = (float)(base_radius * Math.sin(angle));
         this.scratchTopVerts[i].set(x, 0.0F, z);
         this.scratchBottomVerts[i].set(x, 0.0F, z);
      }

      this.scratchVTop.set(0.0F, h_pyramid, 0.0F);
      this.scratchVBottom.set(0.0F, -h_pyramid, 0.0F);

      for (int i = 0; i < sides; i++) {
         Vector3f v1 = this.scratchTopVerts[i];
         Vector3f v2 = this.scratchTopVerts[(i + 1) % sides];
         float gradientFactor = (float)i / sides;
         int topColor = this.getFaceColor(color, gradientFactor, 0);
         this.drawTriangle(bufferBuilder, matrixStack, this.scratchVTop, v1, v2, topColor, anim);
         Vector3f v3 = this.scratchBottomVerts[i];
         Vector3f v4 = this.scratchBottomVerts[(i + 1) % sides];
         int bottomColor = this.getFaceColor(color, gradientFactor, 1);
         this.drawTriangle(bufferBuilder, matrixStack, this.scratchVBottom, v4, v3, bottomColor, anim);
      }

      for (int i = 0; i < sides; i++) {
         Vector3f v1 = this.scratchTopVerts[i];
         Vector3f v2 = this.scratchTopVerts[(i + 1) % sides];
         Vector3f v3 = this.scratchBottomVerts[i];
         Vector3f v4 = this.scratchBottomVerts[(i + 1) % sides];
         float sideGradient = (float)i / sides;
         int sideColor = this.getFaceColor(color, sideGradient, 2);
         this.drawTriangle(bufferBuilder, matrixStack, v1, v2, v3, sideColor, anim);
         this.drawTriangle(bufferBuilder, matrixStack, v2, v4, v3, sideColor, anim);
      }

      try {
         BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      } catch (Exception var20) {
      }

      RenderSystem.enableCull();
      if (canSee) {
         RenderSystem.disableDepthTest();
      } else {
         RenderSystem.enableDepthTest();
      }

      RenderSystem.depthMask(true);
   }

   private float smoothStepEaseInOut(float t) {
      return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
   }

   private float getAnimatedDistance(float anim, float crystalDistance) {
      float maxDistance = crystalDistance * 2.0F;
      float smoothAnim = this.smoothStepEaseInOut(1.0F - anim);
      return crystalDistance + (maxDistance - crystalDistance) * smoothAnim;
   }

   private int getColorByPosition(Vector3f position, int[] customColors, int crystalIndex) {
      if (customColors != null && customColors.length != 0) {
         float sharedCircleStep = TargetRenderer.getInterpolatedCircleStep();
         float anglePos = (float)crystalIndex / this.crystals.size();
         float flowSpeed = 0.3F;
         float flowOffset = sharedCircleStep * flowSpeed % 1.0F;
         float gradientPos = (anglePos + flowOffset) % 1.0F;
         if (customColors.length == 1) {
            return customColors[0];
         } else {
            float scaledPos = gradientPos * customColors.length;
            int colorIndex1 = (int)scaledPos % customColors.length;
            int colorIndex2 = (colorIndex1 + 1) % customColors.length;
            float blend = scaledPos - (int)scaledPos;
            blend = (float)(Math.sin((blend - 0.5F) * Math.PI) * 0.5 + 0.5);
            return this.lerpColor(customColors[colorIndex1], customColors[colorIndex2], blend);
         }
      } else {
         return ColorUtil.getClientColor();
      }
   }

   private int lerpColor(int color1, int color2, float t) {
      int a1 = color1 >> 24 & 0xFF;
      int r1 = color1 >> 16 & 0xFF;
      int g1 = color1 >> 8 & 0xFF;
      int b1 = color1 & 0xFF;
      int a2 = color2 >> 24 & 0xFF;
      int r2 = color2 >> 16 & 0xFF;
      int g2 = color2 >> 8 & 0xFF;
      int b2 = color2 & 0xFF;
      int a = (int)(a1 + (a2 - a1) * t);
      int r = (int)(r1 + (r2 - r1) * t);
      int g = (int)(g1 + (g2 - g1) * t);
      int b = (int)(b1 + (b2 - b1) * t);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private int getFaceColor(int baseColor, float gradientFactor, int faceType) {
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
      r = (int)(r * brightness);
      g = (int)(g * brightness);
      b = (int)(b * brightness);
      r = Math.max(0, Math.min(255, r));
      g = Math.max(0, Math.min(255, g));
      b = Math.max(0, Math.min(255, b));
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private void drawCrystalGlow(
      MatrixStack matrixStack, int color, WorldRenderEvent event, Vec3d crystalWorldPos, float crystalSize, float glowSize, float transparency
   ) {
      Camera camera = this.mc.getEntityRenderDispatcher().camera;
      Vec3d vec = crystalWorldPos.subtract(camera.getPos());
      MatrixStack billboardStack = this.scratchBillboardStack;
      billboardStack.loadIdentity();
      billboardStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      billboardStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      billboardStack.translate(vec.x, vec.y, vec.z);
      billboardStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      billboardStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      Entry entry = billboardStack.peek().copy();
      float size = crystalSize * glowSize;
      int glowColor = ColorUtil.multAlpha(color, transparency);
      Vector4i colorVec = this.scratchColorVec.set(glowColor, glowColor, glowColor, glowColor);
      Identifier glowTexture = Identifier.of("textures/particles/bloom/bloom_soft.png");
      Render3DUtil.drawTexture(entry, glowTexture, -size / 2.0F, -size / 2.0F, size, size, colorVec, true);
   }

   private void drawTriangle(BufferBuilder bufferBuilder, MatrixStack matrixStack, Vector3f v1, Vector3f v2, Vector3f v3, int color, float anim) {
      Entry entry = matrixStack.peek();
      entry.getPositionMatrix().transformPosition(v1.x, v1.y, v1.z, this.scratchP1);
      entry.getPositionMatrix().transformPosition(v2.x, v2.y, v2.z, this.scratchP2);
      entry.getPositionMatrix().transformPosition(v3.x, v3.y, v3.z, this.scratchP3);
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      bufferBuilder.vertex(this.scratchP1.x, this.scratchP1.y, this.scratchP1.z).color(r, g, b, anim);
      bufferBuilder.vertex(this.scratchP2.x, this.scratchP2.y, this.scratchP2.z).color(r, g, b, anim);
      bufferBuilder.vertex(this.scratchP3.x, this.scratchP3.y, this.scratchP3.z).color(r, g, b, anim);
   }

   public boolean hasActiveExplosions() {
      return !this.activeExplosions.isEmpty();
   }

   public int getActiveExplosionCount() {
      return this.activeExplosions.size();
   }

   public boolean isCrystalsEmpty() {
      return this.crystals.isEmpty();
   }

   public boolean hasLastRenderPos() {
      return this.lastRenderEntityPos != null;
   }

   public void startExplosion(
      float speedModifier, float red, boolean horizontal, int[] customColors, float size, boolean glow, float glowSize, long lifetimeMs, float extraSpeed
   ) {
      if (!this.crystals.isEmpty() && this.lastRenderEntityPos != null) {
         long now = System.currentTimeMillis();
         Vec3d entityPos = this.lastRenderEntityPos;
         float circleStep = this.lastRenderCircleStep;
         float targetHeight = this.lastRenderTargetHeight;
         float targetWidth = this.lastRenderTargetWidth;
         float distance = this.lastRenderDistance;
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         float orbitAngVel = 0.15F * speedModifier * 3.0F * 0.5F;
         float orbitSpinDeg = (float)Math.toDegrees(orbitAngVel);
         List<TargetESPCrystals.ExplCrystal> explCrystals = new ArrayList<>();

         for (int i = 0; i < this.crystals.size(); i++) {
            TargetESPCrystals.Crystal crystal = this.crystals.get(i);
            TargetESPCrystals.ExplCrystal ec = new TargetESPCrystals.ExplCrystal();
            double cs = circleStep * 3.0F * 0.5;
            float angle = (float)(i * 2 * Math.PI / this.crystals.size());
            float radius = this.getAnimatedDistance(1.0F, distance) * (targetWidth / 0.6F);
            float heightBase = targetHeight * 0.5F;
            float height = heightBase + (float)Math.sin(angle * 3.0F) * (targetHeight * 0.45F);
            float cos = (float)Math.cos(-cs);
            float sin = (float)Math.sin(-cs);
            float baseX = (float)(Math.cos(angle) * radius);
            float baseZ = (float)(Math.sin(angle) * radius);
            float finalX = baseX * cos - baseZ * sin;
            float finalZ = baseX * sin + baseZ * cos;
            ec.posX = (float)entityPos.x + finalX;
            ec.posY = (float)entityPos.y + height;
            ec.posZ = (float)entityPos.z + finalZ;
            ec.prevPosX = ec.posX;
            ec.prevPosY = ec.posY;
            ec.prevPosZ = ec.posZ;
            ec.orbitCenterX = (float)entityPos.x;
            ec.orbitCenterZ = (float)entityPos.z;
            ec.orbitAngle = (float)Math.atan2(finalX, finalZ);
            ec.orbitRadius = (float)Math.sqrt(finalX * finalX + finalZ * finalZ);
            ec.orbitAngVel = orbitAngVel;
            float forceMult = 0.7F + rng.nextFloat() * 0.6F;
            ec.radiusSpeed = 0.04F * forceMult * extraSpeed;
            float relY = height - targetHeight * 0.5F;
            double dist3D = Math.sqrt(finalX * finalX + relY * relY + finalZ * finalZ);
            if (dist3D < 0.01) {
               dist3D = 1.0;
            }

            ec.velY = (float)(relY / dist3D * 0.04F * forceMult * extraSpeed) + rng.nextFloat() * 0.01F;
            heightBase = (float)Math.toDegrees(Math.atan2(finalX, finalZ));
            height = (float)Math.sqrt(finalX * finalX + finalZ * finalZ);
            if (!horizontal) {
               cos = (float)Math.toDegrees(Math.atan2(height, height));
               sin = targetHeight * 0.5F;
               ec.rotX = height > sin ? -cos - 45.0F : -cos;
            } else {
               ec.rotX = 90.0F;
            }

            ec.rotY = heightBase;
            ec.rotZ = 0.0F;
            ec.prevRotX = ec.rotX;
            ec.prevRotY = ec.rotY;
            ec.prevRotZ = ec.rotZ;
            ec.spinX = (0.5F + rng.nextFloat() * 0.5F) * 18.0F * (rng.nextBoolean() ? 1 : -1);
            ec.spinY = orbitSpinDeg + (rng.nextFloat() - 0.5F) * 3.0F;
            ec.spinZ = (0.5F + rng.nextFloat() * 0.5F) * 18.0F * (rng.nextBoolean() ? 1 : -1);
            int color = this.getColorByPosition(crystal.position, customColors, i);
            if (red > 0.0F) {
               color = ColorUtil.gradientToRed(color, red);
            }

            ec.color = color;
            explCrystals.add(ec);
         }

         this.activeExplosions.add(new TargetESPCrystals.ActiveExplosion(now, explCrystals, size, glow, glowSize, lifetimeMs));
      }
   }

   private void tickAllExplosions() {
      long now = System.currentTimeMillis();
      this.activeExplosions.removeIf(e -> now - e.startTime > e.lifetimeMs + 200L);

      for (TargetESPCrystals.ActiveExplosion explosion : this.activeExplosions) {
         long currentTick = this.mc.world != null ? this.mc.world.getTime() : 0L;
         if (explosion.lastTickedTick != currentTick) {
            explosion.lastTickedTick = currentTick;
            float progress = Math.min((float)(now - explosion.startTime) / (float)explosion.lifetimeMs, 1.0F);
            float eased = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
            float spinFactor = 1.0F - eased * 0.6F;

            for (TargetESPCrystals.ExplCrystal c : explosion.crystals) {
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

   private void renderAllExplosions(WorldRenderEvent event) {
      if (RenderSystem.isOnRenderThread()) {
         long now = System.currentTimeMillis();
         MatrixStack eventStack = event.getStack();
         Vec3d cameraPos = this.mc.getEntityRenderDispatcher().camera.getPos();
         float tickDelta = this.mc.getRenderTickCounter().getTickDelta(false);
         RenderSystem.enableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);

         for (TargetESPCrystals.ActiveExplosion explosion : this.activeExplosions) {
            float age = (float)(now - explosion.startTime) / (float)explosion.lifetimeMs;
            float life = Math.max(0.0F, 1.0F - age);
            float smoothAlpha = this.smoothStepEaseInOut(life);
            float brightness = 0.3F + 0.7F * smoothAlpha;
            float scaleMult = 0.8F + 0.2F * life;
            float size = explosion.size;
            boolean glow = explosion.glow;
            float glowSize = explosion.glowSize;

            for (TargetESPCrystals.ExplCrystal c : explosion.crystals) {
               if (!(smoothAlpha < 0.01F)) {
                  float lerpX = MathHelper.lerp(tickDelta, c.prevPosX, c.posX);
                  float lerpY = MathHelper.lerp(tickDelta, c.prevPosY, c.posY);
                  float lerpZ = MathHelper.lerp(tickDelta, c.prevPosZ, c.posZ);
                  float lerpRotX = MathHelper.lerp(tickDelta, c.prevRotX, c.rotX);
                  float lerpRotY = MathHelper.lerp(tickDelta, c.prevRotY, c.rotY);
                  float lerpRotZ = MathHelper.lerp(tickDelta, c.prevRotZ, c.rotZ);
                  MatrixStack matrixStack = this.scratchMatrixStack;
                  matrixStack.loadIdentity();
                  matrixStack.peek().getPositionMatrix().set(eventStack.peek().getPositionMatrix());
                  matrixStack.peek().getPositionMatrix().setTranslation(0.0F, 0.0F, 0.0F);
                  matrixStack.peek().getNormalMatrix().set(eventStack.peek().getNormalMatrix());
                  matrixStack.push();
                  matrixStack.translate(lerpX - cameraPos.x, lerpY - cameraPos.y, lerpZ - cameraPos.z);
                  matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(lerpRotY));
                  matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lerpRotX));
                  matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(lerpRotZ));
                  float crystalSize = size * scaleMult;
                  matrixStack.scale(crystalSize, crystalSize, crystalSize);
                  int color = ColorUtil.multDark(c.color, brightness);
                  int alphaInt = (int)(255.0F * smoothAlpha);
                  color = color & 16777215 | alphaInt << 24;
                  this.drawCrystal(matrixStack, color, 4, true, size, smoothAlpha);
                  if (glow && smoothAlpha > 0.1F) {
                     Vec3d crystalWorldPos = new Vec3d(lerpX, lerpY, lerpZ);
                     this.drawCrystalGlow(matrixStack, color, event, crystalWorldPos, crystalSize, glowSize, smoothAlpha * 0.6F);
                  }

                  matrixStack.pop();
               }
            }
         }

         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
      }
   }

   public List<TargetESPCrystals.CrystalSnapshot> captureState(
      LivingEntity target, float distance, float size, float anim, boolean horizontal, int[] customColors, float red, float speedModifier
   ) {
      if (!this.crystals.isEmpty() && target != null) {
         Vec3d targetPos = this.lastRenderEntityPos != null ? this.lastRenderEntityPos : MathUtil.interpolate(target);
         float sharedCircleStep = this.lastRenderEntityPos != null ? this.lastRenderCircleStep : TargetRenderer.getInterpolatedCircleStep();
         float targetHeight = target.getHeight();
         float targetWidth = target.getWidth();
         float orbitRadPerTick = 0.15F * speedModifier * 3.0F * 0.5F;
         float orbitDegPerTick = (float)Math.toDegrees(orbitRadPerTick);
         List<TargetESPCrystals.CrystalSnapshot> snapshots = new ArrayList<>();

         for (int i = 0; i < this.crystals.size(); i++) {
            TargetESPCrystals.Crystal crystal = this.crystals.get(i);
            float crystalOrbitSpinY = 0.0F;
            double cs = sharedCircleStep * 3.0F * 0.5F;
            float angle = (float)(i * 2 * Math.PI / this.crystals.size());
            float radius = this.getAnimatedDistance(anim, distance) * (targetWidth / 0.6F);
            float heightBase = targetHeight * 0.5F;
            float height = heightBase + (float)Math.sin(angle * 3.0F) * (targetHeight * 0.45F);
            float cos = (float)Math.cos(-cs);
            float sin = (float)Math.sin(-cs);
            float baseX = (float)(Math.cos(angle) * radius);
            float baseZ = (float)(Math.sin(angle) * radius);
            float finalX = baseX * cos - baseZ * sin;
            float finalZ = baseX * sin + baseZ * cos;
            Vec3d worldPos = new Vec3d(targetPos.x + finalX, targetPos.y + height, targetPos.z + finalZ);
            float yaw = (float)Math.toDegrees(Math.atan2(finalX, finalZ));
            if (!horizontal) {
               radius = (float)Math.sqrt(finalX * finalX + finalZ * finalZ);
               heightBase = (float)Math.toDegrees(Math.atan2(height, radius));
               float heightThreshold = targetHeight * 0.5F;
               angle = height > heightThreshold ? -heightBase - 45.0F : -heightBase;
            } else {
               angle = 90.0F;
            }

            int color = this.getColorByPosition(crystal.position, customColors, i);
            if (red > 0.0F) {
               color = ColorUtil.gradientToRed(color, red);
            }

            if (anim < 1.0F) {
               heightBase = this.smoothStepEaseInOut(anim);
               height = 0.3F + 0.7F * heightBase;
               int var36 = ColorUtil.multDark(color, height);
               int alpha = (int)(255.0F * heightBase);
               color = var36 & 16777215 | alpha << 24;
            }

            snapshots.add(new TargetESPCrystals.CrystalSnapshot(worldPos, size, color, yaw, angle, orbitDegPerTick));
         }

         return snapshots;
      } else {
         return null;
      }
   }

   public void reset() {
      this.crackAmount = 0.0F;
      this.hitAnimation = 0.0F;
      this.lastHurtTime = 0.0F;
      this.crystals.clear();
   }

   private static class ActiveExplosion {
      final long startTime;
      final long lifetimeMs;
      final List<TargetESPCrystals.ExplCrystal> crystals;
      final float size;
      final boolean glow;
      final float glowSize;
      long lastTickedTick = -1L;

      ActiveExplosion(long startTime, List<TargetESPCrystals.ExplCrystal> crystals, float size, boolean glow, float glowSize, long lifetimeMs) {
         this.startTime = startTime;
         this.crystals = crystals;
         this.size = size;
         this.glow = glow;
         this.glowSize = glowSize;
         this.lifetimeMs = lifetimeMs;
      }
   }

   private static class Crystal {
      final Vector3f position;
      final Vector3f rotation;
      final float size;

      Crystal(Vector3f position, Vector3f rotation, float size) {
         this.position = position;
         this.rotation = rotation;
         this.size = size;
      }
   }

   public record CrystalSnapshot(Vec3d worldPos, float size, int color, float yaw, float pitch, float orbitSpinY) {
   }

   private static class ExplCrystal {
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
      int color;
   }
}
