package padej.soup.api.feature.module.setting.implement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.system.localization.LocalizationManager;

public class GroupSetting extends Setting {
   private boolean value;
   private Boolean defaultValue;
   private boolean checkbox = true;
   private List<Setting> subSettings = new ArrayList<>();

   public GroupSetting(String name, String description) {
      super(name, description);
   }

   public GroupSetting(String name, String description, boolean checkbox) {
      super(name, description);
      this.checkbox = checkbox;
   }

   public GroupSetting settings(Setting... setting) {
      this.subSettings.addAll(Arrays.asList(setting));
      return this;
   }

   public GroupSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   @Override
   public String getName() {
      String nameKey = this.getNameKey();
      if (nameKey.contains(".")) {
         String translated = LocalizationManager.getInstance().get(nameKey);
         return translated.equals(nameKey) ? nameKey : translated;
      } else {
         String moduleContext = this.getModuleContext();
         if (moduleContext != null && !moduleContext.isEmpty()) {
            String autoKey = "group." + moduleContext.toLowerCase().replace(" ", "") + "." + nameKey.toLowerCase().replace(" ", "") + ".name";
            String translated = LocalizationManager.getInstance().get(autoKey);
            return translated.equals(autoKey) ? nameKey : translated;
         } else {
            String translated = LocalizationManager.getInstance().get(nameKey);
            return translated.equals(nameKey) ? nameKey : translated;
         }
      }
   }

   @Override
   public String getDescription() {
      String descriptionKey = this.getDescriptionKey();
      if (descriptionKey == null || descriptionKey.isEmpty()) {
         return "";
      } else if (descriptionKey.contains(".")) {
         String translated = LocalizationManager.getInstance().get(descriptionKey);
         return translated.equals(descriptionKey) ? descriptionKey : translated;
      } else {
         String moduleContext = this.getModuleContext();
         if (moduleContext != null && !moduleContext.isEmpty()) {
            String autoKey = "group." + moduleContext.toLowerCase().replace(" ", "") + "." + descriptionKey.toLowerCase().replace(" ", "") + ".desc";
            String translated = LocalizationManager.getInstance().get(autoKey);
            return translated.equals(autoKey) ? descriptionKey : translated;
         } else {
            String translated = LocalizationManager.getInstance().get(descriptionKey);
            return translated.equals(descriptionKey) ? descriptionKey : translated;
         }
      }
   }

   public GroupSetting setValue(boolean value) {
      if (this.defaultValue == null) {
         this.defaultValue = value;
      }

      this.value = value;
      this.notifyChange();
      return this;
   }

   @Override
   public boolean isModified() {
      if (this.defaultValue != null && this.value != this.defaultValue) {
         return true;
      } else {
         for (Setting subSetting : this.subSettings) {
            if (subSetting.isModified()) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public void reset() {
      if (this.defaultValue != null) {
         this.value = this.defaultValue;
      }

      for (Setting subSetting : this.subSettings) {
         subSetting.reset();
      }
   }

   public boolean isValue() {
      return this.value;
   }

   public Boolean getDefaultValue() {
      return this.defaultValue;
   }

   public boolean isCheckbox() {
      return this.checkbox;
   }

   public List<Setting> getSubSettings() {
      return this.subSettings;
   }

   public GroupSetting setDefaultValue(Boolean defaultValue) {
      this.defaultValue = defaultValue;
      return this;
   }

   public GroupSetting setCheckbox(boolean checkbox) {
      this.checkbox = checkbox;
      return this;
   }

   public GroupSetting setSubSettings(List<Setting> subSettings) {
      this.subSettings = subSettings;
      return this;
   }
}
