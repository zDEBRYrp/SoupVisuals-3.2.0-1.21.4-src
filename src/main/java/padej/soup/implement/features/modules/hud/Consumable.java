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

public class Consumable extends Module {
   private final MultiSelectSetting itemTypes = new MultiSelectSetting("setting.consumable.itemtypes.name", "setting.consumable.itemtypes.desc")
      .value(
         "Snowball",
         "Egg",
         "Wind Charge",
         "Golden Apple",
         "Enchanted Golden Apple",
         "Arrow",
         "Spectral Arrow",
         "Tipped Arrow",
         "Totem",
         "Chorus Fruit",
         "Ender Pearl",
         "Firework Rocket",
         "Experience Bottle"
      )
      .selected("Golden Apple", "Enchanted Golden Apple", "Totem", "Ender Pearl");
   private final SelectSetting layout = new SelectSetting("setting.consumable.layout.name", "setting.consumable.layout.desc")
      .value("Line", "Column", "Table")
      .selected("Line");
   private final BooleanSetting showCount = new BooleanSetting("setting.consumable.showcount.name", "setting.consumable.showcount.desc").setValue(true);
   private final ValueSetting scale = new ValueSetting("setting.consumable.scale.name", "setting.consumable.scale.desc").setValue(1.0F).range(0.5F, 2.0F);
   private final BooleanSetting showBackground = new BooleanSetting("setting.consumable.showbackground.name", "setting.consumable.showbackground.desc")
      .setValue(true);

   public static Consumable getInstance() {
      return Instance.get(Consumable.class);
   }

   public Consumable() {
      super("module.consumable.name", ModuleCategory.HUD);
      GroupSetting itemGroup = new GroupSetting("group.consumable.items.name", "group.consumable.items.desc", false).settings(this.itemTypes);
      GroupSetting displayGroup = new GroupSetting("group.consumable.display.name", "group.consumable.display.desc", false)
         .settings(this.layout, this.showCount, this.scale, this.showBackground);
      this.setup(new Setting[]{itemGroup, displayGroup});
   }

   public MultiSelectSetting getItemTypes() {
      return this.itemTypes;
   }

   public SelectSetting getLayout() {
      return this.layout;
   }

   public BooleanSetting getShowCount() {
      return this.showCount;
   }

   public ValueSetting getScale() {
      return this.scale;
   }

   public BooleanSetting getShowBackground() {
      return this.showBackground;
   }
}
