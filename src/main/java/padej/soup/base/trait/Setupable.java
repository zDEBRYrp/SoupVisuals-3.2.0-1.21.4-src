package padej.soup.base.trait;

import padej.soup.api.feature.module.setting.Setting;

@FunctionalInterface
public interface Setupable {
   void setup(Setting... var1);
}
