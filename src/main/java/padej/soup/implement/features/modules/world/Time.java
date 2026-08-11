package padej.soup.implement.features.modules.world;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class Time extends Module {
   private ValueSetting timeSetting = new ValueSetting("setting.worldtweaks.time.name", "setting.worldtweaks.time.desc").setValue(12.0F).range(0, 24);

   public static Time getInstance() {
      return Instance.get(Time.class);
   }

   public Time() {
      super("module.time.name", ModuleCategory.WORLD);
      this.setup(new Setting[]{this.timeSetting});
   }

   public ValueSetting getTimeSetting() {
      return this.timeSetting;
   }
}
