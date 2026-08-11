package padej.soup.api.feature.module.setting.implement;

import java.util.function.Supplier;
import padej.soup.api.feature.module.setting.Setting;

public class TextSetting extends Setting {
   private String text = "";
   private String defaultText;
   private int min = 0;
   private int max = Integer.MAX_VALUE;

   public TextSetting(String name, String description) {
      super(name, description);
   }

   public TextSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public TextSetting setText(String text) {
      if (this.defaultText == null) {
         this.defaultText = text;
         this.text = text;
         return this;
      } else {
         boolean changed = !this.text.equals(text);
         this.text = text;
         if (changed) {
            this.notifyChange();
         }

         return this;
      }
   }

   @Override
   public boolean isModified() {
      if (this.defaultText == null) {
         return false;
      } else {
         return this.text == null ? this.defaultText != null : !this.text.equals(this.defaultText);
      }
   }

   @Override
   public void reset() {
      if (this.defaultText != null) {
         this.text = this.defaultText;
      }
   }

   public String getText() {
      return this.text;
   }

   public String getDefaultText() {
      return this.defaultText;
   }

   public int getMin() {
      return this.min;
   }

   public int getMax() {
      return this.max;
   }

   public TextSetting setDefaultText(String defaultText) {
      this.defaultText = defaultText;
      return this;
   }

   public TextSetting setMin(int min) {
      this.min = min;
      return this;
   }

   public TextSetting setMax(int max) {
      this.max = max;
      return this;
   }
}
