package padej.soup.api.feature.module.setting.implement;

import java.util.function.Supplier;
import padej.soup.api.feature.module.setting.Setting;

public class ValueSetting extends Setting {
   private float value;
   private float min;
   private float max;
   private boolean integer;
   private Float defaultValue;

   public ValueSetting(String name, String description) {
      super(name, description);
   }

   public ValueSetting range(float min, float max) {
      this.min = min;
      this.max = max;
      return this;
   }

   public ValueSetting range(int min, int max) {
      this.min = min;
      this.max = max;
      this.integer = true;
      return this;
   }

   public int getInt() {
      return (int)this.value;
   }

   public ValueSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public ValueSetting setValue(float value) {
      if (this.defaultValue == null) {
         this.defaultValue = value;
         this.value = value;
         return this;
      } else {
         boolean changed = this.value != value;
         this.value = value;
         if (changed) {
            this.notifyChange();
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

   public float getValue() {
      return this.value;
   }

   public float getMin() {
      return this.min;
   }

   public float getMax() {
      return this.max;
   }

   public boolean isInteger() {
      return this.integer;
   }

   public Float getDefaultValue() {
      return this.defaultValue;
   }

   public ValueSetting setMin(float min) {
      this.min = min;
      return this;
   }

   public ValueSetting setMax(float max) {
      this.max = max;
      return this;
   }

   public ValueSetting setInteger(boolean integer) {
      this.integer = integer;
      return this;
   }

   public ValueSetting setDefaultValue(Float defaultValue) {
      this.defaultValue = defaultValue;
      return this;
   }
}
