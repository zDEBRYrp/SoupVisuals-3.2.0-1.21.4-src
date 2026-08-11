package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class HotBar extends Module {
   private final SelectSetting statusBarsMode = new SelectSetting("setting.hotbar.statusbarsmode.name", "setting.hotbar.statusbarsmode.desc")
      .value("Default", "Bars")
      .selected("Default");
   private final BooleanSetting showBinds = new BooleanSetting("setting.hotbar.showbinds.name", "setting.hotbar.showbinds.desc").setValue(false);
   private final BooleanSetting barsGlow = new BooleanSetting("setting.hotbar.barsglow.name", "setting.hotbar.barsglow.desc")
      .setValue(true)
      .visible(() -> this.statusBarsMode.isSelected("Bars"));
   private final BooleanSetting barsText = new BooleanSetting("setting.hotbar.barstext.name", "setting.hotbar.barstext.desc")
      .setValue(true)
      .visible(() -> this.statusBarsMode.isSelected("Bars"));
   private final BooleanSetting experienceBar = new BooleanSetting("setting.hotbar.experiencebar.name", "setting.hotbar.experiencebar.desc")
      .setValue(false)
      .visible(() -> this.statusBarsMode.isSelected("Bars"));
   private final ValueSetting draggableScale = new ValueSetting("setting.hotbar.draggablescale.name", "setting.hotbar.draggablescale.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F);

   public static HotBar getInstance() {
      return Instance.get(HotBar.class);
   }

   public HotBar() {
      super("module.hotbar.name", ModuleCategory.HUD);
      this.setup(new Setting[]{this.statusBarsMode, this.showBinds, this.barsGlow, this.barsText, this.experienceBar, this.draggableScale});
   }

   public SelectSetting getStatusBarsMode() {
      return this.statusBarsMode;
   }

   public BooleanSetting getShowBinds() {
      return this.showBinds;
   }

   public BooleanSetting getBarsGlow() {
      return this.barsGlow;
   }

   public BooleanSetting getBarsText() {
      return this.barsText;
   }

   public BooleanSetting getExperienceBar() {
      return this.experienceBar;
   }

   public ValueSetting getDraggableScale() {
      return this.draggableScale;
   }
}
