package padej.soup.base.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.modules.hud.ItemOverlay;

public final class ItemOverlayRenderer implements QuickImports {
   private static final StringBuilder COUNT_SB = new StringBuilder(8);
   private static FontRenderer cachedCountFont;
   private static FontRenderer cachedCooldownFont;

   public static void prepareBatch() {
      cachedCountFont = Fonts.getSize(18, Fonts.Type.SF_BOLD);
      cachedCooldownFont = Fonts.getSize(14, Fonts.Type.SF_BOLD);
   }

   public static boolean renderItemBar(DrawContext context, ItemStack stack, int x, int y) {
      boolean useBatch = ItemBatchMode.isActive();
      boolean enabled;
      boolean customDura;
      if (useBatch) {
         enabled = ItemBatchMode.cachedEnabled;
         customDura = ItemBatchMode.cachedCustomDuraBar;
      } else {
         ItemOverlay io = ItemOverlay.getInstance();
         enabled = io.isEnabled();
         customDura = enabled && io.getCustomDurabilityBar().isValue();
      }

      if (!enabled || !customDura) {
         return false;
      } else if (!stack.isItemBarVisible()) {
         return false;
      } else {
         int barX = x + 2;
         int barY = y + 13;
         int barWidth = stack.getItemBarStep();
         boolean drawShadow = useBatch ? ItemBatchMode.cachedDuraShadow : ItemOverlay.getInstance().getDurabilityBarShadow().isValue();
         int barColor = useBatch ? getBarColorCached() : getBarColorFallback();
         if (drawShadow) {
            context.fill(RenderLayer.getGui(), barX, barY, barX + 13, barY + 2, 200, -16777216);
         }

         context.fill(RenderLayer.getGui(), barX, barY, barX + barWidth, barY + 1, 200, barColor);
         return true;
      }
   }

   private static int getBarColorCached() {
      int[] colors = ItemBatchMode.cachedBarColors;
      if (colors == null) {
         return ColorUtil.multAlpha(ColorUtil.getClientColor(), 1.0F);
      } else {
         return "Wave".equals(ItemBatchMode.cachedBarAnimation) ? getWaveColor(colors) : ColorUtil.multAlpha(colors[0], 1.0F);
      }
   }

