package padej.soup.api.feature.module;

import net.minecraft.util.Identifier;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;

public interface ITargetRenderModule {
   int[] getCustomColors();

   boolean isOptimalAim();

   boolean isStaticMode();

   float getLegacySize();

   Identifier getGhostTexture();

   Identifier getLegacyTexture();

   SelectSetting getGhostsBlend();

   SelectSetting getGhostsTrajectory();

   ValueSetting getGhostsCount();

   float getCrystalsDistance();

   float getCrystalsSize();

   boolean isCrystalsGlow();

   float getCrystalsGlowSize();

   boolean isCrystalsHorizontal();
}
