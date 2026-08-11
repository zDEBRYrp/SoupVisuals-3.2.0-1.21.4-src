package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class ScoreBoard extends Module {
   private final BooleanSetting showHeader = new BooleanSetting("setting.scoreboard.showheader.name", "setting.scoreboard.showheader.desc").setValue(true);
   private final BooleanSetting darkenHeader = new BooleanSetting("setting.scoreboard.darkenheader.name", "setting.scoreboard.darkenheader.desc")
      .setValue(true)
      .visible(this.showHeader::isValue);
   private final ValueSetting draggableScale = new ValueSetting("setting.scoreboard.draggablescale.name", "setting.scoreboard.draggablescale.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F);

   public static ScoreBoard getInstance() {
      return Instance.get(ScoreBoard.class);
   }

   public ScoreBoard() {
      super("module.scoreboard.name", ModuleCategory.HUD);
      GroupSetting headerGroup = new GroupSetting("group.scoreboard.header.name", "group.scoreboard.header.desc", false)
         .settings(this.showHeader, this.darkenHeader);
      this.setup(new Setting[]{headerGroup, this.draggableScale});
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
