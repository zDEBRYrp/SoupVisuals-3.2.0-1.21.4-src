package padej.soup.implement.features.modules.other;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class AspectRatio extends Module {
   private final BooleanSetting usePreset = new BooleanSetting("setting.aspectratio.usepreset.name", "setting.aspectratio.usepreset.desc").setValue(false);
   private final SelectSetting preset = new SelectSetting("setting.aspectratio.preset.name", "setting.aspectratio.preset.desc")
      .value("16:9", "5:4", "4:3", "21:9")
      .selected("16:9")
      .visible(this.usePreset::isValue);
   private final ValueSetting factor = new ValueSetting("setting.aspectratio.factor.name", "setting.aspectratio.factor.desc")
      .setValue(1.0F)
      .range(0.5F, 1.5F)
      .visible(() -> !this.usePreset.isValue());

   public static AspectRatio getInstance() {
      return Instance.get(AspectRatio.class);
   }

   public AspectRatio() {
      super("module.aspectratio.name", ModuleCategory.OTHER);
      GroupSetting mainGroup = new GroupSetting("group.aspectratio.main.name", "group.aspectratio.main.desc", false)
         .settings(this.usePreset, this.preset, this.factor);
      this.setup(new Setting[]{mainGroup});
   }

   public float getRatio() {
      if (this.usePreset.isValue()) {
         String var1 = this.preset.getSelected();

         return switch (var1) {
            case "16:9" -> 1.7777778F;
            case "5:4" -> 1.25F;
            case "4:3" -> 1.3333334F;
            case "21:9" -> 2.3333333F;
            default -> 1.7777778F;
         };
      } else {
         return this.factor.getValue();
      }
   }

   public BooleanSetting getUsePreset() {
      return this.usePreset;
   }

   public SelectSetting getPreset() {
      return this.preset;
   }

   public ValueSetting getFactor() {
      return this.factor;
   }
}
