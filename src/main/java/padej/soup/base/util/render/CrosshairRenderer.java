package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase.Transparency;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.EntityHitResult;
import org.joml.Matrix4f;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.animation.Interpolation;
import padej.soup.base.util.animation.Interpolations;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.features.modules.hud.CrossHair;

public final class CrosshairRenderer implements QuickImports {
   private static final RenderLayer CROSSHAIR_INVERT_LAYER = RenderLayer.of(
      "crosshair_invert",
      VertexFormats.POSITION_COLOR,
      DrawMode.QUADS,
      1536,
      MultiPhaseParameters.builder().program(RenderPhase.POSITION_COLOR_PROGRAM).transparency(new Transparency("crosshair_transparency", () -> {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SrcFactor.ONE_MINUS_DST_COLOR, DstFactor.ONE_MINUS_SRC_COLOR, SrcFactor.ONE, DstFactor.ZERO);
      }, () -> {
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      })).build(false)
   );
   private static final RenderLayer CROSSHAIR_INVERT_TRIANGLES = RenderLayer.of(
      "crosshair_invert_triangles",
      VertexFormats.POSITION_COLOR,
      DrawMode.TRIANGLES,
      1536,
      MultiPhaseParameters.builder().program(RenderPhase.POSITION_COLOR_PROGRAM).transparency(new Transparency("crosshair_transparency_tri", () -> {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SrcFactor.ONE_MINUS_DST_COLOR, DstFactor.ONE_MINUS_SRC_COLOR, SrcFactor.ONE, DstFactor.ZERO);
      }, () -> {
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      })).build(false)
   );
   private static final MatrixStack IDENTITY_STACK = new MatrixStack();
   private static final Matrix4f IDENTITY_MATRIX = IDENTITY_STACK.peek().getPositionMatrix();
   private static float colorAnimation = 0.0F;
   private static float hitAnimation = 0.0F;
   private static float lastAttackProgress = 1.0F;
   private static long lastBlobUpdateTime = System.currentTimeMillis();
   private static long lastCacheRefresh = 0L;
   private static final long CACHE_INTERVAL_MS = 50L;
   private static String cachedStyle = "Cross";
   private static boolean cachedDynamic = false;
   private static int cachedAttackIndent = 10;
   private static float cachedCrossWidth = 4.0F;
   private static float cachedCrossHeight = 1.0F;
   private static int cachedIndent = 0;
   private static boolean cachedInvertColor = false;
   private static float cachedColorAnimSpeed = 2.0F;
   private static float cachedBlobSize = 6.0F;
   private static float cachedBlobThickness = 2.0F;
   private static float cachedAttackSizeMul = 0.9F;
   private static float cachedEntityWidthMul = 0.7F;
   private static float cachedEntityHeightMul = 0.2F;
   private static float cachedHitStretchMul = 0.0F;
   private static float cachedOutlineThickness = 3.5F;
   private static float cachedFillMaxAlpha = 0.5F;
   private static boolean cachedEnableFill = true;
   private static float cachedHitAnimSpeed = 5.0F;
   private static Interpolation cachedInterpolation = Interpolations.getByName("Ease Out");
   private static float cachedRingSize = 18.0F;
   private static float cachedRingThickness = 0.3F;
   private static float cachedRingRoundness = 0.15F;
   private static boolean cachedDynInvert = true;
   private static float cachedDynSpeed = 3.0F;
   private static float cachedDynRange = 15.0F;
   private static float cachedDynBackSpeed = 5.0F;
   private static boolean cachedCustomColorMode = false;
   private static int[] cachedCustomColors = null;
   private static String cachedColorAnimType = "Wave";
   private static int cachedTargetColor = ColorUtil.RED;

   private static void refreshCache(CrossHair module) {
      long now = System.currentTimeMillis();
      if (now - lastCacheRefresh >= 50L) {
         lastCacheRefresh = now;
         cachedStyle = module.getStyleSetting().getSelected();
         cachedDynamic = module.getDynamicSetting().isValue();
         cachedAttackIndent = module.getAttackSetting().getInt();
         cachedCrossWidth = module.getSize1Setting().getValue();
         cachedCrossHeight = module.getSize2Setting().getValue();
         cachedIndent = module.getIndentSetting().getInt();
         cachedInvertColor = module.getInvertColorSetting().isValue();
         cachedColorAnimSpeed = module.getColorAnimationSpeedSetting().getValue();
         cachedBlobSize = module.getBlobSizeSetting().getValue();
         cachedBlobThickness = module.getBlobThicknessSetting().getValue();
         cachedAttackSizeMul = module.getAttackSizeMultiplierSetting().getValue();
         cachedEntityWidthMul = module.getEntityWidthMultiplierSetting().getValue();
         cachedEntityHeightMul = module.getEntityHeightMultiplierSetting().getValue();
         cachedHitStretchMul = module.getHitStretchMultiplierSetting().getValue();
         cachedOutlineThickness = module.getOutlineThicknessSetting().getValue();
         cachedFillMaxAlpha = module.getFillMaxAlphaSetting().getValue();
         cachedEnableFill = module.getEnableFillSetting().isValue();
         cachedHitAnimSpeed = module.getHitAnimationSpeedSetting().getValue();
         cachedInterpolation = Interpolations.getByName(module.getAnimationTypeSetting().getSelected());
         cachedRingSize = module.getRingSizeSetting().getValue();
         cachedRingThickness = module.getRingThicknessSetting().getValue();
         cachedRingRoundness = module.getRingRoundnessSetting().getValue();
         cachedDynInvert = module.getDynamicInvertSetting().isValue();
         cachedDynSpeed = module.getDynamicSpeedSetting().getValue();
         cachedDynRange = module.getDynamicRangeSetting().getInt();
         cachedDynBackSpeed = module.getDynamicBackSpeedSetting().getValue();
         cachedCustomColorMode = module.getColorMode().isSelected("Custom");
         cachedCustomColors = module.getCustomColors();
         cachedColorAnimType = module.getColorAnimation().getSelected();
         cachedTargetColor = ColorUtil.multAlpha(module.getTargetColorSetting().getColor(), 1.0F);
      }
   }

   public static void render(CrossHair module) {
      refreshCache(module);
      float colorSpeed = "Blob".equals(cachedStyle) ? cachedColorAnimSpeed : 2.0F;
      colorAnimation = MathUtil.interpolateSmooth((double)colorSpeed, colorAnimation, mc.crosshairTarget instanceof EntityHitResult ? 1.0F : 0.0F);
      int mainColor = getCrosshairColor();
      float centerX = window.getScaledWidth() / 2.0F;
      float centerY = window.getScaledHeight() / 2.0F;
      if (cachedDynamic) {
         updateDynamicPosition(module, centerX, centerY);
         centerX = module.xAnim;
         centerY = module.yAnim;
      } else {
         module.xAnim = centerX;
         module.yAnim = centerY;
      }

      float cooldown = cachedAttackIndent - cachedAttackIndent * mc.player.getAttackCooldownProgress(tickCounter.getTickDelta(false));
      if ("Cross".equals(cachedStyle)) {
         renderCross(centerX, centerY, cooldown, mainColor);
      } else if ("Blob".equals(cachedStyle)) {
         renderBlob(centerX, centerY, mainColor);
      } else if ("Ring".equals(cachedStyle)) {
         renderRing(centerX, centerY);
      }
   }

   private static void updateDynamicPosition(CrossHair module, float midX, float midY) {
      float yawDelta = mc.player.prevHeadYaw - mc.player.getHeadYaw();
      float pitchDelta = module.prevPitch - mc.player.getPitch();
      module.prevPitch = mc.player.getPitch();
      float invertMultiplier = cachedDynInvert ? -1.0F : 1.0F;
      float speed = cachedDynSpeed * 0.05F;
      module.xAnim += yawDelta * speed * invertMultiplier;
      module.yAnim += pitchDelta * speed * invertMultiplier;
      float offsetX = module.xAnim - midX;
      float offsetY = module.yAnim - midY;
      double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY);
      double attractionFactor = Math.min(distance / cachedDynRange, 1.0);
      float maxSmoothingFactor = cachedDynBackSpeed * 0.1F;
      float smoothingFactor = (float)(maxSmoothingFactor * attractionFactor);
      smoothingFactor = Math.min(smoothingFactor, 0.8F);
      module.xAnim = module.xAnim + (midX - module.xAnim) * smoothingFactor;
      module.yAnim = module.yAnim + (midY - module.yAnim) * smoothingFactor;
   }

   private static void renderCross(float centerX, float centerY, float cooldown, int mainColor) {
      float width = cachedCrossWidth;
      float height = cachedCrossHeight;
      float offset = height / 2.0F;
      float indent = cachedIndent + cooldown;
      if (cachedInvertColor) {
         Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
         VertexConsumer vertexConsumer = immediate.getBuffer(CROSSHAIR_INVERT_LAYER);
         int white = -1;
         renderQuad(vertexConsumer, IDENTITY_MATRIX, centerX - offset, centerY - width - indent, height, width, white);
         renderQuad(vertexConsumer, IDENTITY_MATRIX, centerX - offset, centerY + indent, height, width, white);
         renderQuad(vertexConsumer, IDENTITY_MATRIX, centerX - width - indent, centerY - offset, width, height, white);
         renderQuad(vertexConsumer, IDENTITY_MATRIX, centerX + indent, centerY - offset, width, height, white);
         immediate.draw();
      } else {
         int outlineColor = ColorUtil.BLACK;
         renderCrosshairLines(centerX, centerY, width, height, 1.0F, indent, offset, outlineColor);
         renderCrosshairLines(centerX, centerY, width, height, 0.0F, indent, offset, mainColor);
      }
   }

   private static void renderQuad(VertexConsumer buffer, Matrix4f matrix, float x, float y, float width, float height, int color) {
      buffer.vertex(matrix, x, y, 0.0F).color(color);
      buffer.vertex(matrix, x, y + height, 0.0F).color(color);
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(color);
      buffer.vertex(matrix, x + width, y, 0.0F).color(color);
   }

   private static void renderBlob(float centerX, float centerY, int mainColor) {
      float baseSize = cachedBlobSize;
      float thicknessFactor = Math.max(0.1F, cachedBlobThickness / 2.0F);
      float attackProgress = 1.0F - mc.player.getAttackCooldownProgress(tickCounter.getTickDelta(false));
      float attackProgressDelta = attackProgress - lastAttackProgress;
      boolean attackedThisFrame = attackProgressDelta > 0.35F || lastAttackProgress < 0.1F && attackProgress > 0.9F;
      if (attackedThisFrame && mc.crosshairTarget instanceof EntityHitResult) {
         hitAnimation = 1.0F;
      }

      lastAttackProgress = attackProgress;
      long currentTime = System.currentTimeMillis();
      float deltaMs = Math.max(1.0F, Math.min(100.0F, (float)(currentTime - lastBlobUpdateTime)));
      lastBlobUpdateTime = currentTime;
      float hitDecay = (float)Math.exp(-cachedHitAnimSpeed * deltaMs * 0.0035F);
      hitAnimation *= hitDecay;
      if (hitAnimation < 0.001F) {
         hitAnimation = 0.0F;
      }

      float easedProgress = (float)cachedInterpolation.interpolate(attackProgress);
      float animatedSize = baseSize + baseSize * cachedAttackSizeMul * easedProgress;
      float width = animatedSize * (1.0F + colorAnimation * cachedEntityWidthMul);
      float height = baseSize * thicknessFactor * (1.0F - colorAnimation * cachedEntityHeightMul);
      float rounded = height / 2.0F;
      float hitMultiplier = 1.0F + hitAnimation * cachedHitStretchMul;
      width *= hitMultiplier;
      height /= hitMultiplier;
      int fillColor = cachedEnableFill ? ColorUtil.replAlpha(mainColor, easedProgress * cachedFillMaxAlpha) : ColorUtil.replAlpha(mainColor, 0);
      rectangle.render(
         ShapeProperties.create(IDENTITY_STACK, centerX - width / 2.0F, centerY - height / 2.0F, width, height)
            .round(rounded)
            .thickness(cachedOutlineThickness)
            .outlineColor(mainColor)
            .color(fillColor)
            .build()
      );
   }

   private static void renderEllipse(VertexConsumer buffer, Matrix4f matrix, float x, float y, float width, float height, int color) {
      int segments = 64;
      float centerX = x + width / 2.0F;
      float centerY = y + height / 2.0F;
      float radiusX = width / 2.0F;
      float radiusY = height / 2.0F;

      for (int i = 0; i < segments; i++) {
         float angle1 = (float)((Math.PI * 2) * i / segments);
         float angle2 = (float)((Math.PI * 2) * (i + 1) / segments);
         float x1 = centerX + (float)Math.cos(angle1) * radiusX;
         float y1 = centerY + (float)Math.sin(angle1) * radiusY;
         float x2 = centerX + (float)Math.cos(angle2) * radiusX;
         float y2 = centerY + (float)Math.sin(angle2) * radiusY;
         buffer.vertex(matrix, centerX, centerY, 0.0F).color(color);
         buffer.vertex(matrix, x1, y1, 0.0F).color(color);
         buffer.vertex(matrix, x2, y2, 0.0F).color(color);
      }
   }

   private static void renderRoundedRect(VertexConsumer buffer, Matrix4f matrix, float x, float y, float width, float height, float radius, int color) {
      float maxRadius = Math.min(width, height) / 2.0F;
      radius = Math.min(radius, maxRadius);
      if (radius <= 0.0F) {
         renderQuad(buffer, matrix, x, y, width, height, color);
      } else {
         float innerX = x + radius;
         float innerY = y + radius;
         float innerWidth = width - radius * 2.0F;
         float innerHeight = height - radius * 2.0F;
         if (innerWidth > 0.0F) {
            renderQuad(buffer, matrix, innerX, y, innerWidth, height, color);
         }

         if (innerHeight > 0.0F) {
            renderQuad(buffer, matrix, x, innerY, radius, innerHeight, color);
            renderQuad(buffer, matrix, innerX + innerWidth, innerY, radius, innerHeight, color);
         }

         int cornerSegments = 16;
         renderCorner(buffer, matrix, innerX, innerY, radius, 180.0F, 270.0F, cornerSegments, color);
         renderCorner(buffer, matrix, innerX + innerWidth, innerY, radius, 270.0F, 360.0F, cornerSegments, color);
         renderCorner(buffer, matrix, innerX + innerWidth, innerY + innerHeight, radius, 0.0F, 90.0F, cornerSegments, color);
         renderCorner(buffer, matrix, innerX, innerY + innerHeight, radius, 90.0F, 180.0F, cornerSegments, color);
      }
   }

   private static void renderCorner(
      VertexConsumer buffer, Matrix4f matrix, float centerX, float centerY, float radius, float startAngle, float endAngle, int segments, int color
   ) {
      float angleStep = (endAngle - startAngle) / segments;

      for (int i = 0; i < segments; i++) {
         float angle1 = (float)Math.toRadians(startAngle + i * angleStep);
         float angle2 = (float)Math.toRadians(startAngle + (i + 1) * angleStep);
         float x1 = centerX + (float)Math.cos(angle1) * radius;
         float y1 = centerY + (float)Math.sin(angle1) * radius;
         float x2 = centerX + (float)Math.cos(angle2) * radius;
         float y2 = centerY + (float)Math.sin(angle2) * radius;
         buffer.vertex(matrix, centerX, centerY, 0.0F).color(color);
         buffer.vertex(matrix, x1, y1, 0.0F).color(color);
         buffer.vertex(matrix, x2, y2, 0.0F).color(color);
         buffer.vertex(matrix, centerX, centerY, 0.0F).color(color);
      }
   }

   private static void renderCrosshairLines(float x, float y, float width, float height, float padding, float indent, float offset, int color) {
      Render2DUtil.drawQuad(x - offset - padding / 2.0F, y - width - indent - padding / 2.0F, height + padding, width + padding, color);
      Render2DUtil.drawQuad(x - offset - padding / 2.0F, y + indent - padding / 2.0F, height + padding, width + padding, color);
      Render2DUtil.drawQuad(x - width - indent - padding / 2.0F, y - offset - padding / 2.0F, width + padding, height + padding, color);
      Render2DUtil.drawQuad(x + indent - padding / 2.0F, y - offset - padding / 2.0F, width + padding, height + padding, color);
   }

   private static void renderRing(float centerX, float centerY) {
      float attackProgress = mc.player.getAttackCooldownProgress(tickCounter.getTickDelta(false)) * 360.0F;
      int color1 = getRingVertexColor(0);
      int color2 = getRingVertexColor(90);
      int color3 = getRingVertexColor(180);
      int color4 = getRingVertexColor(270);
      arc.render(
         ShapeProperties.create(IDENTITY_STACK, centerX - cachedRingSize / 2.0F, centerY - cachedRingSize / 2.0F, cachedRingSize, cachedRingSize)
            .round(cachedRingRoundness)
            .thickness(cachedRingThickness)
            .end(attackProgress)
            .color(color1, color2, color3, color4)
            .build()
      );
   }

   private static void renderArcTriangles(
      VertexConsumer buffer, Matrix4f matrix, float x, float y, float width, float height, float startAngle, float endAngle, float thickness, int color
   ) {
      int segments = 64;
      float centerX = x + width / 2.0F;
      float centerY = y + height / 2.0F;
      float outerRadiusX = width / 2.0F;
      float outerRadiusY = height / 2.0F;
      float actualThickness = thickness;
      if (thickness < 1.0F) {
         actualThickness = outerRadiusX * thickness;
      }

      float innerRadiusX = Math.max(0.1F, outerRadiusX - actualThickness);
      float innerRadiusY = Math.max(0.1F, outerRadiusY - actualThickness);
      if (!(endAngle <= 0.0F)) {
         int actualSegments = Math.max(4, (int)(segments * endAngle / 360.0F));
         float angleStep = endAngle / actualSegments;

         for (int i = 0; i < actualSegments; i++) {
            float angle1 = (float)Math.toRadians(startAngle + i * angleStep);
            float angle2 = (float)Math.toRadians(startAngle + (i + 1) * angleStep);
            float cos1 = (float)Math.cos(angle1);
            float sin1 = (float)Math.sin(angle1);
            float cos2 = (float)Math.cos(angle2);
            float sin2 = (float)Math.sin(angle2);
            float x1Outer = centerX + cos1 * outerRadiusX;
            float y1Outer = centerY + sin1 * outerRadiusY;
            float x2Outer = centerX + cos2 * outerRadiusX;
            float y2Outer = centerY + sin2 * outerRadiusY;
            float x1Inner = centerX + cos1 * innerRadiusX;
            float y1Inner = centerY + sin1 * innerRadiusY;
            float x2Inner = centerX + cos2 * innerRadiusX;
            float y2Inner = centerY + sin2 * innerRadiusY;
            buffer.vertex(matrix, x1Inner, y1Inner, 0.0F).color(color);
            buffer.vertex(matrix, x1Outer, y1Outer, 0.0F).color(color);
            buffer.vertex(matrix, x2Outer, y2Outer, 0.0F).color(color);
            buffer.vertex(matrix, x1Inner, y1Inner, 0.0F).color(color);
            buffer.vertex(matrix, x2Outer, y2Outer, 0.0F).color(color);
            buffer.vertex(matrix, x2Inner, y2Inner, 0.0F).color(color);
         }
      }
   }

   private static void renderArc(
      VertexConsumer buffer, Matrix4f matrix, float x, float y, float width, float height, float startAngle, float endAngle, float thickness, int color
   ) {
      int segments = 64;
      float centerX = x + width / 2.0F;
      float centerY = y + height / 2.0F;
      float outerRadiusX = width / 2.0F;
      float outerRadiusY = height / 2.0F;
      float actualThickness = thickness;
      if (thickness < 1.0F) {
         actualThickness = outerRadiusX * thickness;
      }

      float innerRadiusX = Math.max(0.1F, outerRadiusX - actualThickness);
      float innerRadiusY = Math.max(0.1F, outerRadiusY - actualThickness);
      if (!(endAngle <= 0.0F)) {
         int actualSegments = Math.max(4, (int)(segments * endAngle / 360.0F));
         float angleStep = endAngle / actualSegments;

         for (int i = 0; i < actualSegments; i++) {
            float angle1 = (float)Math.toRadians(startAngle + i * angleStep);
            float angle2 = (float)Math.toRadians(startAngle + (i + 1) * angleStep);
            float cos1 = (float)Math.cos(angle1);
            float sin1 = (float)Math.sin(angle1);
            float cos2 = (float)Math.cos(angle2);
            float sin2 = (float)Math.sin(angle2);
            float x1Outer = centerX + cos1 * outerRadiusX;
            float y1Outer = centerY + sin1 * outerRadiusY;
            float x2Outer = centerX + cos2 * outerRadiusX;
            float y2Outer = centerY + sin2 * outerRadiusY;
            float x1Inner = centerX + cos1 * innerRadiusX;
            float y1Inner = centerY + sin1 * innerRadiusY;
            float x2Inner = centerX + cos2 * innerRadiusX;
            float y2Inner = centerY + sin2 * innerRadiusY;
            buffer.vertex(matrix, x1Inner, y1Inner, 0.0F).color(color);
            buffer.vertex(matrix, x1Outer, y1Outer, 0.0F).color(color);
            buffer.vertex(matrix, x2Outer, y2Outer, 0.0F).color(color);
            buffer.vertex(matrix, x2Inner, y2Inner, 0.0F).color(color);
         }
      }
   }

   private static int getRingVertexColor(int vertexAngle) {
      int baseColor = getBaseColorAtAngle(vertexAngle);
      return ColorUtil.overCol(baseColor, cachedTargetColor, colorAnimation);
   }

   private static int getBaseColorAtAngle(int angle) {
      if (!cachedCustomColorMode || cachedCustomColors == null || cachedCustomColors.length <= 0) {
         return ColorUtil.multAlpha(ColorUtil.getClientColor(), 1.0F);
      } else {
         return "Wave".equals(cachedColorAnimType) ? getWaveColorAtAngle(cachedCustomColors, angle) : ColorUtil.multAlpha(cachedCustomColors[0], 1.0F);
      }
   }

   private static int getWaveColorAtAngle(int[] colors, int angle) {
      if (colors.length == 1) {
         return ColorUtil.multAlpha(colors[0], 1.0F);
      } else {
         long time = System.currentTimeMillis() / 10L;
         float totalAngle = (float)((angle + time) % 360L);
         float anglePerColor = 360.0F / colors.length;
         float colorProgress = totalAngle / anglePerColor;
         int index1 = (int)Math.floor(colorProgress) % colors.length;
         int index2 = (index1 + 1) % colors.length;
         float lerp = colorProgress - (int)Math.floor(colorProgress);
         return ColorUtil.multAlpha(ColorUtil.overCol(colors[index1], colors[index2], lerp), 1.0F);
      }
   }

   private static int getCrosshairColor() {
      int defaultColor = getBaseColor();
      return ColorUtil.overCol(defaultColor, cachedTargetColor, colorAnimation);
   }

   private static int getBaseColor() {
      if (!cachedCustomColorMode || cachedCustomColors == null || cachedCustomColors.length <= 0) {
         return ColorUtil.multAlpha(ColorUtil.getClientColor(), 1.0F);
      } else {
         return "Wave".equals(cachedColorAnimType) ? getWaveColor(cachedCustomColors) : ColorUtil.multAlpha(cachedCustomColors[0], 1.0F);
      }
   }

   private static int getWaveColor(int[] colors) {
      if (colors.length == 1) {
         return ColorUtil.multAlpha(colors[0], 1.0F);
      } else if (colors.length == 2) {
         int angle = (int)(System.currentTimeMillis() / 8L % 360L);
         angle = angle >= 180 ? 360 - angle : angle;
         return ColorUtil.multAlpha(ColorUtil.overCol(colors[0], colors[1], angle / 180.0F), 1.0F);
      } else {
         float timeProgress = (float)(System.currentTimeMillis() / 10L % (colors.length * 360L)) / 360.0F;
         int index1 = (int)Math.floor(timeProgress) % colors.length;
         int index2 = (index1 + 1) % colors.length;
         float lerp = timeProgress - (int)Math.floor(timeProgress);
         return ColorUtil.multAlpha(ColorUtil.overCol(colors[index1], colors[index2], lerp), 1.0F);
      }
   }

   private CrosshairRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
