package padej.soup.api.feature.module.setting.implement;

import java.util.function.Supplier;
import padej.soup.api.feature.module.setting.Setting;

public class BindSetting extends Setting {
   private int key = -1;
   private Integer defaultKey;
   private int type = 1;

   public BindSetting(String name, String description) {
      super(name, description);
   }

   public BindSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public BindSetting setKey(int key) {
      if (this.defaultKey == null) {
         this.defaultKey = key;
      }

      this.key = key;
      this.notifyChange();
      return this;
   }

   @Override
   public boolean isModified() {
      return this.defaultKey == null ? false : this.key != this.defaultKey;
   }

   @Override
   public void reset() {
      if (this.defaultKey != null) {
         this.key = this.defaultKey;
      }
   }

   public int getKey() {
      return this.key;
   }

   public Integer getDefaultKey() {
      return this.defaultKey;
   }

   public int getType() {
      return this.type;
   }

   public BindSetting setDefaultKey(Integer defaultKey) {
      this.defaultKey = defaultKey;
      return this;
   }

   public BindSetting setType(int type) {
      this.type = type;
      return this;
   }
}
