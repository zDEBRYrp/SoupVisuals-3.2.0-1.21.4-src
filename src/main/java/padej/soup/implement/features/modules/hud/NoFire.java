package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class NoFire extends Module {
   public final BooleanSetting hideFire = new BooleanSetting("setting.nofire.hidefire.name", "setting.nofire.hidefire.desc").setValue(false);
   public final ValueSetting yOffset = new ValueSetting("setting.nofire.yoffset.name", "setting.nofire.yoffset.desc")
      .setValue(-0.2F)
      .range(-0.5F, 0.0F)
      .visible(() -> !this.hideFire.isValue());
   public final ValueSetting fireAlpha = new ValueSetting("setting.nofire.firealpha.name", "setting.nofire.firealpha.desc")
      .setValue(0.5F)
      .range(0.0F, 1.0F)
      .visible(() -> !this.hideFire.isValue());

   public static NoFire getInstance() {
      return Instance.get(NoFire.class);
   }

   public NoFire() {
      super("module.nofire.name", ModuleCategory.HUD);
      this.setup(new Setting[]{this.hideFire, this.yOffset, this.fireAlpha});
   }

   public BooleanSetting getHideFire() {
      return this.hideFire;
   }

   public ValueSetting getYOffset() {
      return this.yOffset;
   }

   public ValueSetting getFireAlpha() {
      return this.fireAlpha;
   }
}
