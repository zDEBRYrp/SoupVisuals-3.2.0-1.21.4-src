package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class Inventory extends Module {
   private final BooleanSetting showHeader = new BooleanSetting("setting.inventory.showheader.name", "setting.inventory.showheader.desc").setValue(true);
   private final BooleanSetting darkenHeader = new BooleanSetting("setting.inventory.darkenheader.name", "setting.inventory.darkenheader.desc")
      .setValue(true)
      .visible(this.showHeader::isValue);
   private final ValueSetting draggableScale = new ValueSetting("setting.inventory.draggablescale.name", "setting.inventory.draggablescale.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F);

   public static Inventory getInstance() {
      return Instance.get(Inventory.class);
   }

   public Inventory() {
      super("module.inventory.name", ModuleCategory.HUD);
      GroupSetting headerGroup = new GroupSetting("group.inventory.header.name", "group.inventory.header.desc", false)
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
