package padej.soup.api.feature.module;

import padej.soup.api.system.localization.LocalizationManager;

public class ModuleDescription {
   public static String getDescription(String moduleName) {
      LocalizationManager localizationManager = LocalizationManager.getInstance();
      String descKey = moduleName;
      if (moduleName.endsWith(".name")) {
         descKey = moduleName.replace(".name", ".desc");
      }

      String localizedDescription = localizationManager.get(descKey);
      return !localizedDescription.equals(descKey) ? localizedDescription : "";
   }

   public static String getDescription(Module module) {
      String localizationKey = module.getVisibleNameKey();
      String description = getDescription(localizationKey);
      if (description.isEmpty()) {
         description = getDescription(module.getName());
      }

      return description;
   }

   public static void setDescription(String moduleName, String description) {
   }

   public static boolean hasDescription(String moduleName) {
      String description = getDescription(moduleName);
      return !description.isEmpty();
   }
}
