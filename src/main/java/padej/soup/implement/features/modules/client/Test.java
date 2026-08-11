package padej.soup.implement.features.modules.client;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class Test extends Module {
   final ValueSetting valA = new ValueSetting("setting.test.vala.name", "setting.test.vala.desc").setValue(-30.0F).range(-60, -10);
   final ValueSetting valB = new ValueSetting("setting.test.valb.name", "setting.test.valb.desc").setValue(20.0F).range(10, 40);

   public static Test getInstance() {
      return Instance.get(Test.class);
   }

   public Test() {
      super("module.test.name", ModuleCategory.CLIENT);
      this.setup(new Setting[]{this.valA, this.valB});
   }

   public ValueSetting getValA() {
      return this.valA;
   }

   public ValueSetting getValB() {
      return this.valB;
   }
}
