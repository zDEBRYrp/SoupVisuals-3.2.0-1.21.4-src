package padej.soup.implement.menu.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import padej.soup.api.feature.module.setting.implement.TextSetting;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;

public class TextComponent extends AbstractSettingComponent {
   public static TextComponent focused;
   @Deprecated
   public static boolean typing;
   private final TextSetting setting;
   private float rectX;
   private float rectY;
   private float rectWidth;
   private float rectHeight;
   private boolean dragging;
   private int cursorPosition = 0;
   private int selectionStart = -1;
   private int selectionEnd = -1;
   private long lastClickTime = 0L;
   private float xOffset = 0.0F;
   private String text = "";

   private boolean isFocused() {
      return focused == this;
   }

   private int effectiveMax() {
      int max = this.setting.getMax();
      return max <= 0 ? Integer.MAX_VALUE : max;
   }

   private int effectiveMin() {
      int min = this.setting.getMin();
      return Math.max(0, min);
   }

   private void takeFocus() {
      if (focused != this && (this.text == null || this.text.isEmpty())) {
         String s = this.setting.getText();
         this.text = s != null ? s : "";
      }

      focused = this;
      typing = true;
   }

   private void releaseFocus() {
      if (focused == this) {
         focused = null;
         typing = false;
      }
   }

   public TextComponent(TextSetting setting) {
      super(setting);
      this.setting = setting;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.updateVisibilityAnimation();
      boolean isModified = this.setting.isModified();
      float textOffset = isModified ? ResetIconComponent.getTextOffset() : 0.0F;
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(12);
      String wrapped = StringUtil.wrap(this.setting.getLocalizedName(), (int)(this.width - 75.0F - textOffset), 14);
      float wrappedHeight = Fonts.getSize(14).getStringHeight(wrapped);
      float oneLineHeight = Fonts.getSize(14).getStringHeight("");
      this.height = (int)(18.0F + Math.max(0.0F, wrappedHeight - oneLineHeight));
      this.rectX = this.x + this.width - 61.5F;
      this.rectY = this.y + this.height / 2.0F - 6.0F;
      this.rectWidth = 53.0F;
      this.rectHeight = 12.0F;
      rectangle.render(
         ShapeProperties.create(matrix, this.rectX, this.rectY, this.rectWidth, this.rectHeight)
            .round(2.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getGuiRectColor(1.0F))
            .build()
      );
      int textColor = ColorUtil.multAlpha(ColorUtil.getText(), this.currentAlpha);
      int descColor = ColorUtil.multAlpha(ColorUtil.getDescription(), this.currentAlpha);
      this.resetIcon.position(this.x, this.y).alpha(this.currentAlpha).modified(isModified).render(matrix);
      float textX = this.x + 9.0F + textOffset;
      if (isModified) {
         Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawItalicString(context.getMatrices(), wrapped, textX, this.y + 9.0F, textColor);
      } else {
         Fonts.getSize(14, Fonts.Type.INTER_BOLD).drawString(context.getMatrices(), wrapped, textX, this.y + 9.0F, textColor);
      }

      this.updateXOffset(font, this.cursorPosition);
      if (this.isFocused() && this.selectionStart != -1 && this.selectionEnd != -1 && this.selectionStart != this.selectionEnd) {
         int start = Math.max(0, Math.min(this.getStartOfSelection(), this.text.length()));
         int end = Math.max(0, Math.min(this.getEndOfSelection(), this.text.length()));
         if (start < end) {
            float selectionXStart = this.rectX + 3.0F - this.xOffset + font.getStringWidth(this.text.substring(0, start));
            float selectionXEnd = this.rectX + 3.0F - this.xOffset + font.getStringWidth(this.text.substring(0, end));
            float selectionWidth = selectionXEnd - selectionXStart;
            rectangle.render(
               ShapeProperties.create(matrix, selectionXStart, this.rectY + this.rectHeight / 2.0F - 4.0F, selectionWidth, 8.0).color(-11172376).build()
            );
         }
      }

      font.drawString(
         context.getMatrices(),
         this.text,
         this.rectX + 3.0F - this.xOffset,
         this.rectY + this.rectHeight / 2.0F - 1.0F,
         this.isFocused() ? -1 : ColorUtil.getDescription()
      );
      if (!this.isFocused() && this.text.isEmpty()) {
         String s = this.setting.getText();
         this.text = s != null ? s : "";
         font.drawString(context.getMatrices(), this.text, this.rectX + 3.0F, this.rectY + this.rectHeight / 2.0F - 1.0F, ColorUtil.getDescription());
      }

      long currentTime = System.currentTimeMillis();
      boolean cursorBlink = this.isFocused() && currentTime % 1000L < 500L;
      if (cursorBlink && (this.selectionStart == -1 || this.selectionStart == this.selectionEnd)) {
         float cursorX = font.getStringWidth(this.text.substring(0, this.cursorPosition));
         rectangle.render(
            ShapeProperties.create(matrix, this.rectX + 3.0F - this.xOffset + cursorX, this.rectY + this.rectHeight / 2.0F - 3.5F, 0.5, 7.0).color(-1).build()
         );
      }

      if (this.dragging) {
         this.cursorPosition = this.getCursorIndexAt(mouseX);
         if (this.selectionStart == -1) {
            this.selectionStart = this.cursorPosition + 1;
         }

         this.selectionEnd = this.cursorPosition;
      }
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      this.dragging = true;
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.resetIcon.isHovered(mouseX, mouseY)) {
         this.setting.reset();
         this.text = this.setting.getText() != null ? this.setting.getText() : "";
         this.cursorPosition = this.text.length();
         return true;
      } else if (MathUtil.isHovered(mouseX, mouseY, this.rectX, this.rectY, this.rectWidth, this.rectHeight) && button == 0) {
         SoundManager.playSound(SoundManager.CLICK);
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastClickTime < 250L) {
            this.selectionStart = 0;
            this.selectionEnd = this.text.length();
         } else {
            this.takeFocus();
            this.dragging = true;
            this.lastClickTime = currentTime;
            this.cursorPosition = this.getCursorIndexAt(mouseX);
            this.selectionStart = this.cursorPosition;
            this.selectionEnd = this.cursorPosition;
         }

