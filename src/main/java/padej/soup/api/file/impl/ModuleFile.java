package padej.soup.api.file.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.feature.draggable.DraggableRepository;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleRepository;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BindSetting;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.ColorSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.TextSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.api.file.ClientFile;
import padej.soup.api.file.SafeFileUtil;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.core.Main;

public class ModuleFile extends ClientFile {
   private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final ModuleRepository moduleRepository;
   private final DraggableRepository draggableRepository;

   public ModuleFile(ModuleRepository moduleRepository, DraggableRepository draggableRepository) {
      super("moduleSettings");
      this.moduleRepository = moduleRepository;
      this.draggableRepository = draggableRepository;
   }

   private String getCurrentConfigName() {
      CurrentConfigFile currentConfigFile = this.getCurrentConfigFile();
      String configName = null;
      if (currentConfigFile != null && currentConfigFile.hasCurrentConfig()) {
         configName = currentConfigFile.getCurrentConfigName();
      }

      if (configName != null) {
         File configDir = new File(Main.getInstance().getClientInfoProvider().configsDir(), "config");
         File configFile = new File(configDir, configName + ".soup");
         if (configFile.exists()) {
            return configName;
         }
      }

      String firstAvailableConfig = this.findFirstAvailableConfig();
      if (firstAvailableConfig != null) {
         if (currentConfigFile != null) {
            currentConfigFile.setCurrentConfigName(firstAvailableConfig);

            try {
               currentConfigFile.saveToFile(Main.getInstance().getClientInfoProvider().filesDir());
            } catch (FileSaveException var5) {
            }
         }

         return firstAvailableConfig;
      } else {
         return "default";
      }
   }

   private String findFirstAvailableConfig() {
      try {
         File configDir = new File(Main.getInstance().getClientInfoProvider().configsDir(), "config");
         if (!configDir.exists()) {
            return null;
         }

         File[] configFiles = configDir.listFiles((dir, name) -> name.endsWith(".soup"));
         if (configFiles != null && configFiles.length > 0) {
            String fileName = configFiles[0].getName();
            return fileName.substring(0, fileName.length() - 5);
         }
      } catch (Exception var4) {
      }

      return null;
   }

   private CurrentConfigFile getCurrentConfigFile() {
      try {
         return Main.getInstance()
            .getFileRepository()
            .getClientFiles()
            .stream()
            .filter(file -> file instanceof CurrentConfigFile)
            .map(file -> (CurrentConfigFile)file)
            .findFirst()
            .orElse(null);
      } catch (Exception var2) {
         return null;
      }
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      String configName = this.getCurrentConfigName();
      this.saveToFile(new File(path, "config"), configName + ".soup");
   }

   @Override
   public void loadFromFile(File path) throws FileLoadException {
      String configName = this.getCurrentConfigName();
      this.loadFromFile(new File(path, "config"), configName + ".soup");
   }

   @Override
   public void saveToFile(File path, String fileName) throws FileSaveException {
      JsonObject functionObject = this.createJsonObjectFromModules();
      File file = new File(path, fileName);
      this.writeJsonToFile(functionObject, file);
      super.saveToFile(path, fileName);
   }

   @Override
   public void loadFromFile(File path, String fileName) throws FileLoadException {
      File file = new File(path, fileName);
      if (!file.exists()) {
         try {
            if (!file.getParentFile().exists()) {
               file.getParentFile().mkdirs();
            }

            JsonObject defaultConfig = this.createJsonObjectFromModules();
            this.writeJsonToFile(defaultConfig, file);
         } catch (FileSaveException var5) {
            throw new FileLoadException("Failed to create default config file", var5);
         }
      }

      JsonObject functionObject = this.readJsonFromFile(file);
      if (functionObject != null) {
         this.updateModulesFromJsonObject(functionObject);
      }

      super.loadFromFile(path, fileName);
   }

   private JsonObject createJsonObjectFromModules() {
      JsonObject functionObject = new JsonObject();

      for (Module module : this.moduleRepository.modules()) {
         JsonObject moduleObject = new JsonObject();
         moduleObject.addProperty("expande", module.isExpanded());
         moduleObject.addProperty("bind", module.getKey());
         moduleObject.addProperty("state", module.isState());
         module.settings().forEach(setting -> this.addSettingToJsonObject(moduleObject, setting));
         functionObject.add(module.getIdentifier().toLowerCase(), moduleObject);
      }

      for (AbstractDraggable draggable : this.draggableRepository.draggable()) {
         String key = draggable.getName().toLowerCase();
         JsonObject moduleObject = functionObject.has(key) ? functionObject.getAsJsonObject(key) : new JsonObject();
         moduleObject.addProperty("posX", draggable.getX());
         moduleObject.addProperty("posY", draggable.getY());
         functionObject.add(key, moduleObject);
      }

      return functionObject;
   }

