package padej.soup.base.util.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.pipeline.HudRenderPipeline;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.base.util.entity.VisibleUtils;
import padej.soup.core.perftest.HudProfiler;
import padej.soup.core.server.ServerLimitCfg;
import padej.soup.implement.features.modules.hud.TargetHud;

public final class TargetHudRenderer implements QuickImports {
   public static void renderStyleZenith(DrawContext context, LivingEntity target, float x, float y, float width, float height, float health) {
      String displayName = VisibleUtils.getDisplayName(target);
      float hp = PlayerIntersectionUtil.getHealth(target);
      String stringHp = ServerLimitCfg.showHp(target) ? PlayerIntersectionUtil.getHealthString(hp) : "??";
      boolean shouldShowSkin = VisibleUtils.shouldShowSkin(target);
      boolean isPartiallyVisible = VisibleUtils.isPartiallyVisible(target);
      boolean showItems = ServerLimitCfg.showItems();
      boolean showItemsOverlay = ServerLimitCfg.showItemsOverlay();
      List<ItemStack> items = new ArrayList<>(4);
      if (showItems) {
         for (ItemStack stack : target.getEquippedItems()) {
            if (!stack.isEmpty()) {
               items.add(stack);
            }
         }
      }

      Identifier faceTexture = resolveFaceTexture(target);
      renderStyleZenith(
         context,
         target,
         x,
         y,
         width,
         height,
         health,
         displayName,
         stringHp,
         shouldShowSkin,
         isPartiallyVisible,
         showItems,
         showItemsOverlay,
         items,
         faceTexture
      );
   }

   public static void renderStyleAres(DrawContext context, LivingEntity target, float x, float y, float width, float height, float health) {
      String displayName = VisibleUtils.getDisplayName(target);
      float hp = PlayerIntersectionUtil.getHealth(target);
      String stringHp = ServerLimitCfg.showHp(target) ? PlayerIntersectionUtil.getHealthString(hp) : "??";
      boolean shouldShowSkin = VisibleUtils.shouldShowSkin(target);
      boolean isPartiallyVisible = VisibleUtils.isPartiallyVisible(target);
      Identifier faceTexture = resolveFaceTexture(target);
      renderStyleAres(context, target, x, y, width, height, health, displayName, stringHp, shouldShowSkin, isPartiallyVisible, faceTexture);
   }

   private static Identifier resolveFaceTexture(LivingEntity target) {
      if (mc.getEntityRenderDispatcher().getRenderer(target) instanceof LivingEntityRenderer<?, ?, ?> livingRenderer) {
         return resolveFaceTextureTyped(livingRenderer, target);
      } else {
         return null;
      }
   }

   private static <T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> Identifier resolveFaceTextureTyped(
      LivingEntityRenderer<T, S, M> renderer, LivingEntity target
   ) {
      @SuppressWarnings("unchecked")
      T typedTarget = (T)target;
      S state = renderer.getAndUpdateRenderState(typedTarget, tickCounter.getTickDelta(false));
      return renderer.getTexture(state);
   }

   public static void renderStyleZenith(
      DrawContext context,
      LivingEntity target,
      float x,
      float y,
      float width,
      float height,
      float health,
      String displayName,
      String stringHp,
      boolean shouldShowSkin,
      boolean isPartiallyVisible,
      boolean showItems,
      boolean showItemsOverlay,
      List<ItemStack> cachedEquippedItems,
      Identifier cachedFaceTexture
   ) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(18);
      FontRenderer hpFont = Fonts.getSize(14);
      float widthHp = 61.0F;
      long t0 = HudProfiler.nano();
      blur.render(
         ShapeProperties.create(matrix, x, y, width, height)
            .round(3.0F)
            .softness(1.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getBlurRect(TargetHud.getInstance().backAlpha.getValue()))
            .build()
      );
      HudProfiler.recordComponent("TH:Blur", t0);
      if (shouldShowSkin || isPartiallyVisible) {
         Matrix4f capturedPosMatrix = new Matrix4f(matrix.peek().getPositionMatrix());
         Matrix3f capturedNrmMatrix = new Matrix3f(matrix.peek().getNormalMatrix());
         HudRenderPipeline.getInstance().recordVanilla(() -> {
            long tf = HudProfiler.nano();
            matrix.push();
            matrix.peek().getPositionMatrix().set(capturedPosMatrix);
            matrix.peek().getNormalMatrix().set(capturedNrmMatrix);
            if (shouldShowSkin) {
               renderFace(context, target, x + 5.0F, y + 5.5F, 3.0F, 25.0F, cachedFaceTexture);
            } else {
               renderBlurFace(context, target, x + 5.0F, y + 5.5F, 3.0F, 25.0F);
            }

            matrix.pop();
            HudProfiler.recordComponent("TH:Face", tf);
         }, HudRenderPipeline.VanillaLayer.AFTER_RECT);
      }

