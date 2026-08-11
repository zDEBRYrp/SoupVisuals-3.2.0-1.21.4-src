package padej.soup.api.feature.module.setting.implement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.system.localization.LocalizationManager;

public class MultiSelectSetting extends Setting {
   private List<String> list;
   private List<String> selected = new ArrayList<>();
   private List<String> defaultSelected;

   public MultiSelectSetting(String name, String description) {
      super(name, description);
   }

   public MultiSelectSetting value(String... settings) {
      this.list = Arrays.asList(settings);
      return this;
   }

   public MultiSelectSetting selected(String... settings) {
      if (this.defaultSelected == null) {
         this.defaultSelected = new ArrayList<>(Arrays.asList(settings));
      }

      this.selected = new ArrayList<>(Arrays.asList(settings));
      this.notifyChange();
      return this;
   }

   public MultiSelectSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public boolean isSelected(String name) {
      return this.selected.contains(name);
   }

   public List<String> getListLocalized() {
      return this.list == null ? null : this.list.stream().map(this::localizeOption).collect(Collectors.toList());
   }

   public List<String> getSelectedLocalized() {
      return this.selected == null ? null : this.selected.stream().map(this::localizeOption).collect(Collectors.toList());
   }

   private String localizeOption(String option) {
      String key = "select.option." + option.toLowerCase().replace(" ", "");
      String localized = LocalizationManager.getInstance().get(key);
      return localized.equals(key) ? option : localized;
   }

   @Override
   public boolean isModified() {
      if (this.defaultSelected == null) {
         return false;
      } else if (this.selected == null) {
         return this.defaultSelected != null;
      } else {
         return this.selected.size() != this.defaultSelected.size()
            ? true
            : !this.selected.containsAll(this.defaultSelected) || !this.defaultSelected.containsAll(this.selected);
      }
   }

   @Override
   public void reset() {
      if (this.defaultSelected != null) {
         this.selected = new ArrayList<>(this.defaultSelected);
      }
   }

   public List<String> getList() {
      return this.list;
   }

   public List<String> getSelected() {
      return this.selected;
   }

   public List<String> getDefaultSelected() {
      return this.defaultSelected;
   }

   public void setList(List<String> list) {
      this.list = list;
   }

   public void setSelected(List<String> selected) {
      this.selected = selected;
   }

   public void setDefaultSelected(List<String> defaultSelected) {
      this.defaultSelected = defaultSelected;
   }
}
