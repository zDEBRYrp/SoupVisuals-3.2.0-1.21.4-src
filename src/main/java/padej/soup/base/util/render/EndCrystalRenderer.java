package padej.soup.base.util.render;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.particle.ParticleColorUtil;
import padej.soup.implement.features.modules.other.EndCrystal;
import padej.soup.mixins.accessor.EndCrystalEntityModelAccessor;

public final class EndCrystalRenderer implements QuickImports {
   public static void drawCrystals(WorldRenderContext context, EndCrystal module) {
      if (module.isEnabled() && mc.world != null) {
         MatrixStack matrixStack = context.matrixStack();
         Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
         renderPhantomCrystals(matrixStack, vertexConsumers, 15728880, null, module);
         vertexConsumers.draw();
      }
   }

   public static void renderCrystal(
      EndCrystalEntityRenderState state,
      MatrixStack matrixStack,
      int light,
      EndCrystalEntityModel model,
      VertexConsumerProvider vertexConsumerProvider,
      EndCrystal module
   ) {
      if (module.getCrystalModel() == null) {
         module.setCrystalModel(model);
      }

      matrixStack.push();
      float baseYOffset = -0.5F + module.getYOffset();
      matrixStack.translate(0.0F, baseYOffset, 0.0F);
      matrixStack.translate(0.0F, 0.5F, 0.0F);
      float scale = module.getSize();
      matrixStack.scale(scale, scale, scale);
      matrixStack.translate(0.0F, -0.5F, 0.0F);
      EndCrystalEntityModelAccessor accessor = (EndCrystalEntityModelAccessor)model;
      ModelPart outerGlass = accessor.getOuterGlass();
      float originalAge = state.age;
      state.age = state.age * module.getAnimationSpeed();
      model.setAngles(state);
      state.age = originalAge;
      Vec3d crystalPos = new Vec3d(state.x, state.y, state.z);
      module.trackCrystal(crystalPos, outerGlass.yaw, outerGlass.pivotY);
      RenderLayer renderLayer = RenderLayer.getBeaconBeam(Identifier.ofVanilla("textures/entity/alt_crystal.png"), true);
      VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);
      ModelPart base = accessor.getBase();
      ModelPart innerGlass = accessor.getInnerGlass();
      ModelPart cube = accessor.getCube();
      int baseOffset = (int)((state.x + state.y + state.z) * 100.0) % 360;
      if (module.isVertexMode()) {
         int color = getColor(baseOffset, module);
         base.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, color);
         if (module.isOuterGlassVisible()) {
            color = getColor(baseOffset + 90, module);
            matrixStack.push();
            outerGlass.rotate(matrixStack);
            outerGlass.renderCuboids(matrixStack.peek(), vertexConsumer, light, OverlayTexture.DEFAULT_UV, color);
            matrixStack.pop();
         }

         if (module.isInnerGlassVisible()) {
            color = getColor(baseOffset + 180, module);
            matrixStack.push();
            outerGlass.rotate(matrixStack);
            innerGlass.rotate(matrixStack);
            innerGlass.renderCuboids(matrixStack.peek(), vertexConsumer, light, OverlayTexture.DEFAULT_UV, color);
            matrixStack.pop();
         }

