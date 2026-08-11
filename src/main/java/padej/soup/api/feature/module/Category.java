package padej.soup.api.feature.module;

import padej.soup.api.system.font.msdf.MsdfFont;

public interface Category {
   String getIdentifier();

   String getLocalizationKey();

   String getLocalizedName();

   default int getIconCodepoint() {
      return -1;
   }

   default MsdfFont getIconFont() {
      return null;
   }

   default boolean hasIcon() {
      return this.getIconCodepoint() >= 0;
   }

   default boolean isHidden() {
      return false;
   }
}
