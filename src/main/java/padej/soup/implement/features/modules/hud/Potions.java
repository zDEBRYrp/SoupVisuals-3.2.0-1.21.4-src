package padej.soup.implement.features.modules.hud;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class Potions extends Module {
   private final BooleanSetting showHeader = new BooleanSetting("setting.potions.showheader.name", "setting.potions.showheader.desc").setValue(true);
   private final BooleanSetting darkenHeader = new BooleanSetting("setting.potions.darkenheader.name", "setting.potions.darkenheader.desc")
      .setValue(true)
      .visible(this.showHeader::isValue);
   private final BooleanSetting romanNumerals = new BooleanSetting("setting.potions.romannumerals.name", "setting.potions.romannumerals.desc").setValue(false);
   private final BooleanSetting coloredEffects = new BooleanSetting("setting.potions.coloredeffects.name", "setting.potions.coloredeffects.desc")
      .setValue(true);
   private final ValueSetting draggableScale = new ValueSetting("setting.potions.draggablescale.name", "setting.potions.draggablescale.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F);

   public static Potions getInstance() {
      return Instance.get(Potions.class);
   }

   public Potions() {
      super("module.potions.name", ModuleCategory.HUD);
      GroupSetting headerGroup = new GroupSetting("group.potions.header.name", "group.potions.header.desc", false).settings(this.showHeader, this.darkenHeader);
      this.setup(new Setting[]{headerGroup, this.romanNumerals, this.coloredEffects, this.draggableScale});
   }

   public BooleanSetting getShowHeader() {
      return this.showHeader;
   }

   public BooleanSetting getDarkenHeader() {
      return this.darkenHeader;
   }

   public BooleanSetting getRomanNumerals() {
      return this.romanNumerals;
   }

   public BooleanSetting getColoredEffects() {
      return this.coloredEffects;
   }

   public ValueSetting getDraggableScale() {
      return this.draggableScale;
   }
}
