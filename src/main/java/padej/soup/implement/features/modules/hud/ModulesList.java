package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class ModulesList extends Module {
   public final SelectSetting sortMode = new SelectSetting("setting.moduleslist.sortmode.name", "setting.moduleslist.sortmode.desc")
      .value("Length", "Settings Count")
      .selected("Length");
   public final BooleanSetting showBackground = new BooleanSetting("setting.moduleslist.showbackground.name", "setting.moduleslist.showbackground.desc")
      .setValue(true);
   public final BooleanSetting lowercase = new BooleanSetting("setting.moduleslist.lowercase.name", "setting.moduleslist.lowercase.desc").setValue(false);
   public final BooleanSetting gradientText = new BooleanSetting("setting.moduleslist.gradienttext.name", "setting.moduleslist.gradienttext.desc")
      .setValue(true);
   public final BooleanSetting sideLine = new BooleanSetting("setting.moduleslist.sideline.name", "setting.moduleslist.sideline.desc").setValue(true);
   public final BooleanSetting gradientSideLine = new BooleanSetting("setting.moduleslist.gradientsideline.name", "setting.moduleslist.gradientsideline.desc")
      .setValue(true)
      .visible(this.sideLine::isValue);
   public final ValueSetting draggableScale = new ValueSetting("setting.moduleslist.draggablescale.name", "setting.moduleslist.draggablescale.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F);

   public static ModulesList getInstance() {
      return Instance.get(ModulesList.class);
   }

   public ModulesList() {
      super("module.moduleslist.name", ModuleCategory.HUD);
      GroupSetting appearanceGroup = new GroupSetting("group.moduleslist.appearance.name", "group.moduleslist.appearance.desc", false)
         .settings(this.showBackground, this.lowercase);
      GroupSetting colorsGroup = new GroupSetting("group.moduleslist.colors.name", "group.moduleslist.colors.desc", false)
         .settings(this.gradientText, this.sideLine, this.gradientSideLine);
      this.setup(new Setting[]{this.sortMode, appearanceGroup, colorsGroup, this.draggableScale});
   }

   public SelectSetting getSortMode() {
      return this.sortMode;
   }

   public BooleanSetting getShowBackground() {
      return this.showBackground;
   }

   public BooleanSetting getLowercase() {
      return this.lowercase;
   }

   public BooleanSetting getGradientText() {
      return this.gradientText;
   }

   public BooleanSetting getSideLine() {
      return this.sideLine;
   }

   public BooleanSetting getGradientSideLine() {
      return this.gradientSideLine;
   }

   public ValueSetting getDraggableScale() {
      return this.draggableScale;
   }
}
