package padej.soup.implement.features.modules.client;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.file.impl.CurrentLanguageFile;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.core.Main;

public class Language extends Module {
   private final SelectSetting languageSetting = new SelectSetting("setting.language.language.name", "setting.language.language.desc")
      .value("Русский", "English")
      .selected("English");

   public static Language getInstance() {
      return Instance.get(Language.class);
   }

   public Language() {
      super("module.language.name", ModuleCategory.CLIENT, false);
      this.setup(new Setting[]{this.languageSetting});
      this.loadLanguageFromFile();
      this.languageSetting.onChange(value -> {
         this.updateLanguage();
         this.saveLanguageToFile();
      });
      this.updateLanguage();
   }

   private void loadLanguageFromFile() {
      try {
         CurrentLanguageFile languageFile = this.getCurrentLanguageFile();
         if (languageFile != null && languageFile.hasCurrentLanguage()) {
            String langCode = languageFile.getCurrentLanguage();
            byte var5 = -1;
            switch (langCode.hashCode()) {
               case 108861887:
                  if (langCode.equals("ru_ru")) {
                     var5 = 0;
                  }
               default:
                  String displayName = switch (var5) {
                     case 0 -> "Русский";
                     default -> "English";
                  };
                  this.languageSetting.setSelected(displayName);
            }
         }
      } catch (Exception var6) {
         LoggerUtil.error("Failed to load language from file: " + var6.getMessage());
      }
   }

   private void saveLanguageToFile() {
      try {
         CurrentLanguageFile languageFile = this.getCurrentLanguageFile();
         if (languageFile != null) {
            String selected = this.languageSetting.getSelected();
            byte var5 = -1;
            switch (selected.hashCode()) {
               case -1185086888:
                  if (selected.equals("Русский")) {
                     var5 = 0;
                  }
               default:
                  String langCode = switch (var5) {
                     case 0 -> "ru_ru";
                     default -> "en_us";
                  };
                  languageFile.setCurrentLanguage(langCode);
            }
         }
      } catch (Exception var6) {
         LoggerUtil.error("Failed to save language to file: " + var6.getMessage());
      }
   }

   private CurrentLanguageFile getCurrentLanguageFile() {
      try {
         return Main.getInstance()
            .getFileRepository()
            .getClientFiles()
            .stream()
            .filter(file -> file instanceof CurrentLanguageFile)
            .map(file -> (CurrentLanguageFile)file)
            .findFirst()
            .orElse(null);
      } catch (Exception var2) {
         return null;
      }
   }

   private void updateLanguage() {
      String selected = this.languageSetting.getSelected();
      byte var4 = -1;
      switch (selected.hashCode()) {
         case -1185086888:
            if (selected.equals("Русский")) {
               var4 = 0;
            }
         default:
            String langCode = switch (var4) {
               case 0 -> "ru_ru";
               default -> "en_us";
            };
            LocalizationManager.getInstance().setLanguage(langCode);
      }
   }

   public SelectSetting getLanguageSetting() {
      return this.languageSetting;
   }
}
