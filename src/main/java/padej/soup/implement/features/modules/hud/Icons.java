package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.base.util.other.Instance;

public class Icons extends Module {
   public final BooleanSetting showIcons = new BooleanSetting("setting.icons.showicons.name", "setting.icons.showicons.desc").setValue(true);
   public final BooleanSetting coloredIcons = new BooleanSetting("setting.icons.coloredicons.name", "setting.icons.coloredicons.desc").setValue(true);
   public final BooleanSetting iconGradient = new BooleanSetting("setting.icons.icongradient.name", "setting.icons.icongradient.desc")
      .setValue(true)
      .visible(this.coloredIcons::isValue);

   public static Icons getInstance() {
      return Instance.get(Icons.class);
   }

   public Icons() {
      super("module.icons.name", ModuleCategory.HUD, false);
      GroupSetting generalGroup = new GroupSetting("group.icons.general.name", "group.icons.general.desc", false)
         .settings(this.showIcons, this.coloredIcons, this.iconGradient);
      this.setup(new Setting[]{generalGroup});
   }

   public BooleanSetting getShowIcons() {
      return this.showIcons;
   }

   public BooleanSetting getColoredIcons() {
      return this.coloredIcons;
   }

   public BooleanSetting getIconGradient() {
      return this.iconGradient;
   }
}
