package padej.soup.base.util.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.modules.hud.HotBar;

public final class HotBarStatusRenderer implements QuickImports {
   private static float currentScale = 1.0F;
   private static float lastHealthValue = Float.NaN;
   private static String cachedHealthText;
   private static float lastArmorValue = Float.NaN;
   private static String cachedArmorText;
   private static float lastFoodValue = Float.NaN;
   private static String cachedFoodText;
   private static float lastAirValue = Float.NaN;
   private static String cachedAirText;

   public static void setScale(float scale) {
      currentScale = scale;
   }

   public static void renderHealthBar(DrawContext context, int x, int y, int width, int height, int radius, float health, float maxHealth, float absorption) {
      float healthProgress = Math.min(health / maxHealth, 1.0F);
      int healthFilledWidth = (int)(width * healthProgress);
      MatrixStack matrices = context.getMatrices();
      blur.render(
         ShapeProperties.create(matrices, x, y, width, height)
            .round(radius)
            .thickness(2.0F * currentScale)
            .softness(1.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getRect(0.7F))
            .build()
      );
      if (healthFilledWidth > 0) {
         int colorStart = ColorUtil.multDark(-52429, 0.6F);
         int colorEnd = -43691;
         if (HotBar.getInstance().getBarsGlow().isValue()) {
            rectangle.render(
               ShapeProperties.create(matrices, x, y, healthFilledWidth, height)
                  .round(radius)
                  .softness(10.0F)
                  .color(ColorUtil.multAlpha(colorEnd, 0.2F))
                  .build()
            );
         }

         rectangle.render(
            ShapeProperties.create(matrices, x + 1, y + 1, healthFilledWidth - 2, height - 2)
               .round(radius - 0.5F)
               .color(colorStart, colorEnd, colorEnd, colorStart)
               .build()
         );
      }

      if (absorption > 0.0F) {
         float maxAbsorption = mc.player != null ? mc.player.getMaxAbsorption() : 20.0F;
         float absorptionProgress = maxAbsorption > 0.0F ? Math.min(absorption / maxAbsorption, 1.0F) : 0.0F;
         int absorptionWidth = (int)(width * absorptionProgress);
         if (absorptionWidth > 0) {
            int absColorStart = ColorUtil.multDark(-22016, 0.6F);
            int absColorEnd = -8909;
            if (HotBar.getInstance().getBarsGlow().isValue()) {
               rectangle.render(
                  ShapeProperties.create(matrices, x, y, absorptionWidth, height)
                     .round(radius)
                     .softness(10.0F)
                     .color(ColorUtil.multAlpha(absColorEnd, 0.2F))
                     .build()
               );
            }

            rectangle.render(
               ShapeProperties.create(matrices, x + 1, y + 1, absorptionWidth - 2, height - 2)
                  .round(radius - 0.5F)
                  .color(absColorStart, absColorEnd, absColorEnd, absColorStart)
                  .build()
            );
         }
      }

      if (HotBar.getInstance().getBarsText().isValue()) {
         float totalHealth = health + absorption;
         if (totalHealth != lastHealthValue) {
            lastHealthValue = totalHealth;
            cachedHealthText = String.format("%.1f", totalHealth);
         }

         String text = cachedHealthText;
         float textHeight = 12.0F;
         float textY = y + (height - textHeight) / 2.0F;
         Fonts.getSize(12, Fonts.Type.INTER_BOLD).drawCenteredString(matrices, text, x + width / 2.0F, textY + 5.0F, ColorUtil.getText());
      }

      image.setTexture("textures/hotbar/hp.png").render(ShapeProperties.create(matrices, x + 2, y + 1.5F, 6.0, 6.0).color(ColorUtil.getText()).build());
   }

   public static void renderArmorBar(DrawContext context, int x, int y, int width, int height, int radius, float armor) {
      float progress = Math.min(armor / 20.0F, 1.0F);
      int filledWidth = (int)(width * progress);
      MatrixStack matrices = context.getMatrices();
      blur.render(
         ShapeProperties.create(matrices, x, y, width, height)
            .round(radius)
            .thickness(2.0F * currentScale)
            .softness(1.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getRect(0.7F))
            .build()
      );
      if (filledWidth > 0) {
         int colorStart = ColorUtil.multDark(-12285697, 0.6F);
         int colorEnd = -10048769;
         if (HotBar.getInstance().getBarsGlow().isValue()) {
            rectangle.render(
               ShapeProperties.create(matrices, x, y, filledWidth, height).round(radius).softness(10.0F).color(ColorUtil.multAlpha(colorEnd, 0.2F)).build()
            );
         }

         rectangle.render(
            ShapeProperties.create(matrices, x + 1, y + 1, filledWidth - 2, height - 2)
               .round(radius - 0.5F)
               .color(colorStart, colorEnd, colorEnd, colorStart)
               .build()
         );
      }

      if (HotBar.getInstance().getBarsText().isValue()) {
         if (armor != lastArmorValue) {
            lastArmorValue = armor;
            cachedArmorText = String.format("%.0f", armor);
         }

         String text = cachedArmorText;
         float textHeight = 12.0F;
         float textY = y + (height - textHeight) / 2.0F;
         Fonts.getSize(12, Fonts.Type.INTER_BOLD).drawCenteredString(matrices, text, x + width / 2.0F, textY + 4.5F, ColorUtil.getText());
      }

      image.setTexture("textures/hotbar/armor.png").render(ShapeProperties.create(matrices, x + 2, y + 1.5F, 6.0, 6.0).color(ColorUtil.getText()).build());
   }

