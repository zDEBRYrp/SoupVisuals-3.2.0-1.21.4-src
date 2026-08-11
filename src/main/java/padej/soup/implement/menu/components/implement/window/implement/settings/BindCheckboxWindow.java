package padej.soup.implement.menu.components.implement.window.implement.settings;

import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.implement.menu.components.implement.window.implement.AbstractBindWindow;

public class BindCheckboxWindow extends AbstractBindWindow {
   private final BooleanSetting setting;

   @Override
   protected int getKey() {
      return this.setting.getKey();
   }

   @Override
   protected void setKey(int key) {
      this.setting.setKey(key);
   }

   @Override
   protected int getType() {
      return this.setting.getType();
   }

   @Override
   protected void setType(int type) {
      this.setting.setType(type);
   }

   public BindCheckboxWindow(BooleanSetting setting) {
      this.setting = setting;
   }
}
