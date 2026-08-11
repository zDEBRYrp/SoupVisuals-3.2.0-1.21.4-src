package padej.soup.implement.features.modules.visuals;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class ChinaHat extends Module {
   private final SelectSetting colorMode = new SelectSetting("setting.chinahat.colormode.name", "setting.chinahat.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   private final SelectSetting customColorsCount = new SelectSetting("setting.chinahat.colorcount.name", "setting.chinahat.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Quartet")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiColorSetting customColors = new MultiColorSetting("setting.chinahat.gradientcolors.name", "setting.chinahat.gradientcolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final ValueSetting centerAlpha = new ValueSetting("setting.chinahat.centeralpha.name", "setting.chinahat.centeralpha.desc")
      .setValue(1.0F)
      .range(0.0F, 1.0F);
   private final ValueSetting edgeAlpha = new ValueSetting("setting.chinahat.edgealpha.name", "setting.chinahat.edgealpha.desc")
      .setValue(0.4F)
      .range(0.0F, 1.0F);
   private final BooleanSetting renderOnFriends = new BooleanSetting("setting.chinahat.renderonfriends.name", "setting.chinahat.renderonfriends.desc")
      .setValue(true);
   private final ValueSetting rotationSpeed = new ValueSetting("setting.chinahat.rotationspeed.name", "setting.chinahat.rotationspeed.desc")
      .setValue(-0.2F)
      .range(-3.0F, 3.0F);

   public static ChinaHat getInstance() {
      return Instance.get(ChinaHat.class);
   }

   public ChinaHat() {
      super("module.chinahat.name", ModuleCategory.VISUALS);
      GroupSetting colorGroup = new GroupSetting("group.chinahat.colors.name", "group.chinahat.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors);
      GroupSetting alphaGroup = new GroupSetting("group.chinahat.alpha.name", "group.chinahat.alpha.desc", false).settings(this.centerAlpha, this.edgeAlpha);
      this.setup(new Setting[]{colorGroup, alphaGroup, this.renderOnFriends, this.rotationSpeed});
   }

   public int[] getCustomColors() {
      if (!this.colorMode.isSelected("Custom")) {
         return null;
      } else {
         String var1 = this.customColorsCount.getSelected();

         return switch (var1) {
            case "Solo" -> new int[]{this.customColors.getColor1().getColor()};
            case "Duo" -> new int[]{this.customColors.getColor1().getColor(), this.customColors.getColor2().getColor()};
            case "Triple" -> new int[]{
               this.customColors.getColor1().getColor(), this.customColors.getColor2().getColor(), this.customColors.getColor3().getColor()
            };
            case "Quartet" -> this.customColors.getColorValues();
            default -> null;
         };
      }
   }

   public SelectSetting getColorMode() {
      return this.colorMode;
   }

   public SelectSetting getCustomColorsCount() {
      return this.customColorsCount;
   }

   public ValueSetting getCenterAlpha() {
      return this.centerAlpha;
   }

   public ValueSetting getEdgeAlpha() {
      return this.edgeAlpha;
   }

   public BooleanSetting getRenderOnFriends() {
      return this.renderOnFriends;
   }

   public ValueSetting getRotationSpeed() {
      return this.rotationSpeed;
   }
}