   private void addSettingToJsonObject(JsonObject moduleObject, Setting setting) {
      if (setting instanceof BooleanSetting booleanSetting) {
         moduleObject.addProperty(setting.getNameKey(), booleanSetting.isValue());
      }

      if (setting instanceof ValueSetting valueSetting) {
         moduleObject.addProperty(setting.getNameKey(), valueSetting.getValue());
      }

      if (setting instanceof ColorSetting colorSetting) {
         moduleObject.addProperty(setting.getNameKey(), colorSetting.getColor());
      }

      if (setting instanceof BindSetting bindSetting) {
         moduleObject.addProperty(setting.getNameKey(), bindSetting.getKey());
      }

      if (setting instanceof TextSetting textSetting) {
         moduleObject.addProperty(setting.getNameKey(), textSetting.getText());
      }

      if (setting instanceof SelectSetting selectSetting) {
         moduleObject.addProperty(setting.getNameKey(), selectSetting.getSelected());
      }

      if (setting instanceof MultiSelectSetting multiSelectSetting) {
         List<String> selected = multiSelectSetting.getSelected();
         String selectedAsString = String.join(",", selected);
         moduleObject.addProperty(setting.getNameKey(), selectedAsString);
      }

      if (setting instanceof MultiColorSetting multiColorSetting) {
         JsonObject multiColorObject = new JsonObject();
         multiColorObject.addProperty("selectedColorIndex", multiColorSetting.getSelectedColorIndex());
         JsonArray colorsArray = new JsonArray();

         for (ColorSetting colorSetting : multiColorSetting.getAllColors()) {
            colorsArray.add(colorSetting.getColor());
         }

         multiColorObject.add("colors", colorsArray);
         moduleObject.add(setting.getNameKey(), multiColorObject);
      }

      if (setting instanceof GroupSetting groupSetting) {
         JsonObject groupObject = new JsonObject();
         groupObject.addProperty("state", groupSetting.isValue());

         for (Setting subSetting : groupSetting.getSubSettings()) {
            this.addSettingToJsonObject(groupObject, subSetting);
         }

         moduleObject.add(setting.getNameKey(), groupObject);
      }
   }

   private void writeJsonToFile(JsonObject functionObject, File file) throws FileSaveException {
      try {
         SafeFileUtil.atomicWrite(file, writer -> this.GSON.toJson(functionObject, writer));
      } catch (IOException var4) {
         throw new FileSaveException("Failed to save module to file", var4);
      }
   }

