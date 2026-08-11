package padej.soup.api.feature.module;

import padej.soup.api.system.localization.LocalizationManager;

public enum ModuleCategory implements Category {
   VISUALS("category.visuals", -1),
   WORLD("category.world", 61464),
   PARTICLES("category.particles", 61465),
   CLIENT("category.client", 61462),
   OTHER("category.other", -1),
   HUD("category.hud", 61463),
   MCTIERS("category.mctiers", -1),
   PERSONAL_INFO("category.personal_info", -1),
   HOME("category.home", -1),
   SEARCH("category.search", -1);

   private final String localizationKey;
   private final int iconCodepoint;

   private ModuleCategory(String localizationKey, int iconCodepoint) {
      this.localizationKey = localizationKey;
      this.iconCodepoint = iconCodepoint;
   }

   @Override
   public String getLocalizedName() {
      String localized = LocalizationManager.getInstance().get(this.localizationKey);
      return localized.equals(this.localizationKey) ? this.localizationKey : localized;
   }

   @Deprecated
   public String getReadableName() {
      return this.getLocalizedName();
   }

   @Override
   public String getIdentifier() {
      return this.name().toLowerCase();
   }

   @Override
   public String getLocalizationKey() {
      return this.localizationKey;
   }

   @Override
   public int getIconCodepoint() {
      return this.iconCodepoint;
   }
}
