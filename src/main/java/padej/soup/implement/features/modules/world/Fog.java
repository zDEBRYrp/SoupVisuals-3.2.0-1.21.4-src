package padej.soup.implement.features.modules.world;

import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.render.FogEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.other.Instance;

public class Fog extends Module {
   private final ValueSetting distanceSetting = new ValueSetting("setting.worldtweaks.fogdistance.name", "setting.worldtweaks.fogdistance.desc")
      .setValue(100.0F)
      .range(20, 200);
   private final BooleanSetting useCustomFogColor = new BooleanSetting("setting.worldtweaks.customfogcolor.name", "setting.worldtweaks.customfogcolor.desc")
      .setValue(false);
   private final ColorSetting fogColor = new ColorSetting("setting.worldtweaks.fogcolor.name", "setting.worldtweaks.fogcolor.desc")
      .setColor(-2525353)
      .visible(this.useCustomFogColor::isValue);

   public static Fog getInstance() {
      return Instance.get(Fog.class);
   }

   public Fog() {
      super("module.fog.name", ModuleCategory.WORLD);
      GroupSetting fogGroup = new GroupSetting("group.worldtweaks.fog.name", "group.worldtweaks.fog.desc", false)
         .settings(this.distanceSetting, this.useCustomFogColor, this.fogColor);
      this.setup(new Setting[]{fogGroup});
   }

   @EventHandler
   public void onFog(FogEvent e) {
      e.setDistance(this.distanceSetting.getValue());
      e.setColor(this.useCustomFogColor.isValue() ? this.fogColor.getColor() : ColorUtil.getClientColor());
      e.cancel();
   }

   public ValueSetting getDistanceSetting() {
      return this.distanceSetting;
   }

   public BooleanSetting getUseCustomFogColor() {
      return this.useCustomFogColor;
   }

   public ColorSetting getFogColor() {
      return this.fogColor;
   }
}
