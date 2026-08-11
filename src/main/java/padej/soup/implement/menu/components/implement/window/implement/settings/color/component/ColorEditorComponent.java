package padej.soup.implement.menu.components.implement.window.implement.settings.color.component;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.menu.components.AbstractComponent;

public class ColorEditorComponent extends AbstractComponent {
   private final ColorSetting setting;
   private boolean typing = false;
   private String hexInput = "";
   private int cursorPosition = 0;
   private int selectionStart = -1;
   private int selectionEnd = -1;
   private float xOffset = 0.0F;
   private boolean dragging = false;
   private long lastClickTime = 0L;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      rectangle.render(
         ShapeProperties.create(matrix, this.x + 6.0F, this.y + 90.5F, 31.0, 14.0)
            .round(1.5F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline(0.8F))
            .color(ColorUtil.getRect(1.0F))
            .build()
      );
      Fonts.getSize(13).drawString(context.getMatrices(), LocalizationManager.getInstance().get("ui.hex"), this.x + 10.0F, this.y + 96.0F, -1);
      float hexFieldX = this.x + 40.0F;
      float hexFieldY = this.y + 90.5F;
      float hexFieldWidth = 80.0F;
      float hexFieldHeight = 14.0F;
      rectangle.render(
         ShapeProperties.create(matrix, hexFieldX, hexFieldY, hexFieldWidth, hexFieldHeight)
            .round(1.5F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline(0.8F))
            .color(ColorUtil.getRect(1.0F))
            .build()
      );
      String currentHex = String.format("%08X", this.setting.getColor()).toUpperCase();
      if (!this.typing) {
         this.hexInput = currentHex;
      }

      this.updateXOffset(Fonts.getSize(13), this.cursorPosition);
      FontRenderer font = Fonts.getSize(13);
      float textStartX = hexFieldX + 5.0F - this.xOffset;
      float textY = hexFieldY + 5.5F;
      float selectionY = hexFieldY + 2.0F;
      float selectionHeight = 9.0F;
      if (this.typing && this.selectionStart != -1 && this.selectionEnd != -1 && this.selectionStart != this.selectionEnd) {
         int start = Math.max(0, Math.min(this.getStartOfSelection(), this.hexInput.length()));
         int end = Math.max(0, Math.min(this.getEndOfSelection(), this.hexInput.length()));
         if (start < end) {
            String textBeforeSelection = "#" + this.hexInput.substring(0, start);
            String textUpToEndSelection = "#" + this.hexInput.substring(0, end);
            float selectionXStart = textStartX + font.getStringWidth(textBeforeSelection);
            float selectionXEnd = textStartX + font.getStringWidth(textUpToEndSelection);
            float selectionWidth = selectionXEnd - selectionXStart;
            rectangle.render(ShapeProperties.create(matrix, selectionXStart, selectionY, selectionWidth, selectionHeight).color(-11172376).build());
         }
      }

      String displayText = "#" + this.hexInput;
      font.drawString(context.getMatrices(), displayText, textStartX, textY, this.typing ? -1 : ColorUtil.getDescription());
      long currentTime = System.currentTimeMillis();
      boolean focused = this.typing && currentTime % 1000L < 500L;
      if (focused && (this.selectionStart == -1 || this.selectionStart == this.selectionEnd)) {
         float cursorX = Fonts.getSize(13).getStringWidth("#" + this.hexInput.substring(0, this.cursorPosition));
         rectangle.render(ShapeProperties.create(matrix, hexFieldX + 5.0F - this.xOffset + cursorX, selectionY, 0.5, selectionHeight).color(-1).build());
      }

