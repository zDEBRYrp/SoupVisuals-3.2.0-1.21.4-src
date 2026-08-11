package padej.soup.api.feature.module.setting.implement;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.base.util.logger.LoggerUtil;

public class SelectSetting extends Setting {
   private String selected;
   private List<String> list;
   private Consumer<String> onChangeCallback;
   private String defaultSelected;

   public SelectSetting(String name, String description) {
      super(name, description);
   }

   public SelectSetting value(String... values) {
      List<String> list = Arrays.asList(values);
      this.selected = list.getFirst();
      this.list = list;
      return this;
   }

   public SelectSetting visible(Supplier<Boolean> visible) {
      this.setVisible(visible);
      return this;
   }

   public SelectSetting selected(String string) {
      if (this.defaultSelected == null) {
         this.defaultSelected = string;
      }

      this.selected = string;
      return this;
   }

   public boolean isSelected(String name) {
      return this.selected.equalsIgnoreCase(name);
   }

   public SelectSetting onChange(Consumer<String> callback) {
      this.onChangeCallback = callback;
      return this;
   }

   public void setSelected(String newSelected) {
      boolean changed = this.selected == null || !this.selected.equals(newSelected);
      if (changed && this.selected != null && "Fix Type".equals(this.getName())) {
         LoggerUtil.protectedInfo("Fix type changed from " + this.selected + " to " + newSelected);
      }

      this.selected = newSelected;
      if (changed) {
         this.notifyChange();
         if (this.onChangeCallback != null) {
            this.onChangeCallback.accept(newSelected);
         }
      }
   }

   public String getSelectedLocalized() {
      return this.selected == null ? null : this.localizeOption(this.selected);
   }

   public List<String> getListLocalized() {
      return this.list == null ? null : this.list.stream().map(this::localizeOption).collect(Collectors.toList());
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
      } else {
         return this.selected == null ? this.defaultSelected != null : !this.selected.equals(this.defaultSelected);
      }
   }

   @Override
   public void reset() {
      if (this.defaultSelected != null) {
         this.setSelected(this.defaultSelected);
      }
   }

   public String getSelected() {
      return this.selected;
   }

   public List<String> getList() {
      return this.list;
   }

   public Consumer<String> getOnChangeCallback() {
      return this.onChangeCallback;
   }

   public String getDefaultSelected() {
      return this.defaultSelected;
   }
}
