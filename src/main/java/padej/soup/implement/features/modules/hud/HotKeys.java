package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class HotKeys extends Module {
   public final SelectSetting mode = new SelectSetting("setting.hotkeys.mode.name", "setting.hotkeys.mode.desc").value("Active", "All").selected("All");
   private final BooleanSetting showHeader = new BooleanSetting("setting.hotkeys.showheader.name", "setting.hotkeys.showheader.desc").setValue(true);
   private final BooleanSetting darkenHeader = new BooleanSetting("setting.hotkeys.darkenheader.name", "setting.hotkeys.darkenheader.desc")
      .setValue(true)
      .visible(this.showHeader::isValue);
   private final ValueSetting draggableScale = new ValueSetting("setting.hotkeys.draggablescale.name", "setting.hotkeys.draggablescale.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F);

   public static HotKeys getInstance() {
      return Instance.get(HotKeys.class);
   }

   public HotKeys() {
      super("module.hotkeys.name", ModuleCategory.HUD);
      GroupSetting headerGroup = new GroupSetting("group.hotkeys.header.name", "group.hotkeys.header.desc", false).settings(this.showHeader, this.darkenHeader);
      this.setup(new Setting[]{this.mode, headerGroup, this.draggableScale});
   }

   public BooleanSetting getShowHeader() {
      return this.showHeader;
   }

   public BooleanSetting getDarkenHeader() {
      return this.darkenHeader;
   }

   public ValueSetting getDraggableScale() {
      return this.draggableScale;
   }
}