         return true;
      } else {
         this.releaseFocus();
         this.clearSelection();
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean isHover(double mouseX, double mouseY) {
      if (!this.setting.isVisible()) {
         return false;
      } else {
         return MathUtil.isHovered(mouseX, mouseY, this.rectX, this.rectY, this.rectWidth, this.rectHeight)
            ? true
            : MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, this.y + 6.0F, this.width - this.rectWidth - 18.0F, this.height - 12.0F);
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.isFocused() && this.text.length() < this.effectiveMax()) {
         this.deleteSelectedText();
         this.text = this.text.substring(0, this.cursorPosition) + chr + this.text.substring(this.cursorPosition);
         this.cursorPosition++;
         this.clearSelection();
      }

      return super.charTyped(chr, modifiers);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.isFocused()) {
         if (Screen.hasControlDown()) {
            switch (keyCode) {
               case 65:
                  this.selectAllText();
                  break;
               case 67:
                  this.copyToClipboard();
                  break;
               case 86:
                  this.pasteFromClipboard();
            }
         } else {
            switch (keyCode) {
               case 257:
               case 259:
                  this.handleTextModification(keyCode);
               case 258:
               case 260:
               case 261:
               default:
                  break;
               case 262:
               case 263:
                  this.moveCursor(keyCode);
            }
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private void pasteFromClipboard() {
      String clipboardText = GLFW.glfwGetClipboardString(window.getHandle());
      if (clipboardText != null) {
         this.replaceText(this.cursorPosition, this.cursorPosition, clipboardText);
      }
   }

   private void copyToClipboard() {
      if (this.hasSelection()) {
         GLFW.glfwSetClipboardString(window.getHandle(), this.getSelectedText());
      }
   }

   private void selectAllText() {
      this.selectionStart = 0;
      this.selectionEnd = this.text.length();
   }

   private void handleTextModification(int keyCode) {
      if (keyCode == 259) {
         if (this.hasSelection()) {
            this.replaceText(this.getStartOfSelection(), this.getEndOfSelection(), "");
         } else if (this.cursorPosition > 0) {
            this.replaceText(this.cursorPosition - 1, this.cursorPosition, "");
         }
      } else if (keyCode == 257 && this.text.length() >= this.effectiveMin() && this.text.length() <= this.effectiveMax()) {
         this.setting.setText(this.text);
         this.releaseFocus();
      }
   }

   private void moveCursor(int keyCode) {
      if (keyCode == 263 && this.cursorPosition > 0) {
         this.cursorPosition--;
      } else if (keyCode == 262 && this.cursorPosition < this.text.length()) {
         this.cursorPosition++;
      }

      this.updateSelectionAfterCursorMove();
   }

   private void updateSelectionAfterCursorMove() {
      if (Screen.hasShiftDown()) {
         if (this.selectionStart == -1) {
            this.selectionStart = this.cursorPosition;
         }

         this.selectionEnd = this.cursorPosition;
      } else {
         this.clearSelection();
      }
   }

   private void replaceText(int start, int end, String replacement) {
      if (start < 0) {
         start = 0;
      }

      if (end > this.text.length()) {
         end = this.text.length();
      }

      if (start > end) {
         start = end;
      }

      this.text = this.text.substring(0, start) + replacement + this.text.substring(end);
      this.cursorPosition = start + replacement.length();
      this.clearSelection();
   }

   private boolean hasSelection() {
      return this.selectionStart != -1 && this.selectionEnd != -1 && this.selectionStart != this.selectionEnd;
   }

   private String getSelectedText() {
      return this.text.substring(this.getStartOfSelection(), this.getEndOfSelection());
   }

   private int getStartOfSelection() {
      return Math.min(this.selectionStart, this.selectionEnd);
   }

   private int getEndOfSelection() {
      return Math.max(this.selectionStart, this.selectionEnd);
   }

   private void clearSelection() {
      this.selectionStart = -1;
      this.selectionEnd = -1;
   }

   private int getCursorIndexAt(double mouseX) {
      FontRenderer font = Fonts.getSize(12, Fonts.Type.INTER_BOLD);
      float relativeX = (float)mouseX - this.rectX - 3.0F + this.xOffset;

      int position;
      for (position = 0; position < this.text.length(); position++) {
         float textWidth = font.getStringWidth(this.text.substring(0, position + 1));
         if (textWidth > relativeX) {
            break;
         }
      }

      return position;
   }

   private void updateXOffset(FontRenderer font, int cursorPosition) {
      float cursorX = font.getStringWidth(this.text.substring(0, cursorPosition));
      if (cursorX < this.xOffset) {
         this.xOffset = cursorX;
      } else if (cursorX - this.xOffset > this.rectWidth - 7.0F) {
         this.xOffset = cursorX - (this.rectWidth - 7.0F);
      }
   }

   private void deleteSelectedText() {
      if (this.hasSelection()) {
         this.replaceText(this.getStartOfSelection(), this.getEndOfSelection(), "");
      }
   }
}
