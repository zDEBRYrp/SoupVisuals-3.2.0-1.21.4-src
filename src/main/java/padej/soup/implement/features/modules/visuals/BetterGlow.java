package padej.soup.implement.features.modules.visuals;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class BetterGlow extends Module {
   private final ValueSetting outlineWidth = new ValueSetting("setting.betterglow.outlinewidth.name", "setting.betterglow.outlinewidth.desc")
      .setValue(2.0F)
      .range(1, 5);
   private final ValueSetting glowMultiplier = new ValueSetting("setting.betterglow.glowmultiplier.name", "setting.betterglow.glowmultiplier.desc")
      .setValue(4.0F)
      .range(0, 10);
   private final SelectSetting shapeMode = new SelectSetting("setting.betterglow.shapemode.name", "setting.betterglow.shapemode.desc")
      .value("Lines", "Sides", "Both")
      .selected("Both");

   public static BetterGlow getInstance() {
      return Instance.get(BetterGlow.class);
   }

   public BetterGlow() {
      super("module.betterglow.name", ModuleCategory.VISUALS);
      GroupSetting glowGroup = new GroupSetting("group.betterglow.glow.name", "group.betterglow.glow.desc", false)
         .settings(this.outlineWidth, this.glowMultiplier);
      GroupSetting shapeGroup = new GroupSetting("group.betterglow.shape.name", "group.betterglow.shape.desc", false).settings(this.shapeMode);
      this.setup(new Setting[]{glowGroup, shapeGroup});
   }

   public int getShapeModeValue() {
      String var1 = this.shapeMode.getSelected();

      return switch (var1) {
         case "Lines" -> 0;
         case "Sides" -> 1;
         case "Both" -> 2;
         default -> throw new IllegalStateException("Unexpected value: " + this.shapeMode.getSelected());
      };
   }

   public ValueSetting getOutlineWidth() {
      return this.outlineWidth;
   }

   public ValueSetting getGlowMultiplier() {
      return this.glowMultiplier;
   }

   public SelectSetting getShapeMode() {
      return this.shapeMode;
   }
}
