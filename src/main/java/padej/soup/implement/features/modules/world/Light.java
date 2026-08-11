package padej.soup.implement.features.modules.world;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.base.util.other.Instance;

public class Light extends Module {
   private final BooleanSetting useCustomLightColor = new BooleanSetting(
         "setting.worldtweaks.customlightcolor.name", "setting.worldtweaks.customlightcolor.desc"
      )
      .setValue(false);
   private final ColorSetting lightColor = new ColorSetting("setting.worldtweaks.lightcolor.name", "setting.worldtweaks.lightcolor.desc")
      .setColor(-2525353)
      .visible(this.useCustomLightColor::isValue);

   public static Light getInstance() {
      return Instance.get(Light.class);
   }

   public Light() {
      super("module.light.name", ModuleCategory.WORLD);
      GroupSetting lightGroup = new GroupSetting("group.worldtweaks.light.name", "group.worldtweaks.light.desc", false)
         .settings(this.useCustomLightColor, this.lightColor);
      this.setup(new Setting[]{lightGroup});
   }

   public BooleanSetting getUseCustomLightColor() {
      return this.useCustomLightColor;
   }

   public ColorSetting getLightColor() {
      return this.lightColor;
   }
}
