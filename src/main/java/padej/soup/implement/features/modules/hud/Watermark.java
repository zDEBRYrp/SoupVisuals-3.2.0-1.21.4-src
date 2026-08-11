package padej.soup.implement.features.modules.hud;

import java.util.ArrayList;
import java.util.List;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.base.util.other.Instance;

public class Watermark extends Module {
   public final SelectSetting style = new SelectSetting("setting.watermark.style.name", "setting.watermark.style.desc").value("Mono", "Split").selected("Mono");
   public final BooleanSetting showBackground = new BooleanSetting("setting.watermark.showbackground.name", "setting.watermark.showbackground.desc")
      .setValue(true);
   public final BooleanSetting coloredSeparators = new BooleanSetting("setting.watermark.coloredseparators.name", "setting.watermark.coloredseparators.desc")
      .setValue(false)
      .visible(() -> this.style.isSelected("Mono"));
   public final BooleanSetting separatorGradient = new BooleanSetting("setting.watermark.separatorgradient.name", "setting.watermark.separatorgradient.desc")
      .setValue(false)
      .visible(this.coloredSeparators::isValue);
   private final MultiSelectSetting clientInfo = new MultiSelectSetting("setting.watermark.clientinfo.name", "setting.watermark.clientinfo.desc")
      .value("Icon", "Client Name", "Role", "User")
      .selected("Icon", "Client Name", "Role", "User");
   private final MultiSelectSetting systemInfo = new MultiSelectSetting("setting.watermark.systeminfo.name", "setting.watermark.systeminfo.desc")
      .value("FPS", "Server IP", "Ping", "Memory", "CPU")
      .selected("FPS");
   public final MultiSelectSetting watermarkComponents = new Watermark.CombinedMultiSelectSetting();
   public final ValueSetting draggableScale = new ValueSetting("setting.watermark.draggablescale.name", "setting.watermark.draggablescale.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F);

   public static Watermark getInstance() {
      return Instance.get(Watermark.class);
   }

   public Watermark() {
      super("module.watermark.name", ModuleCategory.HUD);
      GroupSetting appearanceGroup = new GroupSetting("group.watermark.appearance.name", "group.watermark.appearance.desc", false)
         .settings(this.style, this.showBackground);
      GroupSetting colorsGroup = new GroupSetting("group.watermark.colors.name", "group.watermark.colors.desc", false)
         .settings(this.coloredSeparators, this.separatorGradient)
         .visible(() -> this.style.isSelected("Mono"));
      this.setup(new Setting[]{appearanceGroup, colorsGroup, this.clientInfo, this.systemInfo, this.draggableScale});
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

   public MultiSelectSetting getWatermarkComponents() {
      return this.watermarkComponents;
   }

   public ValueSetting getDraggableScale() {
      return this.draggableScale;
   }

   private class CombinedMultiSelectSetting extends MultiSelectSetting {
      public CombinedMultiSelectSetting() {
         super("", "");
      }

      @Override
      public List<String> getSelected() {
         List<String> combined = new ArrayList<>();
         combined.addAll(Watermark.this.clientInfo.getSelected());
         combined.addAll(Watermark.this.systemInfo.getSelected());
         return combined;
      }

      @Override
      public boolean isSelected(String value) {
         return Watermark.this.clientInfo.isSelected(value) || Watermark.this.systemInfo.isSelected(value);
      }
   }
}
