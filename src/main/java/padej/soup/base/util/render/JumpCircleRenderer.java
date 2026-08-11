package padej.soup.base.util.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4i;
import padej.soup.base.QuickImports;
import padej.soup.base.util.animation.ThreeStageAnimation;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.other.StopWatch;
import padej.soup.implement.features.modules.visuals.JumpCircles;

public final class JumpCircleRenderer implements QuickImports {
   private static final MatrixStack SCRATCH_MATRIX = new MatrixStack();
   private static final Vector4i SCRATCH_COLORS = new Vector4i();

   public static void renderCircle(Vec3d pos, StopWatch timer, float rotationAngle, JumpCircles module, ThreeStageAnimation animation) {
      if (animation != null) {
         double elapsedSeconds = timer.elapsedTime() / 1000.0;
         if (!animation.isFinished(elapsedSeconds)) {
            ThreeStageAnimation.AnimationStage stage = animation.getStage(elapsedSeconds);
            double animationValue = animation.getValue(elapsedSeconds);
            boolean isOverlapping = animation.getExistDuration() < 0.0;
            double overlapStart = animation.getAppearDuration() + animation.getExistDuration();
            boolean inOverlapZone = isOverlapping && elapsedSeconds > overlapStart && elapsedSeconds <= animation.getAppearDuration();
            float alpha;
            float scale;
            if (inOverlapZone) {
               alpha = calculateOverlappingAlpha(animationValue, elapsedSeconds, animation, module);
               scale = calculateOverlappingScale(animationValue, elapsedSeconds, animation, module);
            } else {
               alpha = calculateAlpha(animationValue, stage, module);
               scale = calculateScale(animationValue, stage, module);
            }

            Vector4i colors = getCircleColors(alpha, module);
            MatrixStack matrix = SCRATCH_MATRIX;
            matrix.loadIdentity();
            matrix.push();
            Camera cam = mc.getEntityRenderDispatcher().camera;
            Vec3d targetPos = pos.subtract(cam.getPos());
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180.0F));
            matrix.translate(targetPos.x, targetPos.y + 0.01, targetPos.z);
            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationAngle));
            Entry entry = matrix.peek().copy();
            float size = 2.0F * scale;
            Render3DUtil.drawTexture(entry, module.getCircleTexture(), -size / 2.0F, -size / 2.0F, size, size, colors, true);
            matrix.pop();
         }
      }
   }

   private static Vector4i getCircleColors(float alpha, JumpCircles module) {
      if (module.getColorMode().isSelected("Custom")) {
         int[] colors = module.getCustomColors();
         if (colors != null && colors.length > 0) {
            String animType = module.colorAnimation.getSelected();
            if ("Wave".equals(animType)) {
               int color = getWaveColor(colors, alpha);
               return SCRATCH_COLORS.set(color, color, color, color);
            }

            if ("Vertexes".equals(animType)) {
               return getVertexColors(colors, alpha);
            }
         }
      }

      int playerColor = ColorUtil.multAlpha(ColorUtil.getClientColor(), alpha);
      return SCRATCH_COLORS.set(playerColor, playerColor, playerColor, playerColor);
   }

   private static int getWaveColor(int[] colors, float alpha) {
      if (colors.length == 1) {
         return ColorUtil.multAlpha(colors[0], alpha);
      } else if (colors.length == 2) {
         int angle = (int)(System.currentTimeMillis() / 8L % 360L);
         angle = angle >= 180 ? 360 - angle : angle;
         return ColorUtil.multAlpha(ColorUtil.overCol(colors[0], colors[1], angle / 180.0F), alpha);
      } else {
         float timeProgress = (float)(System.currentTimeMillis() / 10L % (colors.length * 360L)) / 360.0F;
         int index1 = (int)Math.floor(timeProgress) % colors.length;
         int index2 = (index1 + 1) % colors.length;
         float lerp = timeProgress - (int)Math.floor(timeProgress);
         return ColorUtil.multAlpha(ColorUtil.overCol(colors[index1], colors[index2], lerp), alpha);
      }
   }

   private static Vector4i getVertexColors(int[] colors, float alpha) {
      if (colors.length == 1) {
         int color = ColorUtil.multAlpha(colors[0], alpha);
         return SCRATCH_COLORS.set(color, color, color, color);
      } else {
         int color1 = getVertexGradientColor(0, colors, alpha);
         int color2 = getVertexGradientColor(90, colors, alpha);
         int color3 = getVertexGradientColor(180, colors, alpha);
         int color4 = getVertexGradientColor(270, colors, alpha);
         return SCRATCH_COLORS.set(color1, color2, color3, color4);
      }
   }

   private static int getVertexGradientColor(int angleOffset, int[] colors, float alpha) {
      if (colors.length == 1) {
         return ColorUtil.multAlpha(colors[0], alpha);
      } else {
         int angle = (int)((System.currentTimeMillis() / 8L + angleOffset) % 360L);
         if (colors.length == 2) {
            angle = angle >= 180 ? 360 - angle : angle;
            return ColorUtil.multAlpha(ColorUtil.overCol(colors[0], colors[1], angle / 180.0F), alpha);
         } else {
            float progress = angle / 360.0F;
            float colorIndex = progress * colors.length;
            int index1 = (int)colorIndex % colors.length;
            int index2 = (index1 + 1) % colors.length;
            float lerp = colorIndex - (int)colorIndex;
            return ColorUtil.multAlpha(ColorUtil.overCol(colors[index1], colors[index2], lerp), alpha);
         }
      }
   }

   private static float calculateAlpha(double animationValue, ThreeStageAnimation.AnimationStage stage, JumpCircles module) {
      String animType;
      if (stage == ThreeStageAnimation.AnimationStage.APPEAR) {
         animType = module.appearAnimationType.getSelected();
      } else {
         if (stage != ThreeStageAnimation.AnimationStage.DISAPPEAR) {
            return 1.0F;
         }

         animType = module.disappearAnimationType.getSelected();
      }

      return "Scale".equals(animType) ? 1.0F : (float)Math.max(0.0, Math.min(1.0, animationValue));
   }

   private static float calculateScale(double animationValue, ThreeStageAnimation.AnimationStage stage, JumpCircles module) {
      String animType;
      if (stage == ThreeStageAnimation.AnimationStage.APPEAR) {
         animType = module.appearAnimationType.getSelected();
      } else {
         if (stage != ThreeStageAnimation.AnimationStage.DISAPPEAR) {
            return module.circleScale.getValue();
         }

         animType = module.disappearAnimationType.getSelected();
      }

      if ("Fade".equals(animType)) {
         return module.circleScale.getValue();
      } else {
         float scaleFactor = (float)Math.max(0.0, Math.min(1.0, animationValue));
         return module.circleScale.getValue() * scaleFactor;
      }
   }

   private static float calculateOverlappingAlpha(double animationValue, double elapsedSeconds, ThreeStageAnimation animation, JumpCircles module) {
      String appearType = module.appearAnimationType.getSelected();
      String disappearType = module.disappearAnimationType.getSelected();
      double overlapStart = animation.getAppearDuration() + animation.getExistDuration();
      double appearProgress = elapsedSeconds / animation.getAppearDuration();
      double disappearProgress = (elapsedSeconds - overlapStart) / animation.getDisappearDuration();
      boolean bothUseFade = !"Scale".equals(appearType) && !"Scale".equals(disappearType);
      if (bothUseFade) {
         return (float)Math.max(0.0, Math.min(1.0, appearProgress));
      } else {
         float appearAlpha = 1.0F;
         float disappearAlpha = 1.0F;
         if (!"Scale".equals(appearType)) {
            appearAlpha = (float)Math.max(0.0, Math.min(1.0, appearProgress));
         }

         if (!"Scale".equals(disappearType)) {
            disappearAlpha = (float)Math.max(0.0, Math.min(1.0, 1.0 - disappearProgress));
         }

         return Math.min(appearAlpha, disappearAlpha);
      }
   }

   private static float calculateOverlappingScale(double animationValue, double elapsedSeconds, ThreeStageAnimation animation, JumpCircles module) {
      String appearType = module.appearAnimationType.getSelected();
      String disappearType = module.disappearAnimationType.getSelected();
      double overlapStart = animation.getAppearDuration() + animation.getExistDuration();
      double appearProgress = elapsedSeconds / animation.getAppearDuration();
      double disappearProgress = (elapsedSeconds - overlapStart) / animation.getDisappearDuration();
      boolean bothUseScale = !"Fade".equals(appearType) && !"Fade".equals(disappearType);
      if (bothUseScale) {
         float scaleFactor = (float)Math.max(0.0, Math.min(1.0, appearProgress));
         return module.circleScale.getValue() * scaleFactor;
      } else {
         float appearScale = module.circleScale.getValue();
         float disappearScale = module.circleScale.getValue();
         if (!"Fade".equals(appearType)) {
            float scaleFactor = (float)Math.max(0.0, Math.min(1.0, appearProgress));
            appearScale = module.circleScale.getValue() * scaleFactor;
         }

         if (!"Fade".equals(disappearType)) {
            float scaleFactor = (float)Math.max(0.0, Math.min(1.0, 1.0 - disappearProgress));
            disappearScale = module.circleScale.getValue() * scaleFactor;
         }

         return Math.min(appearScale, disappearScale);
      }
   }

   private JumpCircleRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
