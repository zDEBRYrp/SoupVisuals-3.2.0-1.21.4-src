package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class BossBars extends Module {
   private final ValueSetting draggableScale = new ValueSetting("setting.bossbars.draggablescale.name", "setting.bossbars.draggablescale.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F);

   public static BossBars getInstance() {
      return Instance.get(BossBars.class);
   }

   public BossBars() {
      super("module.bossbars.name", ModuleCategory.HUD);
      this.setup(new Setting[]{this.draggableScale});
   }

   public ValueSetting getDraggableScale() {
      return this.draggableScale;
   }
}