   private static int getBarColorFallback() {
      ItemOverlay io = ItemOverlay.getInstance();
      int[] colors = io.getCustomBarColors();
      if (colors == null) {
         return ColorUtil.multAlpha(ColorUtil.getClientColor(), 1.0F);
      } else {
         return "Wave".equals(io.getBarColorAnimation().getSelected()) ? getWaveColor(colors) : ColorUtil.multAlpha(colors[0], 1.0F);
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

   public static boolean renderStackCount(DrawContext context, TextRenderer textRenderer, ItemStack stack, int x, int y, @Nullable String stackCountText) {
      boolean useBatch = ItemBatchMode.isActive();
      boolean enabled;
      boolean customCount;
      if (useBatch) {
         enabled = ItemBatchMode.cachedEnabled;
         customCount = ItemBatchMode.cachedCustomStackCount;
      } else {
         ItemOverlay io = ItemOverlay.getInstance();
         enabled = io.isEnabled();
         customCount = enabled && io.getCustomStackCount().isValue();
      }

      if (enabled && customCount) {
         if (stack.getCount() == 1 && stackCountText == null) {
            return false;
         } else {
            String string;
            if (stackCountText != null) {
               string = stackCountText;
            } else {
               COUNT_SB.setLength(0);
               boolean addPrefix = useBatch ? ItemBatchMode.cachedAddPrefix : ItemOverlay.getInstance().getAddPrefix().isValue();
               if (addPrefix) {
                  COUNT_SB.append('x');
               }

               COUNT_SB.append(stack.getCount());
               string = COUNT_SB.toString();
            }

            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(0.0F, 0.0F, 200.0F);
            int color = useBatch
               ? ItemBatchMode.cachedStackCountColor
               : (ItemOverlay.getInstance().getEnableCustomColor().isValue() ? ItemOverlay.getInstance().getStackCountColor().getColor() : ColorUtil.getText());
            boolean useCustomFont = useBatch ? ItemBatchMode.cachedUseCustomFont : ItemOverlay.getInstance().getUseCustomFont().isValue();
            if (useCustomFont) {
               FontRenderer font = useBatch && cachedCountFont != null ? cachedCountFont : Fonts.getSize(18, Fonts.Type.SF_BOLD);
               int textX = x + 19 - 2 - (int)font.getStringWidth(string);
               int textY = y + 6 + 4;
               font.drawString(matrices, string, textX, textY, color);
            } else {
               RenderSystem.disableDepthTest();
               int textX = x + 19 - 2 - textRenderer.getWidth(string);
               int textY = y + 6 + 3;
               context.drawText(textRenderer, string, textX, textY, color, true);
            }

            matrices.pop();
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean renderCooldownProgress(DrawContext context, int x, int y, float cooldownProgress) {
      boolean useBatch = ItemBatchMode.isActive();
      boolean enabled;
      boolean customCooldown;
      if (useBatch) {
         enabled = ItemBatchMode.cachedEnabled;
         customCooldown = ItemBatchMode.cachedCustomCooldown;
      } else {
         ItemOverlay io = ItemOverlay.getInstance();
         enabled = io.isEnabled();
         customCooldown = enabled && io.getCustomCooldown().isValue();
      }

      if (!enabled || !customCooldown) {
         return false;
      } else if (cooldownProgress <= 0.0F) {
         return false;
      } else {
         String cooldownStyle = useBatch ? ItemBatchMode.cachedCooldownStyle : ItemOverlay.getInstance().getCooldownStyle().getSelected();
         if ("Ring".equals(cooldownStyle)) {
            int color = useBatch ? getCooldownColorCached() : getCooldownColorFallback();
            renderCooldownRing(context, x, y, cooldownProgress, color);
         } else {
            int color = useBatch ? getCooldownColorCached() : getCooldownColorFallback();
            renderCooldownBar(context, x, y, cooldownProgress, color);
         }

         boolean showNumber = useBatch ? ItemBatchMode.cachedShowCooldownNumber : ItemOverlay.getInstance().getShowCooldownNumber().isValue();
         if (showNumber) {
            renderCooldownNumber(context, x, y, cooldownProgress);
         }

         return true;
      }
   }

   private static int getCooldownColorCached() {
      return ColorUtil.replAlpha(ItemBatchMode.cachedCooldownBaseColor, ItemBatchMode.cachedCooldownAlpha);
   }

   private static int getCooldownColorFallback() {
      ItemOverlay io = ItemOverlay.getInstance();
      int baseColor = io.getCooldownColorMode().isSelected("Custom") ? io.getCooldownColor().getColor() : ColorUtil.getClientColor();
      return ColorUtil.replAlpha(baseColor, io.getCooldownAlpha().getValue());
   }

   private static void renderCooldownRing(DrawContext context, int x, int y, float cooldownProgress, int color) {
      if (!(cooldownProgress < 0.01F)) {
         arc.render(
            ShapeProperties.create(context.getMatrices(), x - 2, y - 2, 20.0, 20.0)
               .round(0.27F)
               .thickness(0.25F)
               .end(cooldownProgress * 360.0F)
               .color(color)
               .build()
         );
      }
   }

   private static void renderCooldownBar(DrawContext context, int x, int y, float cooldownProgress, int color) {
      if (!(cooldownProgress < 0.01F)) {
         MatrixStack matrices = context.getMatrices();
         matrices.push();
         matrices.translate(x, y, 0.0F);
         matrices.translate(0.0F, 16.0F * (1.0F - cooldownProgress), 0.0F);
         matrices.scale(1.0F, cooldownProgress, 1.0F);
         context.fill(RenderLayer.getGui(), 0, 0, 16, 16, 200, color);
         matrices.pop();
      }
   }

   private static void renderCooldownNumber(DrawContext context, int x, int y, float remainingSeconds) {
      String text = String.format("%.1f", remainingSeconds);
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(0.0F, 0.0F, 200.0F);
      FontRenderer font = ItemBatchMode.isActive() && cachedCooldownFont != null ? cachedCooldownFont : Fonts.getSize(14, Fonts.Type.SF_BOLD);
      float textWidth = font.getStringWidth(text);
      float textX = x + 8.0F - textWidth / 2.0F;
      float textY = y + 8.0F;
      font.drawString(matrices, text, textX, textY, ColorUtil.getText());
      matrices.pop();
   }

   private ItemOverlayRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
