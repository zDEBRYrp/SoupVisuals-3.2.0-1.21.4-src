package padej.soup.api.feature.module;

import padej.soup.api.feature.module.setting.implement.SelectSetting;

public interface IParticleModule {
   SelectSetting getColorMode();

   SelectSetting getColorAnimation();

   int[] getCustomColors();

   default int getHealColor() {
      return -13641408;
   }

   default int getDamageColor() {
      return -973005;
   }
}