      rectangle.render(
         ShapeProperties.create(matrix, this.x + 122.0F, this.y + 90.5F, 22.0, 14.0)
            .round(1.5F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline(0.8F))
            .color(ColorUtil.getRect(1.0F))
            .build()
      );
      int displayValue = (int)(this.setting.getAlpha() * 100.0F);
      Fonts.getSize(13).drawCenteredString(context.getMatrices(), displayValue + "%", this.x + 133.0F, this.y + 96.0F, -1);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (MathUtil.isHovered(mouseX, mouseY, this.x + 122.0F, this.y + 90.5F, 22.0, 14.0)) {
         this.setting.setAlpha(MathHelper.clamp((float)(this.setting.getAlpha() - amount * 2.0 / 100.0), 0.0F, 1.0F));
      }

      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float hexFieldX = this.x + 40.0F;
      float hexFieldY = this.y + 90.5F;
      float hexFieldWidth = 80.0F;
      float hexFieldHeight = 14.0F;
      if (MathUtil.isHovered(mouseX, mouseY, hexFieldX, hexFieldY, hexFieldWidth, hexFieldHeight) && button == 0) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastClickTime < 250L) {
            this.selectionStart = 0;
            this.selectionEnd = this.hexInput.length();
         } else {
            this.typing = true;
            this.lastClickTime = currentTime;
            this.cursorPosition = this.getCursorIndexAt(mouseX, hexFieldX);
            this.selectionStart = this.cursorPosition;
            this.selectionEnd = this.cursorPosition;
         }

         return true;
      } else {
         this.typing = false;
         this.clearSelection();
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.typing) {
         this.dragging = true;
         float hexFieldX = this.x + 40.0F;
         this.cursorPosition = this.getCursorIndexAt(mouseX, hexFieldX);
         if (this.selectionStart == -1) {
            this.selectionStart = this.cursorPosition;
         }

         this.selectionEnd = this.cursorPosition;
      }

      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.typing && this.hexInput.length() < 8 && (chr >= '0' && chr <= '9' || chr >= 'A' && chr <= 'F' || chr >= 'a' && chr <= 'f')) {
         this.deleteSelectedText();
         this.hexInput = this.hexInput.substring(0, this.cursorPosition) + Character.toUpperCase(chr) + this.hexInput.substring(this.cursorPosition);
         this.cursorPosition++;
         this.clearSelection();
      }

      return super.charTyped(chr, modifiers);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.typing) {
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

   private void handleTextModification(int keyCode) {
      if (keyCode == 259) {
         if (this.hasSelection()) {
            this.replaceText(this.getStartOfSelection(), this.getEndOfSelection(), "");
         } else if (this.cursorPosition > 0) {
            this.replaceText(this.cursorPosition - 1, this.cursorPosition, "");
         }
      } else if (keyCode == 257) {
         this.applyHexColor();
         this.typing = false;
      }
   }

   private void applyHexColor() {
      try {
         if (this.hexInput.length() >= 6) {
            long colorValue = Long.parseLong(this.hexInput, 16);
            this.setting.setColor((int)colorValue);
         }
      } catch (NumberFormatException var3) {
         this.hexInput = String.format("%08X", this.setting.getColor()).toUpperCase();
      }
   }

   private void moveCursor(int keyCode) {
      if (keyCode == 263 && this.cursorPosition > 0) {
         this.cursorPosition--;
      } else if (keyCode == 262 && this.cursorPosition < this.hexInput.length()) {
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

   private void pasteFromClipboard() {
      String clipboardText = GLFW.glfwGetClipboardString(window.getHandle());
      if (clipboardText != null) {
         StringBuilder filtered = new StringBuilder();

         for (char c : clipboardText.toCharArray()) {
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f') {
               filtered.append(Character.toUpperCase(c));
            }

            if (filtered.length() >= 8) {
               break;
            }
         }

         this.replaceText(this.cursorPosition, this.cursorPosition, filtered.toString());
      }
   }

   private void copyToClipboard() {
      if (this.hasSelection()) {
         GLFW.glfwSetClipboardString(window.getHandle(), this.getSelectedText());
      }
   }

   private void selectAllText() {
      this.selectionStart = 0;
      this.selectionEnd = this.hexInput.length();
   }

   private void replaceText(int start, int end, String replacement) {
      if (start < 0) {
         start = 0;
      }

      if (end > this.hexInput.length()) {
         end = this.hexInput.length();
      }

      if (start > end) {
         start = end;
      }

      int maxLength = 8 - (this.hexInput.length() - (end - start));
      if (replacement.length() > maxLength) {
         replacement = replacement.substring(0, maxLength);
      }

      this.hexInput = this.hexInput.substring(0, start) + replacement + this.hexInput.substring(end);
      this.cursorPosition = start + replacement.length();
      this.clearSelection();
   }

   private boolean hasSelection() {
      return this.selectionStart != -1 && this.selectionEnd != -1 && this.selectionStart != this.selectionEnd;
   }

   private String getSelectedText() {
      return this.hexInput.substring(this.getStartOfSelection(), this.getEndOfSelection());
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

   private int getCursorIndexAt(double mouseX, float hexFieldX) {
      float relativeX = (float)mouseX - hexFieldX - 5.0F + this.xOffset;
      float prefixWidth = Fonts.getSize(13).getStringWidth("#");
      relativeX -= prefixWidth;
      if (relativeX <= 0.0F) {
         return 0;
      } else {
         FontRenderer font = Fonts.getSize(13);
         int low = 0;
         int high = this.hexInput.length();

         while (low < high) {
            int mid = (low + high + 1) / 2;
            float width = font.getStringWidth(this.hexInput.substring(0, mid));
            if (width <= relativeX) {
               low = mid;
            } else {
               high = mid - 1;
            }
         }

         return low;
      }
   }

   private void updateXOffset(FontRenderer font, int cursorPosition) {
      float cursorX = font.getStringWidth("#" + this.hexInput.substring(0, cursorPosition));
      float hexFieldWidth = 80.0F;
      if (cursorX < this.xOffset) {
         this.xOffset = cursorX;
      } else if (cursorX - this.xOffset > hexFieldWidth - 10.0F) {
         this.xOffset = cursorX - (hexFieldWidth - 10.0F);
      }
   }

   private void deleteSelectedText() {
      if (this.hasSelection()) {
         this.replaceText(this.getStartOfSelection(), this.getEndOfSelection(), "");
      }
   }

   public ColorEditorComponent(ColorSetting setting) {
      this.setting = setting;
   }
}