      if (showItems) {
         long ta = HudProfiler.nano();
         renderArmorStyleZenith(context, target, x, y, width, showItemsOverlay, cachedEquippedItems);
         HudProfiler.recordComponent("TH:Armor", ta);
      }

      long tt = HudProfiler.nano();
      float nameMaxWidth = width - 34.0F - 3.0F;
      float nameWidth = font.getStringWidth(displayName);
      if (nameWidth > nameMaxWidth * 0.85F) {
         font.drawStringWithFadeout(matrix, displayName, x + 34.0F, y + 8.0F, nameMaxWidth, nameMaxWidth * 0.65F, ColorUtil.getText());
      } else {
         font.drawString(matrix, displayName, x + 34.0F, y + 8.0F, ColorUtil.getText());
      }

      rectangle.render(ShapeProperties.create(matrix, x + 34.0F, y + 27.0F, widthHp, 2.0).round(0.75F).color(-16382190).build());
      rectangle.render(
         ShapeProperties.create(matrix, x + 34.0F, y + 27.0F, health, 2.0).softness(4.0F).round(1.0F).color(ColorUtil.roundClientColor(0.2F)).build()
      );
      rectangle.render(ShapeProperties.create(matrix, x + 34.0F, y + 27.0F, health, 2.0).round(0.75F).color(ColorUtil.roundClientColor(1.0F)).build());
      nameMaxWidth = hpFont.getStringWidth(stringHp);
      hpFont.drawString(
         matrix, stringHp, x + MathHelper.clamp(34.0F + health - nameMaxWidth / 2.0F, 34.0F, 95.0F - nameMaxWidth), y + 21.0F, ColorUtil.getText()
      );
      HudProfiler.recordComponent("TH:Text+HP", tt);
   }

   public static void renderArmorStyleZenith(
      DrawContext context, LivingEntity target, float x, float y, float hudWidth, boolean showItemsOverlay, List<ItemStack> items
   ) {
      if (items != null && !items.isEmpty()) {
         MatrixStack matrix = context.getMatrices();
         float armorX = x + hudWidth / 2.0F - items.size() * 5.5F;
         float armorY = y - 13.0F;
         float itemX = -10.5F;
         matrix.push();
         matrix.translate(armorX, armorY, 0.0F);
         blur.render(
            ShapeProperties.create(matrix, 0.0, 0.0, items.size() * 11, 11.0)
               .round(2.5F)
               .softness(1.0F)
               .thickness(2.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getBlurRect(TargetHud.getInstance().backAlpha.getValue()))
               .build()
         );
         Matrix4f armorPosMatrix = new Matrix4f(matrix.peek().getPositionMatrix());
         Matrix3f armorNrmMatrix = new Matrix3f(matrix.peek().getNormalMatrix());
         HudRenderPipeline.getInstance().recordVanilla(() -> {
            matrix.push();
            matrix.peek().getPositionMatrix().set(armorPosMatrix);
            matrix.peek().getNormalMatrix().set(armorNrmMatrix);
            float ix = -10.5F;

            for (ItemStack item : items) {
               ix += 11.0F;
               matrix.push();
               matrix.translate(ix + 1.0F, 1.5F, 0.0F);
               matrix.scale(0.5F, 0.5F, 1.0F);
               context.drawItem(item, 0, 0);
               if (showItemsOverlay) {
                  context.drawStackOverlay(mc.textRenderer, item, 0, 0);
               }

               context.draw();
               matrix.pop();
            }

            matrix.pop();
         }, HudRenderPipeline.VanillaLayer.AFTER_RECT);
         matrix.pop();
      }
   }

   public static void renderStyleAres(
      DrawContext context,
      LivingEntity target,
      float x,
      float y,
      float width,
      float height,
      float health,
      String displayName,
      String stringHp,
      boolean shouldShowSkin,
      boolean isPartiallyVisible,
      Identifier cachedFaceTexture
   ) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(18, Fonts.Type.INTER_BOLD);
      FontRenderer hpFont = Fonts.getSize(16, Fonts.Type.INTER_BOLD);
      float widthHp = 68.0F;
      String aresHp = "HP: " + stringHp;
      float healthBarWidth = health * widthHp / 61.0F;
      long t0 = HudProfiler.nano();
      blur.render(
         ShapeProperties.create(matrix, x, y, width, height)
            .round(7.0F)
            .softness(1.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getBlurRect(TargetHud.getInstance().backAlpha.getValue()))
            .build()
      );
      HudProfiler.recordComponent("TH:Blur", t0);
      if (shouldShowSkin || isPartiallyVisible) {
         Matrix4f capturedPosMatrix = new Matrix4f(matrix.peek().getPositionMatrix());
         Matrix3f capturedNrmMatrix = new Matrix3f(matrix.peek().getNormalMatrix());
         HudRenderPipeline.getInstance().recordVanilla(() -> {
            long tf = HudProfiler.nano();
            matrix.push();
            matrix.peek().getPositionMatrix().set(capturedPosMatrix);
            matrix.peek().getNormalMatrix().set(capturedNrmMatrix);
            if (shouldShowSkin) {
               renderFace(context, target, x + 3.0F, y + 3.0F, 6.0F, 40.0F, cachedFaceTexture);
            } else {
               renderBlurFace(context, target, x + 3.0F, y + 3.0F, 6.0F, 40.0F);
            }

            matrix.pop();
            HudProfiler.recordComponent("TH:Face", tf);
         }, HudRenderPipeline.VanillaLayer.AFTER_RECT);
      }

      long tt = HudProfiler.nano();
      float nameMaxWidth = width - 48.0F - 3.0F;
      float nameWidth = font.getStringWidth(displayName);
      if (nameWidth > nameMaxWidth * 0.85F) {
         font.drawStringWithFadeout(matrix, displayName, x + 48.0F, y + 8.0F, nameMaxWidth, nameMaxWidth * 0.65F, ColorUtil.getText());
      } else {
         font.drawString(matrix, displayName, x + 48.0F, y + 7.0F, ColorUtil.getText());
      }

      nameMaxWidth = x + 48.0F;
      nameWidth = y + 32.0F;
      float barHeight = 9.0F;
      rectangle.render(ShapeProperties.create(matrix, nameMaxWidth, nameWidth, widthHp, barHeight).round(1.5F).color(ColorUtil.getMainGuiColor()).build());
      rectangle.render(
         ShapeProperties.create(matrix, nameMaxWidth - 1.0F, nameWidth - 1.0F, healthBarWidth + 2.0F, barHeight + 2.0F)
            .softness(10.0F)
            .round(2.0F)
            .color(ColorUtil.roundClientColor(0.2F))
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrix, nameMaxWidth, nameWidth, healthBarWidth, barHeight).round(2.0F).color(ColorUtil.roundClientColor(1.0F)).build()
      );
      hpFont.drawString(matrix, aresHp, x + 48.0F, y + 20.5F, ColorUtil.getText());
      HudProfiler.recordComponent("TH:Text+HP", tt);
   }

   private static void renderFace(DrawContext context, LivingEntity target, float x, float y, float round, float size, Identifier textureLocation) {
      if (textureLocation != null) {
         float hurtScale = 1.0F;
         if (target.hurtTime > 0) {
            float hurtProgress = target.hurtTime / 10.0F;
            hurtScale = (float)(1.0 - 0.075F * Math.sin(hurtProgress * Math.PI));
         }

         MatrixStack matrix = context.getMatrices();
         matrix.push();
         float centerX = x + size / 2.0F;
         float centerY = y + size / 2.0F;
         matrix.translate(centerX, centerY, 0.0F);
         matrix.scale(hurtScale, hurtScale, 1.0F);
         matrix.translate(-centerX, -centerY, 0.0F);
         Render2DUtil.drawHead(context, textureLocation, x, y, size, round, ColorUtil.getRect(1.0F), ColorUtil.multRed(-1, 1.0F + target.hurtTime / 4.0F));
         matrix.pop();
      }
   }

   private static void renderBlurFace(DrawContext context, LivingEntity target, float x, float y, float round, float size) {
      MatrixStack matrix = context.getMatrices();
      float hurtScale = 1.0F;
      if (target.hurtTime > 0) {
         float hurtProgress = target.hurtTime / 10.0F;
         hurtScale = (float)(1.0 - 0.075F * Math.sin(hurtProgress * Math.PI));
      }

      matrix.push();
      float centerX = x + size / 2.0F;
      float centerY = y + size / 2.0F;
      matrix.translate(centerX, centerY, 0.0F);
      matrix.scale(hurtScale, hurtScale, 1.0F);
      matrix.translate(-centerX, -centerY, 0.0F);
      int blurColor = ColorUtil.multRed(ColorUtil.getRect(0.2F), 1.0F + target.hurtTime / 4.0F);
      blur.render(
         ShapeProperties.create(matrix, x, y, size, size)
            .round(round)
            .softness(1.0F)
            .thickness(2.0F)
            .color(blurColor)
            .outlineColor(ColorUtil.getOutline())
            .build()
      );
      matrix.pop();
   }

   private TargetHudRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