         if (module.isCubeVisible()) {
            color = getColor(baseOffset + 270, module);
            matrixStack.push();
            outerGlass.rotate(matrixStack);
            innerGlass.rotate(matrixStack);
            cube.rotate(matrixStack);
            cube.renderCuboids(matrixStack.peek(), vertexConsumer, light, OverlayTexture.DEFAULT_UV, color);
            matrixStack.pop();
         }
      } else {
         int colorx = getColor(baseOffset, module);
         base.visible = true;
         outerGlass.visible = module.isOuterGlassVisible();
         innerGlass.visible = module.isInnerGlassVisible();
         cube.visible = module.isCubeVisible();
         model.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, colorx);
         outerGlass.visible = true;
         innerGlass.visible = true;
         cube.visible = true;
      }

      matrixStack.pop();
   }

   public static void renderPhantomCrystals(
      MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, EndCrystalEntityModel model, EndCrystal module
   ) {
      List<EndCrystal.PhantomCrystal> phantomCrystals = module.getPhantomCrystals();
      phantomCrystals.removeIf(EndCrystal.PhantomCrystal::isFinished);
      EndCrystalEntityModel renderModel = model != null ? model : module.getCrystalModel();
      if (renderModel != null) {
         for (EndCrystal.PhantomCrystal phantom : phantomCrystals) {
            renderPhantomCrystal(phantom, matrixStack, vertexConsumerProvider, light, renderModel, module);
         }
      }
   }

   private static void renderPhantomCrystal(
      EndCrystal.PhantomCrystal phantom,
      MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumerProvider,
      int light,
      EndCrystalEntityModel model,
      EndCrystal module
   ) {
      if (!phantom.isFinished()) {
         matrixStack.push();
         Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
         matrixStack.translate(phantom.pos.x - cameraPos.x, phantom.pos.y - cameraPos.y, phantom.pos.z - cameraPos.z);
         double animationProgress = phantom.animation.getOutput();
         float alphaFade = 1.0F - (float)animationProgress;
         float scaleMultiplier = module.getBreakScaleMultiplier();
         float baseScale = module.getSize();
         float totalScale = baseScale * (1.0F + scaleMultiplier * (float)animationProgress);
         float baseYOffset = -0.5F + module.getYOffset();
         matrixStack.translate(0.0F, baseYOffset, 0.0F);
         matrixStack.translate(0.0F, 0.5F, 0.0F);
         matrixStack.scale(totalScale, totalScale, totalScale);
         matrixStack.translate(0.0F, -0.5F, 0.0F);
         EndCrystalEntityModelAccessor accessor = (EndCrystalEntityModelAccessor)model;
         ModelPart outerGlass = accessor.getOuterGlass();
         outerGlass.pivotY = phantom.outerGlassPivotY;
         outerGlass.yaw = phantom.outerGlassYaw;
         RenderLayer renderLayer = RenderLayer.getBeaconBeam(Identifier.ofVanilla("textures/entity/alt_crystal.png"), true);
         VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);
         ModelPart base = accessor.getBase();
         ModelPart innerGlass = accessor.getInnerGlass();
         ModelPart cube = accessor.getCube();
         int baseOffset = (int)((phantom.pos.x + phantom.pos.y + phantom.pos.z) * 100.0) % 360;
         if (module.isVertexMode()) {
            int color = applyAlphaFade(getColor(baseOffset, module), alphaFade);
            base.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, color);
            if (module.isOuterGlassVisible()) {
               color = applyAlphaFade(getColor(baseOffset + 90, module), alphaFade);
               matrixStack.push();
               outerGlass.rotate(matrixStack);
               outerGlass.renderCuboids(matrixStack.peek(), vertexConsumer, light, OverlayTexture.DEFAULT_UV, color);
               matrixStack.pop();
            }

            if (module.isInnerGlassVisible()) {
               color = applyAlphaFade(getColor(baseOffset + 180, module), alphaFade);
               matrixStack.push();
               outerGlass.rotate(matrixStack);
               innerGlass.rotate(matrixStack);
               innerGlass.renderCuboids(matrixStack.peek(), vertexConsumer, light, OverlayTexture.DEFAULT_UV, color);
               matrixStack.pop();
            }

            if (module.isCubeVisible()) {
               color = applyAlphaFade(getColor(baseOffset + 270, module), alphaFade);
               matrixStack.push();
               outerGlass.rotate(matrixStack);
               innerGlass.rotate(matrixStack);
               cube.rotate(matrixStack);
               cube.renderCuboids(matrixStack.peek(), vertexConsumer, light, OverlayTexture.DEFAULT_UV, color);
               matrixStack.pop();
            }
         } else {
            int colorx = applyAlphaFade(getColor(baseOffset, module), alphaFade);
            base.visible = true;
            outerGlass.visible = module.isOuterGlassVisible();
            innerGlass.visible = module.isInnerGlassVisible();
            cube.visible = module.isCubeVisible();
            model.render(matrixStack, vertexConsumer, light, OverlayTexture.DEFAULT_UV, colorx);
            outerGlass.visible = true;
            innerGlass.visible = true;
            cube.visible = true;
         }

         matrixStack.pop();
      }
   }

   private static int getColor(int angleOffset, EndCrystal module) {
      if (module.isSyncMode()) {
         return ColorUtil.getClientColor();
      } else {
         int[] colors = module.getCustomColors();
         if (colors == null || colors.length == 0) {
            return -1;
         } else if (module.isWaveMode()) {
            return ParticleColorUtil.getWaveColor(colors, 1.0F);
         } else {
            return module.isVertexMode() ? ParticleColorUtil.getVertexGradientColor(angleOffset, colors, 1.0F) : colors[0];
         }
      }
   }

   private static int applyAlphaFade(int color, float fade) {
      int alpha = color >> 24 & 0xFF;
      int red = color >> 16 & 0xFF;
      int green = color >> 8 & 0xFF;
      int blue = color & 0xFF;
      int newAlpha = (int)(alpha * Math.max(0.0F, Math.min(1.0F, fade)));
      return newAlpha << 24 | red << 16 | green << 8 | blue;
   }

   private EndCrystalRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