   private JsonObject readJsonFromFile(File file) throws FileLoadException {
      if (!SafeFileUtil.isFileValid(file)) {
         LoggerUtil.warn("Config file {} is corrupted, attempting backup restore...", file.getName());
         if (!SafeFileUtil.restoreFromBackup(file)) {
            LoggerUtil.warn("No valid backup for {}, will use defaults", file.getName());
            return null;
         }

         LoggerUtil.info("Restored {} from backup, retrying load", file.getName());
      }

      try {
         Object var16;
         try (Reader reader = SafeFileUtil.safeReader(file)) {
            if (reader == null) {
               return null;
            }

            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && !element.isJsonNull()) {
               return element.getAsJsonObject();
            }

            LoggerUtil.warn("Config file {} parsed as null", file.getName());
            var16 = null;
         }

         return (JsonObject)var16;
      } catch (IOException var11) {
         throw new FileLoadException("Failed to load module from file", var11);
      } catch (JsonIOException | IllegalStateException | JsonSyntaxException var12) {
         LoggerUtil.error("Failed to parse JSON from {}: {}", file.getName(), var12.getMessage());
         if (SafeFileUtil.restoreFromBackup(file)) {
            try {
               Object var5;
               try (Reader reader = SafeFileUtil.safeReader(file)) {
                  if (reader == null) {
                     return null;
                  }

                  JsonElement element = JsonParser.parseReader(reader);
                  if (element != null && !element.isJsonNull()) {
                     return element.getAsJsonObject();
                  }

                  var5 = null;
               }

               return (JsonObject)var5;
            } catch (Exception var9) {
               LoggerUtil.error("Backup also failed to parse for {}", file.getName());
               return null;
            }
         } else {
            return null;
         }
      }
   }

   private void updateModulesFromJsonObject(JsonObject functionObject) {
      for (Module module : this.moduleRepository.modules()) {
         JsonObject moduleObject = functionObject.getAsJsonObject(module.getIdentifier().toLowerCase());
         if (moduleObject != null) {
            if (moduleObject.has("expande")) {
               module.setExpanded(moduleObject.get("expande").getAsBoolean());
            }

            if (moduleObject.has("bind")) {
               module.setKey(moduleObject.get("bind").getAsInt());
            }

            if (moduleObject.has("state")) {
               module.setStateSilent(moduleObject.get("state").getAsBoolean());
            }

            module.settings().forEach(setting -> this.updateSettingFromJsonObject(moduleObject, setting));
         }
      }

      for (AbstractDraggable draggable : this.draggableRepository.draggable()) {
         JsonObject draggableObject = functionObject.getAsJsonObject(draggable.getName().toLowerCase());
         if (draggableObject != null && draggableObject.has("posX") && draggableObject.has("posY")) {
            draggable.setX(draggableObject.get("posX").getAsInt());
            draggable.setY(draggableObject.get("posY").getAsInt());
         }
      }
   }

   private void updateSettingFromJsonObject(JsonObject moduleObject, Setting setting) {
      JsonElement settingElement = moduleObject.get(setting.getNameKey());
      if (settingElement != null && !settingElement.isJsonNull()) {
         try {
            if (setting instanceof BooleanSetting booleanSetting) {
               booleanSetting.setValue(settingElement.getAsBoolean());
            }

            if (setting instanceof ValueSetting valueSetting) {
               valueSetting.setValue(settingElement.getAsFloat());
            }

            if (setting instanceof ColorSetting colorSetting) {
               colorSetting.setColor(settingElement.getAsInt());
            }

            if (setting instanceof BindSetting bindSetting) {
               bindSetting.setKey(settingElement.getAsInt());
            }

            if (setting instanceof TextSetting textSetting) {
               textSetting.setText(settingElement.getAsString());
            }

            if (setting instanceof SelectSetting selectSetting) {
               selectSetting.setSelected(settingElement.getAsString());
            }

            if (setting instanceof MultiSelectSetting multiSelectSetting) {
               String asString = settingElement.getAsString();
               List<String> selectedList = new ArrayList<>(Arrays.asList(asString.split(",")));
               selectedList.removeIf(s -> !multiSelectSetting.getList().contains(s));
               multiSelectSetting.setSelected(selectedList);
            }

            if (setting instanceof MultiColorSetting multiColorSetting) {
               JsonObject multiColorObject = settingElement.getAsJsonObject();
               if (multiColorObject.has("selectedColorIndex")) {
                  multiColorSetting.setSelectedColorIndex(multiColorObject.get("selectedColorIndex").getAsInt());
               }

               if (multiColorObject.has("colors")) {
                  JsonArray colorsArray = multiColorObject.getAsJsonArray("colors");
                  List<ColorSetting> colorSettings = multiColorSetting.getAllColors();

                  for (int i = 0; i < Math.min(colorsArray.size(), colorSettings.size()); i++) {
                     ColorSetting colorSetting = colorSettings.get(i);
                     int colorValue = colorsArray.get(i).getAsInt();
                     colorSetting.setColor(colorValue);
                  }
               }
            }

            if (setting instanceof GroupSetting groupSetting) {
               JsonObject groupObject = settingElement.getAsJsonObject();
               if (groupObject.has("state")) {
                  groupSetting.setValue(groupObject.get("state").getAsBoolean());
               }

               for (Setting subSetting : groupSetting.getSubSettings()) {
                  this.updateSettingFromJsonObject(groupObject, subSetting);
               }
            }
         } catch (Exception var11) {
            LoggerUtil.warn("Failed to load setting '{}': {}", setting.getNameKey(), var11.getMessage());
         }
      }
   }

   public void resetAllSettingsToDefaults() {
      for (Module module : this.moduleRepository.modules()) {
         module.setState(false);
         module.setExpanded(false);
         module.setKey(-1);

         for (Setting setting : module.settings()) {
            this.resetSettingToDefault(setting);
         }
      }
   }

   private void resetSettingToDefault(Setting setting) {
      setting.reset();
      if (setting instanceof GroupSetting groupSetting) {
         for (Setting subSetting : groupSetting.getSubSettings()) {
            this.resetSettingToDefault(subSetting);
         }
      }
   }
}
