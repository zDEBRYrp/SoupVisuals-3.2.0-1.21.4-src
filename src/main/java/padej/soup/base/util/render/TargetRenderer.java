package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;
import padej.soup.api.event.events.render.WorldRenderEvent;
import padej.soup.api.feature.module.ITargetRenderModule;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.features.modules.particles.render.ParticleBatchRenderer;

public final class TargetRenderer implements QuickImports {
   private static final MatrixStack scratchGhostStack = new MatrixStack();
   private static float espValue = 1.0F;
   private static float espSpeed = 1.0F;
   private static float prevEspValue;
   private static float circleStep;
   private static float prevCircleStep;
   private static boolean flipSpeed;
   private static long lastUpdateTick = -1L;
   private static Vec3d lastGhostEntityPos;
   private static double lastGhostInterpolate;
   private static float lastGhostHalfHeight;
   private static float lastGhostWidth;
   private static Vec3d lastCircleEntityPos;
   private static float lastCircleWidth;
   private static float lastCircleHeight;
   private static double lastCircleCircleStep;
   public static Color topLeft = null;
   public static Color topRight = null;
   public static Color bottomRight = null;
   public static Color bottomLeft = null;

   private static int getGradientColor(int index, float red, ITargetRenderModule module) {
      int[] customColors = module.getCustomColors();
      if (customColors != null && customColors.length > 0) {
         int colorIndex = index % customColors.length;
         return ColorUtil.gradientToRed(customColors[colorIndex], red);
      } else {
         return ColorUtil.gradientToRed(ColorUtil.fade(index * 4), red);
      }
   }

   private static int interpolateColor(int color1, int color2, float factor) {
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

   private static int getGhostGradientColor(float ghostPosition, float red, ITargetRenderModule module) {
      int[] customColors = module.getCustomColors();
      if (customColors != null && customColors.length > 0) {
         if (customColors.length == 1) {
            return ColorUtil.gradientToRed(customColors[0], red);
         } else {
            float normalizedPosition = Math.max(0.0F, Math.min(1.0F, ghostPosition));
            float totalSegments = customColors.length - 1;
            float scaledPosition = normalizedPosition * totalSegments;
            int segmentIndex = Math.min((int)Math.floor(scaledPosition), customColors.length - 2);
            float localPosition = scaledPosition - segmentIndex;
            int color1 = customColors[segmentIndex];
            int color2 = customColors[segmentIndex + 1];
            int interpolated = interpolateColor(color1, color2, localPosition);
            return ColorUtil.gradientToRed(interpolated, red);
         }
      } else {
         float colorIndex = ghostPosition * 180.0F;
         return ColorUtil.gradientToRed(ColorUtil.fade((int)colorIndex), red);
      }
   }

   private static int getCircleGradientColor(int segmentIndex, int totalSegments, float red, ITargetRenderModule module) {
      int[] customColors = module.getCustomColors();
      if (customColors != null && customColors.length > 0) {
         float degreesPerColor = 360.0F / customColors.length;
         float currentDegree = segmentIndex * 360.0F / totalSegments;
         int colorIndex1 = (int)(currentDegree / degreesPerColor) % customColors.length;
         int colorIndex2 = (colorIndex1 + 1) % customColors.length;
         float factor = currentDegree % degreesPerColor / degreesPerColor;
         int color1 = customColors[colorIndex1];
         int color2 = customColors[colorIndex2];
         int interpolated = interpolateColor(color1, color2, factor);
         return ColorUtil.gradientToRed(interpolated, red);
      } else {
         return ColorUtil.gradientToRed(ColorUtil.fade(segmentIndex * 4), red);
      }
   }

   public static void drawLegacy(LivingEntity target, float animation, float red, ITargetRenderModule module) {
      float size = (2.2F - animation) * module.getLegacySize();
      Camera camera = mc.getEntityRenderDispatcher().camera;
      Vec3d targetPos;
      if (module.isOptimalAim()) {
         Vec3d closestPoint = MathUtil.closestPointToEntity(target);
         targetPos = closestPoint.subtract(camera.getPos());
      } else {
         targetPos = MathUtil.interpolate(target).subtract(camera.getPos());
      }

      MatrixStack matrix = new MatrixStack();
      matrix.push();
      matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      if (module.isOptimalAim()) {
         matrix.translate(targetPos.x, targetPos.y, targetPos.z);
      } else {
         matrix.translate(targetPos.x, targetPos.y + target.getBoundingBox().getLengthY() / 2.0, targetPos.z);
      }

      matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      if (!module.isStaticMode()) {
         matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathUtil.interpolate(prevEspValue, espValue)));
      } else {
         matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
      }

