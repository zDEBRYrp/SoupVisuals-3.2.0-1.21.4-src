package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class Notifications extends Module {
   private final ValueSetting draggableScale = new ValueSetting("setting.notifications.draggablescale.name", "setting.notifications.draggablescale.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F);

   public static Notifications getInstance() {
      return Instance.get(Notifications.class);
   }

   public Notifications() {
      super("module.notifications.name", ModuleCategory.HUD);
      this.setup(new Setting[]{this.draggableScale});
   }
}
