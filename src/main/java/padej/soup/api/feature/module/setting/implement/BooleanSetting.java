package padej.soup.api.feature.module.setting.implement;

import java.util.function.Consumer;
import java.util.function.Supplier;
import padej.soup.api.feature.module.setting.Setting;

public class BooleanSetting extends Setting {
   private boolean value;
   private Boolean defaultValue;
   private int key = -1;
   private int type = 1;
   private Consumer<Boolean> onChangeCallback;

   public BooleanSetting(String name, String description) {
      super(name, description);
   }

   public BooleanSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public BooleanSetting onChange(Consumer<Boolean> callback) {
      this.onChangeCallback = callback;
      return this;
   }

   public BooleanSetting setValue(boolean value) {
      if (this.defaultValue == null) {
         this.defaultValue = value;
         this.value = value;
         return this;
      } else {
         boolean changed = this.value != value;
         this.value = value;
         if (changed) {
            this.notifyChange();
            if (this.onChangeCallback != null) {
               this.onChangeCallback.accept(value);
            }
         }

         return this;
      }
   }

   @Override
   public boolean isModified() {
      return this.defaultValue == null ? false : this.value != this.defaultValue;
   }

   @Override
   public void reset() {
      if (this.defaultValue != null) {
         this.setValue(this.defaultValue);
      }
   }

   public boolean isValue() {
      return this.value;
   }

   public Boolean getDefaultValue() {
      return this.defaultValue;
   }

   public int getKey() {
      return this.key;
   }

   public int getType() {
      return this.type;
   }

   public Consumer<Boolean> getOnChangeCallback() {
      return this.onChangeCallback;
   }

   public BooleanSetting setDefaultValue(Boolean defaultValue) {
      this.defaultValue = defaultValue;
      return this;
   }

   public BooleanSetting setKey(int key) {
      this.key = key;
      return this;
   }

   public BooleanSetting setType(int type) {
      this.type = type;
      return this;
   }

   public BooleanSetting setOnChangeCallback(Consumer<Boolean> onChangeCallback) {
      this.onChangeCallback = onChangeCallback;
      return this;
   }
}