      Entry entry = matrix.peek().copy();
      Render3DUtil.drawTexture(
         entry,
         module.getLegacyTexture(),
         -size / 2.0F,
         -size / 2.0F,
         size,
         size,
         new Vector4i(
            ColorUtil.multAlpha(getGradientColor(22, red, module), animation),
            ColorUtil.multAlpha(getGradientColor(0, red, module), animation),
            ColorUtil.multAlpha(getGradientColor(45, red, module), animation),
            ColorUtil.multAlpha(getGradientColor(67, red, module), animation)
         ),
         !Objects.requireNonNull(mc.player).canSee(target)
      );
      matrix.pop();
   }

   public static void drawCircle(MatrixStack matrix, LivingEntity target, float animation, float red, ITargetRenderModule module) {
      double cs = MathUtil.interpolate(prevCircleStep, circleStep);
      Vec3d targetPos = MathUtil.interpolate(target);
      lastCircleEntityPos = targetPos;
      lastCircleWidth = target.getWidth();
      lastCircleHeight = target.getHeight();
      lastCircleCircleStep = cs;
      GL11.glEnable(2881);
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      float w = target.getWidth() * 0.9F;
      float height = target.getHeight();
      double yAnim = MathUtil.absSinAnimation(cs) * height;
      double yAnim2 = MathUtil.absSinAnimation(cs - 0.45) * height;
      int size = 90;
      double pi2overSize = MathUtil.PI2 / size;

      for (int i = 0; i <= size; i++) {
         int idx = Math.min(i, size);
         float cosI = (float)(Math.cos(idx * pi2overSize) * w);
         float sinI = (float)(-Math.sin(idx * pi2overSize) * w);
         int nextIdx = Math.min(i + 1, size);
         float cosN = (float)(Math.cos(nextIdx * pi2overSize) * w);
         float sinN = (float)(-Math.sin(nextIdx * pi2overSize) * w);
         int color = getCircleGradientColor(i, size, red, module);
         Render3DUtil.vertexLine(
            matrix,
            buffer,
            targetPos.add(cosI, yAnim, sinI),
            targetPos.add(cosI, yAnim2, sinI),
            ColorUtil.multAlpha(color, 0.6F * animation),
            ColorUtil.multAlpha(color, 0.0F)
         );
         Render3DUtil.drawLine(targetPos.add(cosI, yAnim, sinI), targetPos.add(cosN, yAnim, sinN), ColorUtil.multAlpha(color, animation), 3.0F, true);
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.depthMask(true);
      RenderSystem.disableDepthTest();
      GL11.glDisable(2881);
   }

   public static void drawGhosts(
      LivingEntity target,
      float animation,
      float red,
      float speed,
      float length,
      float radiusModifier,
      float headSize,
      float tailSize,
      float subdivisions,
      ITargetRenderModule module
   ) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      Vec3d entityPos = MathUtil.interpolate(target);
      Vec3d vec = entityPos.subtract(camera.getPos());
      double interpolate = MathUtil.interpolate((float)(mc.player.age - 1), (float)mc.player.age);
      float halfHeight = target.getHeight() / 2.0F + 0.1F;
      float width = target.getWidth();
      lastGhostEntityPos = entityPos;
      lastGhostInterpolate = interpolate;
      lastGhostHalfHeight = halfHeight;
      lastGhostWidth = width;
      boolean isSpiral = module.getGhostsTrajectory().isSelected("Spiral");
      boolean isDefault = module.getGhostsTrajectory().isSelected("Standard");
      int ghostsCount = (int)module.getGhostsCount().getValue();
      int baseGhostLength = Math.round(10.0F * length);
      int subdivisionsInt = Math.round(subdivisions);
      double phaseShiftStep = Math.toRadians(360.0 / ghostsCount);

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

         for (int i = 0; i < baseGhostLength; i++) {
            for (int sub = 0; sub < subdivisionsInt; sub++) {
               float t = (float)sub / subdivisionsInt;
               float currentStep = i + t;
               float ghostPosition = 1.0F - currentStep / baseGhostLength;
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

               MatrixStack matrices = scratchGhostStack;
               matrices.loadIdentity();
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
               if (isSpiral) {
                  matrices.translate(
                     vec.x + Math.cos(radians) * width * radiusModifier, vec.y + halfHeight + sinQuad, vec.z + Math.sin(radians) * width * radiusModifier
                  );
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

                  matrices.translate(vec.x + offsetX, vec.y + halfHeight + offsetY, vec.z + offsetZ);
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
                  double var89 = tempX * cosZ - tempY * sinZ;
                  tempY = tempX * sinZ + tempY * cosZ;
                  matrices.translate(vec.x + var89, vec.y + halfHeight + tempY, vec.z + tempZ);
               }

               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
               float trailGradient = currentStep / baseGhostLength;
               int color = ColorUtil.multAlpha(getGhostGradientColor(ghostPosition, red, module), trailGradient * animation);
               float baseScale = headSize + (tailSize - headSize) * ghostPosition;
               float scale = baseScale * animation;
               ParticleBatchRenderer.queueGhost(
                  matrices.peek().getPositionMatrix(), module.getGhostTexture(), scale, color, module.getGhostsBlend().getSelected()
               );
            }
         }
      }
   }

   public static void updateAnimations(float speedModifier) {
      long currentTick = mc.world != null ? mc.world.getTime() : 0L;
      if (lastUpdateTick != currentTick) {
         lastUpdateTick = currentTick;
         prevEspValue = espValue;
         espValue = espValue + espSpeed * speedModifier;
         if (espSpeed > 25.0F) {
            flipSpeed = true;
         }

         if (espSpeed < -25.0F) {
            flipSpeed = false;
         }

         espSpeed = flipSpeed ? espSpeed - 0.5F * speedModifier : espSpeed + 0.5F * speedModifier;
         prevCircleStep = circleStep;
         circleStep += 0.15F * speedModifier;
      }
   }

   public static float getInterpolatedCircleStep() {
      return MathUtil.interpolate(prevCircleStep, circleStep);
   }

   public static Vec3d getLastGhostEntityPos() {
      return lastGhostEntityPos;
   }

   public static double getLastGhostInterpolate() {
      return lastGhostInterpolate;
   }

   public static float getLastGhostHalfHeight() {
      return lastGhostHalfHeight;
   }

   public static float getLastGhostWidth() {
      return lastGhostWidth;
   }

   public static boolean hasLastGhostRenderPos() {
      return lastGhostEntityPos != null;
   }

   public static Vec3d getLastCircleEntityPos() {
      return lastCircleEntityPos;
   }

   public static float getLastCircleWidth() {
      return lastCircleWidth;
   }

   public static float getLastCircleHeight() {
      return lastCircleHeight;
   }

   public static double getLastCircleCircleStep() {
      return lastCircleCircleStep;
   }

   public static boolean hasLastCircleRenderPos() {
      return lastCircleEntityPos != null;
   }

   public static void drawCrystals(WorldRenderEvent event, LivingEntity target, float animation, float red, ITargetRenderModule module) {
      TargetESPCrystals.instance
         .onRenderWorldEvent(
            event,
            target,
            module.getCrystalsDistance(),
            module.getCrystalsSize(),
            module.isCrystalsGlow(),
            module.getCrystalsGlowSize(),
            module.isCrystalsHorizontal(),
            animation,
            red,
            module.getCustomColors()
         );
   }

   private TargetRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
