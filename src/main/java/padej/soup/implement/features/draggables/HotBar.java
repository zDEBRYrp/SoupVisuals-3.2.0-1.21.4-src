package padej.soup.implement.features.draggables;

import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.pipeline.HudRenderPipeline;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.core.perftest.HudProfiler;

public class HotBar extends AbstractDraggable {
   private float selectItemX;
   private final String[] cachedBindNames = new String[9];
   private boolean cachedShowBinds = false;
   private boolean cachedIsSpectator = false;
   private boolean cachedIsCreative = false;
   private String cachedExpLevel = "0";

   public HotBar() {
      super("HotBar", 0, 50, 182, 22, false);
   }

   @Override
   public void tick() {
      if (mc.player != null) {
         this.cachedShowBinds = padej.soup.implement.features.modules.hud.HotBar.getInstance().getShowBinds().isValue();
         this.cachedIsSpectator = mc.player.isSpectator();
         this.cachedIsCreative = mc.player.isCreative();
         this.cachedExpLevel = String.valueOf(mc.player.experienceLevel);
         if (this.cachedShowBinds && mc.options.hotbarKeys != null) {
            FontRenderer bindFont = Fonts.getSize(11, Fonts.Type.SF_BOLD);
            float maxLabelWidth = 16.0F;

            for (int i = 0; i < 9 && i < mc.options.hotbarKeys.length; i++) {
               String name = this.getCompactHotbarBindName(mc.options.hotbarKeys[i]);
               if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("NONE")) {
                  while (bindFont.getStringWidth(name) > 16.0F && name.length() > 1) {
                     name = name.substring(0, name.length() - 1);
                  }
               }

               this.cachedBindNames[i] = name;
            }
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      PlayerInventory inventory = Objects.requireNonNull(mc.player).getInventory();
      ItemStack offHand = mc.player.getOffHandStack();
      float scale = this.getDraggableScale();
      this.selectItemX = MathUtil.interpolateSmooth(1.0, this.selectItemX, (float)(inventory.selectedSlot * 20));
      this.setX((mc.getWindow().getScaledWidth() - this.getWidth()) / 2);
      this.setY(mc.getWindow().getScaledHeight() - 27);
      long t0 = HudProfiler.nano();
      blur.render(
         ShapeProperties.create(matrix, this.getX() - 0.5F, this.getY() - 0.5F, this.getWidth() + 1, 23.0)
            .round(3.0F)
            .thickness(2.0F * scale)
            .softness(1.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getBlurRect(0.7F))
            .build()
      );
      rectangle.render(
         ShapeProperties.create(matrix, this.getX() + this.selectItemX + 1.0F, this.getY() + 1, 20.0, 20.0)
            .round(2.25F)
            .thickness(3.0F * scale)
            .outlineColor(ColorUtil.getClientColor())
            .color(ColorUtil.getRect(0.0F))
            .build()
      );
      HudProfiler.recordComponent("HB:BG", t0);
      if (this.cachedShowBinds) {
         for (int i = 0; i < 9; i++) {
            this.drawHotbarBind(context, i, this.getX() + i * 20 + 2, this.getY() + 2);
         }
      }

      long ti = HudProfiler.nano();
      float offHandX = !offHand.isEmpty() ? this.getX() + (Objects.requireNonNull(mc.player).getMainArm().equals(Arm.RIGHT) ? -28 : 198) : Float.NaN;
      float hotbarY = this.getY() + 2;
      float hotbarX = this.getX();
      Matrix4f matrixSnapshot = new Matrix4f(context.getMatrices().peek().getPositionMatrix());
      Matrix3f normalSnapshot = new Matrix3f(context.getMatrices().peek().getNormalMatrix());
      HudRenderPipeline.getInstance()
         .recordVanilla(
            () -> {
               context.getMatrices().push();
               context.getMatrices().peek().getPositionMatrix().set(matrixSnapshot);
               context.getMatrices().peek().getNormalMatrix().set(normalSnapshot);

               try {
                  for (int i = 0; i < 9; i++) {
                     ItemStack stack = (ItemStack)inventory.main.get(i);
                     if (!stack.isEmpty()) {
                        context.getMatrices().push();
                        context.getMatrices().translate(hotbarX + i * 20 + 2.0F + 1.0F, hotbarY + 1.0F, 0.0F);
                        context.drawItem(stack, 0, 0);
                        context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
                        context.draw();
                        context.getMatrices().pop();
                     }
                  }

                  if (!offHand.isEmpty()) {
                     blur.render(
                        ShapeProperties.create(context.getMatrices(), offHandX - 2.5F, hotbarY - 2.5F, 23.0, 23.0)
                           .round(3.0F)
                           .thickness(2.0F * scale)
                           .softness(1.0F)
                           .outlineColor(ColorUtil.getOutline())
                           .color(ColorUtil.getBlurRect(0.7F))
                           .build()
                     );
                     context.getMatrices().push();
                     context.getMatrices().translate(offHandX + 1.0F, hotbarY + 1.0F, 0.0F);
                     context.drawItem(offHand, 0, 0);
                     context.drawStackOverlay(mc.textRenderer, offHand, 0, 0);
                     context.draw();
                     context.getMatrices().pop();
                  }
               } finally {
                  context.getMatrices().pop();
               }
            },
            HudRenderPipeline.VanillaLayer.AFTER_RECT
         );
      HudProfiler.recordComponent("HB:Items", ti);
      long te = HudProfiler.nano();
      if (!this.cachedIsSpectator && !this.cachedIsCreative) {
         this.drawExperienceBar(matrix);
      }

      this.drawOverlayInfo(matrix);
      HudProfiler.recordComponent("HB:Overlay", te);
   }

   public void drawExperienceBar(MatrixStack matrix) {
      Fonts.getSize(16).drawCenteredString(matrix, this.cachedExpLevel, window.getScaledWidth() / 2.0F - 1.0F, this.getY() - 14, ColorUtil.GREEN);
   }

   public void drawOverlayInfo(MatrixStack matrix) {
      float scaledWidth = mc.getWindow().getScaledWidth() / 2.0F;
      float heightStart = mc.getWindow().getScaledHeight() - 75;
      float paddingX = 4.0F;
      float paddingY = 3.0F;
      FontRenderer font = Fonts.getSize(14, Fonts.Type.INTER_DEFAULT);
      if (mc.inGameHud.heldItemTooltipFade > 0 && mc.inGameHud.currentStack != null) {
         float alpha = mc.inGameHud.heldItemTooltipFade * 256.0F / 10.0F / 255.0F;
         Text text = mc.inGameHud.currentStack.getName();
         float width = font.getStringWidth(text);
         int x = (int)(scaledWidth - width / 2.0F);
         MathUtil.setAlpha(
            alpha,
            () -> {
               blur.render(
                  ShapeProperties.create(matrix, x - paddingX, heightStart - paddingY, width + paddingX * 2.0F, font.getStringHeight(text) + paddingY * 2.0F)
                     .round(2.5F)
                     .color(ColorUtil.getBlurRect(0.7F))
                     .build()
               );
               font.drawText(matrix, text, x, heightStart + 2.5F);
            }
         );
      }

      if (mc.inGameHud.overlayRemaining > 0 && mc.inGameHud.overlayMessage != null && !mc.inGameHud.overlayMessage.getString().isEmpty()) {
         float alpha = mc.inGameHud.overlayRemaining * 256.0F / 10.0F / 255.0F;
         Text text = mc.inGameHud.overlayMessage;
         float width = font.getStringWidth(text);
         int x = (int)(scaledWidth - width / 2.0F);
         MathUtil.setAlpha(
            alpha,
            () -> {
               blur.render(
                  ShapeProperties.create(
                        matrix, x - paddingX, heightStart - paddingY - 17.0F, width + paddingX * 2.0F, font.getStringHeight(text) + paddingY * 2.0F
                     )
                     .round(2.5F)
                     .color(ColorUtil.getBlurRect(0.7F))
                     .build()
               );
               font.drawText(matrix, text, x, heightStart - 14.5F);
            }
         );
      }
   }

   public void drawStack(DrawContext context, ItemStack stack, float x, float y, boolean offHand) {
      if (!stack.isEmpty() || offHand) {
         if (offHand) {
            blur.render(
               ShapeProperties.create(context.getMatrices(), x - 2.5F, y - 2.5F, 23.0, 23.0)
                  .round(3.0F)
                  .thickness(2.0F * this.getDraggableScale())
                  .softness(1.0F)
                  .outlineColor(ColorUtil.getOutline())
                  .color(ColorUtil.getBlurRect(0.7F))
                  .build()
            );
         }

         Matrix4f posMat = new Matrix4f(context.getMatrices().peek().getPositionMatrix());
         Matrix3f nrmMat = new Matrix3f(context.getMatrices().peek().getNormalMatrix());
         HudRenderPipeline.getInstance().recordVanilla(() -> {
            context.getMatrices().push();
            context.getMatrices().peek().getPositionMatrix().set(posMat);
            context.getMatrices().peek().getNormalMatrix().set(nrmMat);
            context.getMatrices().push();
            context.getMatrices().translate(x + 1.0F, y + 1.0F, 0.0F);
            context.drawItem(stack, 0, 0);
            context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
            context.draw();
            context.getMatrices().pop();
            context.getMatrices().pop();
         }, HudRenderPipeline.VanillaLayer.AFTER_RECT);
      }
   }

   private void drawHotbarBind(DrawContext context, int slotIndex, float x, float y) {
      String keyName = this.cachedBindNames[slotIndex];
      if (keyName != null && !keyName.isEmpty() && !keyName.equalsIgnoreCase("NONE")) {
         MatrixStack matrices = context.getMatrices();
         matrices.push();
         FontRenderer font = Fonts.getSize(11, Fonts.Type.SF_BOLD);
         float textX = x + 1.0F;
         float textY = y + 2.0F;
         matrices.translate(textX, textY, 250.0F);
         font.drawString(matrices, keyName, 0.0, 0.0, ColorUtil.getDescription());
         matrices.pop();
      }
   }

   private String getCompactHotbarBindName(KeyBinding keyBinding) {
      String translation = keyBinding.getBoundKeyTranslationKey();
      if (translation != null && !translation.isEmpty()) {
         String key = translation.toLowerCase(Locale.ROOT);
         if (key.startsWith("key.keyboard.")) {
            key = key.substring("key.keyboard.".length());
         } else if (key.startsWith("key.mouse.")) {
            key = key.substring("key.mouse.".length());
         }
         return switch (key) {
            case "page.up" -> "PGUP";
            case "page.down" -> "PGDN";
            case "home" -> "HOME";
            case "end" -> "END";
            case "pause" -> "PAUSE";
            case "insert" -> "INS";
            case "delete" -> "DEL";
            case "print.screen" -> "PRTSC";
            case "scroll.lock" -> "SCRLK";
            case "caps.lock" -> "CAPS";
            case "left.shift" -> "LSH";
            case "right.shift" -> "RSH";
            case "left.control" -> "LCTL";
            case "right.control" -> "RCTL";
            case "left.alt" -> "LALT";
            case "right.alt" -> "RALT";
            case "space" -> "SPC";
            case "escape" -> "ESC";
            case "enter" -> "ENT";
            case "backspace" -> "BKSP";
            case "tab" -> "TAB";
            case "apostrophe" -> "'";
            case "semicolon" -> ";";
            case "comma" -> ",";
            case "period" -> ".";
            case "slash" -> "/";
            case "backslash" -> "\\";
            case "left.bracket" -> "[";
            case "right.bracket" -> "]";
            case "grave.accent" -> "`";
            case "minus" -> "-";
            case "equal" -> "=";
            default -> {
               if (key.startsWith("keypad.")) {
                  String keypadKey = key.substring("keypad.".length()).toUpperCase(Locale.ROOT).replace(".", "");
                  yield "KP" + keypadKey;
               } else {
                  yield key.toUpperCase(Locale.ROOT).replace(".", "");
               }
            }
         };
      } else {
         String localized = keyBinding.getBoundKeyLocalizedText().getString();
         return localized == null ? "" : this.convertRussianToEnglish(localized).toUpperCase(Locale.ROOT).replace(" ", "");
      }
   }

   private String convertRussianToEnglish(String text) {
      if (text != null && !text.isEmpty()) {
         String russian = "йцукенгшщзхъфывапролджэячсмитьбю.ЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ,";
         String english = "qwertyuiop[]asdfghjkl;'zxcvbnm,./QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>?";
         StringBuilder result = new StringBuilder();

         for (char c : text.toCharArray()) {
            int index = russian.indexOf(c);
            if (index != -1) {
               result.append(english.charAt(index));
            } else {
               result.append(c);
            }
         }

         return result.toString();
      } else {
         return text;
      }
   }
}
