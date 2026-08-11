package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class PlayerInfo extends Module {
   public final SelectSetting position = new SelectSetting("setting.playerinfo.position.name", "setting.playerinfo.position.desc")
      .value("Horizontal", "Vertical")
      .selected("Horizontal");
   public final SelectSetting style = new SelectSetting("setting.playerinfo.style.name", "setting.playerinfo.style.desc")
      .value("Mono", "Split")
      .selected("Split");
   public final BooleanSetting showBackground = new BooleanSetting("setting.playerinfo.showbackground.name", "setting.playerinfo.showbackground.desc")
      .setValue(true);
   public final BooleanSetting coloredSeparators = new BooleanSetting("setting.playerinfo.coloredseparators.name", "setting.playerinfo.coloredseparators.desc")
      .setValue(false)
      .visible(() -> this.position.isSelected("Horizontal") && this.style.isSelected("Mono"));
   public final BooleanSetting separatorGradient = new BooleanSetting("setting.playerinfo.separatorgradient.name", "setting.playerinfo.separatorgradient.desc")
      .setValue(false)
      .visible(() -> this.coloredSeparators.isValue() && this.position.isSelected("Horizontal") && this.style.isSelected("Mono"));
   public final MultiSelectSetting playerInfoComponents = new MultiSelectSetting("setting.playerinfo.components.name", "setting.playerinfo.components.desc")
      .value("BPS", "TPS", "XYZ")
      .selected("BPS", "TPS", "XYZ");
   public final ValueSetting draggableScale = new ValueSetting("setting.playerinfo.draggablescale.name", "setting.playerinfo.draggablescale.desc")
      .setValue(1.0F)
      .range(0.5F, 1.0F);

   public static PlayerInfo getInstance() {
      return Instance.get(PlayerInfo.class);
   }

   public PlayerInfo() {
      super("module.playerinfo.name", ModuleCategory.HUD);
      GroupSetting appearanceGroup = new GroupSetting("group.playerinfo.appearance.name", "group.playerinfo.appearance.desc", false)
         .settings(this.position, this.style, this.showBackground);
      this.setup(new Setting[]{appearanceGroup, this.coloredSeparators, this.separatorGradient, this.playerInfoComponents, this.draggableScale});
   }

   public SelectSetting getPosition() {
      return this.position;
   }

   public SelectSetting getStyle() {
      return this.style;
   }

   public BooleanSetting getShowBackground() {
      return this.showBackground;
   }

   public BooleanSetting getColoredSeparators() {
      return this.coloredSeparators;
   }

   public BooleanSetting getSeparatorGradient() {
      return this.separatorGradient;
   }

   public MultiSelectSetting getPlayerInfoComponents() {
      return this.playerInfoComponents;
   }

   public ValueSetting getDraggableScale() {
      return this.draggableScale;
   }
}
