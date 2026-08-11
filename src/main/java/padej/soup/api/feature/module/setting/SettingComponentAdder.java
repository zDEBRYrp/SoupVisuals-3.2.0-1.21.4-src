package padej.soup.api.feature.module.setting;

import java.util.List;
import padej.soup.api.feature.module.setting.implement.BindSetting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ButtonSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.TextSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.implement.menu.components.implement.settings.AbstractSettingComponent;
import padej.soup.implement.menu.components.implement.settings.BindComponent;
import padej.soup.implement.menu.components.implement.settings.CheckboxComponent;
import padej.soup.implement.menu.components.implement.settings.ColorComponent;
import padej.soup.implement.menu.components.implement.settings.GroupComponent;
import padej.soup.implement.menu.components.implement.settings.MultiColorComponent;
import padej.soup.implement.menu.components.implement.settings.SButtonComponent;
import padej.soup.implement.menu.components.implement.settings.TextComponent;
import padej.soup.implement.menu.components.implement.settings.ValueComponent;
import padej.soup.implement.menu.components.implement.settings.multiselect.MultiSelectComponent;
import padej.soup.implement.menu.components.implement.settings.select.SelectComponent;

public class SettingComponentAdder {
   public void addSettingComponent(List<Setting> settings, List<AbstractSettingComponent> components) {
      settings.forEach(setting -> {
         if (setting instanceof BooleanSetting booleanSetting) {
            components.add(new CheckboxComponent(booleanSetting));
         }

         if (setting instanceof BindSetting bindSetting) {
            components.add(new BindComponent(bindSetting));
         }

         if (setting instanceof ColorSetting colorSetting) {
            components.add(new ColorComponent(colorSetting));
         }

         if (setting instanceof TextSetting textSetting) {
            components.add(new TextComponent(textSetting));
         }

         if (setting instanceof ValueSetting valueSetting) {
            components.add(new ValueComponent(valueSetting));
         }

         if (setting instanceof GroupSetting groupSetting) {
            components.add(new GroupComponent(groupSetting));
         }

         if (setting instanceof ButtonSetting buttonSetting) {
            components.add(new SButtonComponent(buttonSetting));
         }

         if (setting instanceof SelectSetting selectSetting) {
            components.add(new SelectComponent(selectSetting));
         }

         if (setting instanceof MultiSelectSetting multiSelectSetting) {
            components.add(new MultiSelectComponent(multiSelectSetting));
         }

         if (setting instanceof MultiColorSetting multiColorSetting) {
            components.add(new MultiColorComponent(multiColorSetting));
         }
      });
   }
}
