package padej.soup.implement.features.modules.world;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class Bright extends Module {
   private ValueSetting brightSetting = new ValueSetting("setting.worldtweaks.bright.name", "setting.worldtweaks.bright.desc").setValue(1.0F).range(0.0F, 1.0F);

   public static Bright getInstance() {
      return Instance.get(Bright.class);
   }

   public Bright() {
      super("module.bright.name", ModuleCategory.WORLD);
      this.setup(new Setting[]{this.brightSetting});
   }

   public ValueSetting getBrightSetting() {
      return this.brightSetting;
   }
}