   public static void renderHungerBar(DrawContext context, int x, int y, int width, int height, int radius, float food, float saturation) {
      float totalFood = food + saturation;
      float progress = Math.min(totalFood / 40.0F, 1.0F);
      int filledWidth = (int)(width * progress);
      MatrixStack matrices = context.getMatrices();
      blur.render(
         ShapeProperties.create(matrices, x, y, width, height)
            .round(radius)
            .thickness(2.0F * currentScale)
            .softness(1.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getRect(0.7F))
            .build()
      );
      if (filledWidth > 0) {
         int colorStart = ColorUtil.multDark(-30720, 0.6F);
         int colorEnd = -21965;
         float barX = x + width - filledWidth;
         if (HotBar.getInstance().getBarsGlow().isValue()) {
            rectangle.render(
               ShapeProperties.create(matrices, barX, y, filledWidth, height).round(radius).softness(10.0F).color(ColorUtil.multAlpha(colorEnd, 0.2F)).build()
            );
         }

         rectangle.render(
            ShapeProperties.create(matrices, barX + 1.0F, y + 1, filledWidth - 2, height - 2)
               .round(radius - 0.5F)
               .color(colorEnd, colorStart, colorStart, colorEnd)
               .build()
         );
      }

      if (HotBar.getInstance().getBarsText().isValue()) {
         if (totalFood != lastFoodValue) {
            lastFoodValue = totalFood;
            cachedFoodText = String.format("%.0f", totalFood);
         }

         String text = cachedFoodText;
         float textHeight = 12.0F;
         float textY = y + (height - textHeight) / 2.0F;
         Fonts.getSize(12, Fonts.Type.INTER_BOLD).drawCenteredString(matrices, text, x + width / 2.0F, textY + 5.0F, ColorUtil.getText());
      }

      image.setTexture("textures/hotbar/food.png")
         .render(ShapeProperties.create(matrices, x + width - 8, y + 1.5F, 6.0, 6.0).color(ColorUtil.getText()).build());
   }

   public static void renderAirBar(DrawContext context, int x, int y, int width, int height, int radius, float air, float maxAir) {
      float progress = Math.min(air / maxAir, 1.0F);
      int filledWidth = (int)(width * progress);
      MatrixStack matrices = context.getMatrices();
      blur.render(
         ShapeProperties.create(matrices, x, y, width, height)
            .round(radius)
            .thickness(2.0F * currentScale)
            .softness(1.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getRect(0.7F))
            .build()
      );
      if (filledWidth > 0) {
         int colorStart = ColorUtil.multDark(-16733441, 0.6F);
         int colorEnd = -13382401;
         float barX = x + width - filledWidth;
         if (HotBar.getInstance().getBarsGlow().isValue()) {
            rectangle.render(
               ShapeProperties.create(matrices, barX, y, filledWidth, height).round(radius).softness(10.0F).color(ColorUtil.multAlpha(colorEnd, 0.2F)).build()
            );
         }

         rectangle.render(
            ShapeProperties.create(matrices, barX + 1.0F, y + 1, filledWidth - 2, height - 2)
               .round(radius - 0.5F)
               .color(colorEnd, colorStart, colorStart, colorEnd)
               .build()
         );
      }

      if (HotBar.getInstance().getBarsText().isValue()) {
         if (air != lastAirValue) {
            lastAirValue = air;
            cachedAirText = String.format("%.0f", air);
         }

         String text = cachedAirText;
         float textHeight = 12.0F;
         float textY = y + (height - textHeight) / 2.0F;
         Fonts.getSize(12, Fonts.Type.INTER_BOLD).drawCenteredString(matrices, text, x + width / 2.0F, textY + 4.5F, ColorUtil.getText());
      }

      image.setTexture("textures/hotbar/air.png")
         .render(ShapeProperties.create(matrices, x + width - 8, y + 1.5F, 6.0, 6.0).color(ColorUtil.getText()).build());
   }

   public static void renderExperienceBar(DrawContext context, int x, int y, int width, int height, int radius, float experience, int experienceLevel) {
      float progress = Math.min(experience, 1.0F);
      int filledWidth = (int)(width * progress);
      MatrixStack matrices = context.getMatrices();
      if (filledWidth > 0) {
         int colorStart = ColorUtil.multDark(-16711936, 0.6F);
         int colorEnd = -11141291;
         if (HotBar.getInstance().getBarsGlow().isValue()) {
            rectangle.render(
               ShapeProperties.create(matrices, x, y, filledWidth, height).round(radius).softness(3.0F).color(ColorUtil.multAlpha(colorEnd, 0.2F)).build()
            );
         }

         rectangle.render(
            ShapeProperties.create(matrices, x + 1, y + 1, filledWidth - 2, height - 2)
               .round(radius - 0.5F)
               .color(colorStart, colorEnd, colorEnd, colorStart)
               .build()
         );
      }
   }

   private HotBarStatusRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
