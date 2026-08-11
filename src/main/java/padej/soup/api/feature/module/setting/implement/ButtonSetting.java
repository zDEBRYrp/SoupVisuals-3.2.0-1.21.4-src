package padej.soup.api.feature.module.setting.implement;

import java.util.function.Supplier;
import padej.soup.api.feature.module.setting.Setting;

public class ButtonSetting extends Setting {
   private Runnable runnable;
   private String buttonName;

   public ButtonSetting(String name, String description) {
      super(name, description);
   }

   public ButtonSetting onClick(Runnable action) {
      this.runnable = action;
      return this;
   }

   public ButtonSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   @Override
   public boolean isModified() {
      return false;
   }

   @Override
   public void reset() {
   }

   public Runnable getRunnable() {
      return this.runnable;
   }

   public String getButtonName() {
      return this.buttonName;
   }

   public ButtonSetting setRunnable(Runnable runnable) {
      this.runnable = runnable;
      return this;
   }

   public ButtonSetting setButtonName(String buttonName) {
      this.buttonName = buttonName;
      return this;
   }
}
