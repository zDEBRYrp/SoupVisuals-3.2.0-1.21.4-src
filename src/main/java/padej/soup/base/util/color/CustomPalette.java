package padej.soup.base.util.color;

public class CustomPalette extends ColorPalette {
   private final int mainGui;
   private final int guiRect;
   private final int guiRect2;
   private final int rect;
   private final int rectDarker;
   private final int blurRect;
   private final int text;
   private final int description;

   public CustomPalette(int mainGui, int guiRect, int guiRect2, int rect, int rectDarker, int blurRect, int text, int description) {
      this.mainGui = mainGui;
      this.guiRect = guiRect;
      this.guiRect2 = guiRect2;
      this.rect = rect;
      this.rectDarker = rectDarker;
      this.blurRect = blurRect;
      this.text = text;
      this.description = description;
   }

   @Override
   public int mainGuiColor() {
      return this.mainGui;
   }

   @Override
   public int guiRectColor() {
      return this.guiRect;
   }

   @Override
   public int guiRectColor2() {
      return this.guiRect2;
   }

   @Override
   public int rectColor() {
      return this.rect;
   }

   @Override
   public int rectDarkerColor() {
      return this.rectDarker;
   }

   @Override
   public int textColor() {
      return this.text;
   }

   @Override
   public int descriptionColor() {
      return this.description;
   }

   @Override
   public int blurRectColor() {
      return this.blurRect;
   }

   @Override
   public int outlineColor() {
      return this.isDark() ? -12961214 : -3355444;
   }

   @Override
   public int friendColor() {
      return this.isDark() ? -11870592 : -14498466;
   }

   @Override
   public String getName() {
      return "Custom";
   }

   @Override
   public boolean isDark() {
      int r = this.mainGui >> 16 & 0xFF;
      int g = this.mainGui >> 8 & 0xFF;
      int b = this.mainGui & 0xFF;
      int brightness = (r + g + b) / 3;
      return brightness < 128;
   }
}
