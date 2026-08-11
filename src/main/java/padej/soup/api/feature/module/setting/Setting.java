package padej.soup.api.feature.module.setting;

import java.util.function.Consumer;
import java.util.function.Supplier;
import padej.soup.api.system.localization.LocalizationManager;

public class Setting {
   private final String nameKey;
   private final String descriptionKey;
   private String moduleContext;
   private Supplier<Boolean> visible;
   private static Consumer<Setting> globalChangeListener;
   private boolean saveToConfig = true;
   private transient String cachedAutoKeyName;
   private transient String cachedAutoKeyDesc;

   public Setting(String nameKey) {
      this.nameKey = nameKey;
      this.descriptionKey = null;
   }

   public Setting(String nameKey, String descriptionKey) {
      this.nameKey = nameKey;
      this.descriptionKey = descriptionKey;
   }

   public boolean isVisible() {
      return this.visible == null || this.visible.get();
   }

   public String getName() {
      if (this.nameKey.contains(".")) {
         String translated = LocalizationManager.getInstance().get(this.nameKey);
         return translated.equals(this.nameKey) ? this.nameKey : translated;
      } else if (this.moduleContext != null && !this.moduleContext.isEmpty()) {
         if (this.cachedAutoKeyName == null) {
            this.cachedAutoKeyName = "setting."
               + this.moduleContext.toLowerCase().replace(" ", "")
               + "."
               + this.nameKey.toLowerCase().replace(" ", "")
               + ".name";
         }

         String translated = LocalizationManager.getInstance().get(this.cachedAutoKeyName);
         return translated.equals(this.cachedAutoKeyName) ? this.nameKey : translated;
      } else {
         String translated = LocalizationManager.getInstance().get(this.nameKey);
         return translated.equals(this.nameKey) ? this.nameKey : translated;
      }
   }

   public String getDescription() {
      if (this.descriptionKey == null || this.descriptionKey.isEmpty()) {
         return "";
      } else if (this.descriptionKey.contains(".")) {
         String translated = LocalizationManager.getInstance().get(this.descriptionKey);
         return translated.equals(this.descriptionKey) ? this.descriptionKey : translated;
      } else if (this.moduleContext != null && !this.moduleContext.isEmpty()) {
         if (this.cachedAutoKeyDesc == null) {
            this.cachedAutoKeyDesc = "setting."
               + this.moduleContext.toLowerCase().replace(" ", "")
               + "."
               + this.descriptionKey.toLowerCase().replace(" ", "")
               + ".desc";
         }

         String translated = LocalizationManager.getInstance().get(this.cachedAutoKeyDesc);
         return translated.equals(this.cachedAutoKeyDesc) ? this.descriptionKey : translated;
      } else {
         String translated = LocalizationManager.getInstance().get(this.descriptionKey);
         return translated.equals(this.descriptionKey) ? this.descriptionKey : translated;
      }
   }

   @Deprecated
   public String getLocalizedName() {
      return this.getName();
   }

   @Deprecated
   public String getLocalizedDescription() {
      return this.getDescription();
   }

   public boolean isModified() {
      return false;
   }

   public void reset() {
   }

   protected void notifyChange() {
      if (globalChangeListener != null) {
         globalChangeListener.accept(this);
      }
   }

   public String getNameKey() {
      return this.nameKey;
   }

   public String getDescriptionKey() {
      return this.descriptionKey;
   }

   public String getModuleContext() {
      return this.moduleContext;
   }

   public Supplier<Boolean> getVisible() {
      return this.visible;
   }

   public boolean isSaveToConfig() {
      return this.saveToConfig;
   }

   public String getCachedAutoKeyName() {
      return this.cachedAutoKeyName;
   }

   public String getCachedAutoKeyDesc() {
      return this.cachedAutoKeyDesc;
   }

   public void setModuleContext(String moduleContext) {
      this.moduleContext = moduleContext;
   }

   public void setVisible(Supplier<Boolean> visible) {
      this.visible = visible;
   }

   public static void setGlobalChangeListener(Consumer<Setting> globalChangeListener) {
      Setting.globalChangeListener = globalChangeListener;
   }

   public void setSaveToConfig(boolean saveToConfig) {
      this.saveToConfig = saveToConfig;
   }
}
