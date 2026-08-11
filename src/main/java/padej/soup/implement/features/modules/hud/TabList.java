package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.base.util.other.Instance;

public class TabList extends Module {
   private final SelectSetting friendColorMode = new SelectSetting("setting.tablist.friendcolormode.name", "setting.tablist.friendcolormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   private final ColorSetting friendColor = new ColorSetting("setting.tablist.friendcolor.name", "setting.tablist.friendcolor.desc")
      .setColor(-1439773583)
      .visible(() -> this.friendColorMode.isSelected("Custom"));
   private final BooleanSetting extendedTab = new BooleanSetting("setting.tablist.extendedTab.name", "setting.tablist.extendedTab.desc").setValue(false);

   public static TabList getInstance() {
      return Instance.get(TabList.class);
   }

   public TabList() {
      super("module.tablist.name", ModuleCategory.HUD);
      GroupSetting friendsGroup = new GroupSetting("group.tablist.friends.name", "group.tablist.friends.desc", false)
         .settings(this.friendColorMode, this.friendColor);
      this.setup(new Setting[]{friendsGroup, this.extendedTab});
   }

   public int getFriendBackgroundColor() {
      return this.friendColorMode.isSelected("Custom") ? this.friendColor.getColor() : -1439773583;
   }

   public SelectSetting getFriendColorMode() {
      return this.friendColorMode;
   }

   public ColorSetting getFriendColor() {
      return this.friendColor;
   }

   public BooleanSetting getExtendedTab() {
      return this.extendedTab;
   }
}
