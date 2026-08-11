package padej.soup.api.feature.module;

import padej.soup.api.system.font.msdf.MsdfFont;
import padej.soup.api.system.localization.LocalizationManager;

public final class CustomCategory implements Category {
   private final String identifier;
   private final String localizationKey;
   private int iconCodepoint = -1;
   private MsdfFont iconFont;
   private boolean hidden = false;

   public CustomCategory(String identifier, String localizationKey) {
      if (identifier == null || identifier.isBlank()) {
         throw new IllegalArgumentException("CustomCategory identifier must not be blank");
      } else if (localizationKey != null && !localizationKey.isBlank()) {
         this.identifier = identifier;
         this.localizationKey = localizationKey;
      } else {
         throw new IllegalArgumentException("CustomCategory localizationKey must not be blank");
      }
   }

   public CustomCategory setIcon(int codepoint, MsdfFont font) {
      this.iconCodepoint = codepoint;
      this.iconFont = font;
      return this;
   }

   public CustomCategory setHidden(boolean hidden) {
      this.hidden = hidden;
      return this;
   }

   @Override
   public boolean isHidden() {
      return this.hidden;
   }

   @Override
   public int getIconCodepoint() {
      return this.iconCodepoint;
   }

   @Override
   public MsdfFont getIconFont() {
      return this.iconFont;
   }

   @Override
   public String getIdentifier() {
      return this.identifier;
   }

   @Override
   public String getLocalizationKey() {
      return this.localizationKey;
   }

   @Override
   public String getLocalizedName() {
      String translated = LocalizationManager.getInstance().get(this.localizationKey);
      return translated.equals(this.localizationKey) ? this.identifier : translated;
   }

   @Override
   public String toString() {
      return "CustomCategory{" + this.identifier + "}";
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else {
         return obj instanceof CustomCategory other ? this.identifier.equals(other.identifier) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.identifier.hashCode();
   }
}
