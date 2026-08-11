package padej.soup.api.repository.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
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
import padej.soup.api.file.SafeFileUtil;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.core.Main;
import padej.soup.implement.features.modules.client.KeyBind;
import padej.soup.implement.menu.MenuScreen;

public class ConfigManager {
   public static final String CONFIG_FOLDER_NAME = "SoupAPI";
   public static final File MAIN_FOLDER = new File(MinecraftClient.getInstance().runDirectory, "SoupAPI");
   public static final File CONFIGS_FOLDER = new File(MAIN_FOLDER, "configs");
   public static final File TEMP_FOLDER = new File(MAIN_FOLDER, "temp");
   public static final File FILES_FOLDER = new File(MinecraftClient.getInstance().runDirectory, "SoupAPI/files");
   private File currentConfig = null;
   private static boolean firstLaunch = false;
   private final ModuleRepository moduleRepository;
   private DraggableRepository draggableRepository;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final ScheduledExecutorService autoSaveScheduler = Executors.newSingleThreadScheduledExecutor();
   private volatile long lastChangeTime = 0L;
   private volatile boolean isLoadingConfig = false;
   private static final long SAVE_DELAY_IN_MENU_MS = 1000L;
   private static final long SAVE_DELAY_IN_GAME_MS = 10000L;

   public ConfigManager(ModuleRepository moduleRepository) {
      this.moduleRepository = moduleRepository;
      firstLaunch = !MAIN_FOLDER.exists();
      this.createDirs(MAIN_FOLDER, CONFIGS_FOLDER, TEMP_FOLDER);
      Setting.setGlobalChangeListener(setting -> {
         if (!this.isLoadingConfig) {
            this.scheduleSave();
         }
      });
      Module.setGlobalStateChangeListener(module -> {
         if (!this.isLoadingConfig) {
            this.scheduleSave();
         }
      });
      this.autoSaveScheduler.scheduleWithFixedDelay(() -> {
         if (!this.isLoadingConfig && this.lastChangeTime > 0L) {
            long timeSinceLastChange = System.currentTimeMillis() - this.lastChangeTime;
            long saveDelay = this.isInMenuScreen() ? 1000L : 10000L;
            if (timeSinceLastChange >= saveDelay) {
               try {
                  this.saveCurrentActiveConfig();
                  this.lastChangeTime = 0L;
               } catch (Exception var6) {
                  LoggerUtil.error("Failed to auto-save config", var6);
               }
            }
         }
      }, 200L, 200L, TimeUnit.MILLISECONDS);
   }

   private void createDirs(File... dirs) {
      for (File dir : dirs) {
         if (!dir.exists()) {
            dir.mkdirs();
         }
      }
   }

   public void scheduleSave() {
      this.lastChangeTime = System.currentTimeMillis();
   }

   public void reloadAllModuleSettings() {
      if (this.currentConfig != null && this.currentConfig.exists()) {
         this.isLoadingConfig = true;

         try {
            JsonObject rootObject = this.safeReadConfigJson(this.currentConfig);
            if (rootObject != null) {
               JsonArray modules = rootObject.getAsJsonArray("Modules");
               if (modules != null) {
                  for (JsonElement element : modules) {
                     try {
                        this.parseModule(element.getAsJsonObject());
                     } catch (Exception var9) {
                        LoggerUtil.error("Failed to parse module during reload: {}", var9.getMessage());
                     }
                  }
               }
            }

            this.onLoad();
         } finally {
            this.isLoadingConfig = false;
         }
      }
   }

   private JsonObject safeReadConfigJson(File config) {
      if (!SafeFileUtil.isFileValid(config)) {
         LoggerUtil.warn("Config file {} is corrupted, attempting backup restore...", config.getName());
         if (!SafeFileUtil.restoreFromBackup(config)) {
            LoggerUtil.error("No valid backup for {}, file is unrecoverable", config.getName());
            return null;
         }

         LoggerUtil.info("Restored {} from backup", config.getName());
      }

      try {
         Object var16;
         try (Reader reader = SafeFileUtil.safeReader(config)) {
            if (reader == null) {
               return null;
            }

            JsonElement element = JsonParser.parseReader(reader);
            if (element != null && !element.isJsonNull()) {
               return element.getAsJsonObject();
            }

            var16 = null;
         }

         return (JsonObject)var16;
      } catch (IllegalStateException | JsonSyntaxException var11) {
         LoggerUtil.error("JSON parse error in {}: {}", config.getName(), var11.getMessage());
         if (SafeFileUtil.restoreFromBackup(config)) {
            try {
               Object var5;
               try (Reader reader = SafeFileUtil.safeReader(config)) {
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
               LoggerUtil.error("Backup also failed for {}", config.getName());
               return null;
            }
         } else {
            return null;
         }
      } catch (IOException var12) {
         LoggerUtil.error("IO error reading {}: {}", config.getName(), var12.getMessage());
         return null;
      }
   }

   private boolean isInMenuScreen() {
      try {
         MinecraftClient mc = Main.mc;
         return mc.currentScreen instanceof MenuScreen;
      } catch (Exception var2) {
         return false;
      }
   }

   private void saveCurrentActiveConfig() {
      if (this.currentConfig != null && this.currentConfig.exists()) {
         this.save(this.currentConfig);
         LoggerUtil.info("Auto-saved config: " + this.currentConfig.getName());
      }
   }

   public void load(String name) {
      File file = new File(CONFIGS_FOLDER, name + ".soup");
      if (!file.exists()) {
         LoggerUtil.warn("Config " + name + " does not exist!");
      } else {
         if (this.currentConfig != null) {
            this.save(this.currentConfig);
         }

         this.isLoadingConfig = true;

         try {
            this.onUnload();
            this.load(file);
            this.onLoad();
         } finally {
            this.isLoadingConfig = false;
         }
      }
   }

   private void load(@NotNull File config) {
      if (!config.exists()) {
         this.save(config);
      }

      this.isLoadingConfig = true;

      try {
         JsonObject rootObject = this.safeReadConfigJson(config);
         if (rootObject == null) {
            LoggerUtil.warn("Config {} could not be loaded, using defaults", config.getName());
         } else {
            JsonArray modules = rootObject.getAsJsonArray("Modules");
            if (modules != null) {
               for (JsonElement element : modules) {
                  try {
                     this.parseModule(element.getAsJsonObject());
                  } catch (Exception var13) {
                     LoggerUtil.error("Failed to parse module: {}", var13.getMessage());
                  }
               }
            }

            if (this.draggableRepository != null) {
               JsonArray draggables = rootObject.getAsJsonArray("Draggables");
               if (draggables != null) {
                  for (JsonElement element : draggables) {
                     try {
                        this.parseDraggable(element.getAsJsonObject());
                     } catch (Exception var12) {
                        LoggerUtil.error("Failed to parse draggable: {}", var12.getMessage());
                     }
                  }
               }
            }

            LoggerUtil.info("Loaded config: " + config.getName());
         }

         this.currentConfig = config;
         this.saveCurrentConfig();
         this.onLoad();
      } finally {
         this.isLoadingConfig = false;
      }
   }

   public void save(String name) {
      File file = new File(CONFIGS_FOLDER, name + ".soup");
      this.save(file);
   }

   public void save(@NotNull File config) {
      try {
         JsonObject rootObject = new JsonObject();
         rootObject.add("Modules", this.getModuleArray());
         if (this.draggableRepository != null) {
            rootObject.add("Draggables", this.getDraggableArray());
         }

         SafeFileUtil.atomicWrite(config, writer -> this.gson.toJson(rootObject, writer));
      } catch (IOException var3) {
         LoggerUtil.error("Failed to save config", var3);
      }
   }

   private void parseModule(@NotNull JsonObject object) throws NullPointerException {
      Module module = this.moduleRepository.modules().stream().filter(m -> object.has(m.getIdentifier())).findFirst().orElse(null);
      if (module != null) {
         JsonObject moduleObject = object.getAsJsonObject(module.getIdentifier());
         if (moduleObject.has("enabled")) {
            module.setStateSilent(moduleObject.get("enabled").getAsBoolean());
         }

         if (moduleObject.has("bind")) {
            module.setKey(moduleObject.get("bind").getAsInt());
         }

         for (Setting setting : module.settings()) {
            try {
               this.loadSetting(setting, moduleObject);
            } catch (Exception var7) {
               LoggerUtil.error("[SoupAPI] Module: " + module.getIdentifier() + " Setting: " + setting.getNameKey() + " Error: ", var7);
            }
         }
      }
   }

   private void parseDraggable(@NotNull JsonObject object) throws NullPointerException {
      AbstractDraggable draggable = this.draggableRepository.draggable().stream().filter(d -> object.has(d.getName())).findFirst().orElse(null);
      if (draggable != null) {
         JsonObject draggableObject = object.getAsJsonObject(draggable.getName());
         if (draggableObject.has("x")) {
            draggable.setX(draggableObject.get("x").getAsInt());
         }

         if (draggableObject.has("y")) {
            draggable.setY(draggableObject.get("y").getAsInt());
         }
      }
   }

   private void loadSetting(Setting setting, JsonObject moduleObject) {
      if (setting.isSaveToConfig()) {
         String key = setting.getNameKey();
         if (moduleObject.has(key)) {
            JsonElement element = moduleObject.get(key);
            switch (setting) {
               case BooleanSetting booleanSetting:
                  booleanSetting.setValue(element.getAsBoolean());
                  break;
               case ValueSetting valueSetting:
                  valueSetting.setValue(element.getAsFloat());
                  break;
               case TextSetting textSetting: {
                  String value = element.getAsString();
                  value = value.replace("%%", " ").replace("++", "/");
                  textSetting.setText(value);
                  break;
               }
               case BindSetting bindSetting:
                  bindSetting.setKey(element.getAsInt());
                  break;
               case ColorSetting colorSetting:
                  colorSetting.setColor(element.getAsInt());
                  break;
               case SelectSetting selectSetting:
                  selectSetting.setSelected(element.getAsString());
                  break;
               case MultiSelectSetting multiSelectSetting: {
                  String value = element.getAsString();
                  List<String> selected = new ArrayList<>(Arrays.asList(value.split(",")));
                  selected.removeIf(s -> !((MultiSelectSetting)setting).getList().contains(s));
                  multiSelectSetting.setSelected(selected);
                  break;
               }
               case MultiColorSetting multiColor:
                  if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                     int oldColor = element.getAsInt();
                     if (multiColor.getColor1() != null) {
                        multiColor.getColor1().setColor(oldColor);
                     }

                     return;
                  }

                  JsonObject colorObject = element.getAsJsonObject();
                  if (colorObject.has("selectedColorIndex")) {
                     multiColor.setSelectedColorIndex(colorObject.get("selectedColorIndex").getAsInt());
                  }

                  if (colorObject.has("colors")) {
                     JsonArray colorsArray = colorObject.getAsJsonArray("colors");
                     List<ColorSetting> colorSettings = multiColor.getAllColors();

                     for (int i = 0; i < Math.min(colorsArray.size(), colorSettings.size()); i++) {
                        colorSettings.get(i).setColor(colorsArray.get(i).getAsInt());
                     }
                  }
                  break;
               case GroupSetting group:
                  label63: {
                     JsonObject groupObject = element.getAsJsonObject();
                     if (groupObject.has("state")) {
                        group.setValue(groupObject.get("state").getAsBoolean());
                     }

                     for (Setting subSetting : group.getSubSettings()) {
                        this.loadSetting(subSetting, groupObject);
                     }
                     break label63;
                  }
               default:
            }
         }
      }
   }

   @NotNull
   private JsonArray getModuleArray() {
      JsonArray modulesArray = new JsonArray();

      for (Module module : this.moduleRepository.modules()) {
         modulesArray.add(this.getModuleObject(module));
      }

      return modulesArray;
   }

   @NotNull
   private JsonArray getDraggableArray() {
      JsonArray draggablesArray = new JsonArray();

      for (AbstractDraggable draggable : this.draggableRepository.draggable()) {
         draggablesArray.add(this.getDraggableObject(draggable));
      }

      return draggablesArray;
   }

   @NotNull
   private JsonObject getDraggableObject(@NotNull AbstractDraggable draggable) {
      JsonObject draggableContainer = new JsonObject();
      JsonObject draggableData = new JsonObject();
      draggableData.addProperty("x", draggable.getX());
      draggableData.addProperty("y", draggable.getY());
      draggableContainer.add(draggable.getName(), draggableData);
      return draggableContainer;
   }

   @NotNull
   private JsonObject getModuleObject(@NotNull Module module) {
      JsonObject moduleContainer = new JsonObject();
      JsonObject moduleData = new JsonObject();
      moduleData.addProperty("enabled", module.isState());
      moduleData.addProperty("bind", module.getKey());

      for (Setting setting : module.settings()) {
         this.saveSetting(setting, moduleData);
      }

      moduleContainer.add(module.getIdentifier(), moduleData);
      return moduleContainer;
   }

   private void saveSetting(Setting setting, JsonObject moduleData) {
      if (setting.isSaveToConfig()) {
         String key = setting.getNameKey();
         switch (setting) {
            case BooleanSetting booleanSetting:
               moduleData.addProperty(key, booleanSetting.isValue());
               break;
            case ValueSetting valueSetting:
               moduleData.addProperty(key, valueSetting.getValue());
               break;
            case TextSetting textSetting:
               String value = textSetting.getText();
               value = value.replace(" ", "%%").replace("/", "++");
               moduleData.addProperty(key, value);
               break;
            case BindSetting bindSetting:
               moduleData.addProperty(key, bindSetting.getKey());
               break;
            case ColorSetting colorSetting:
               moduleData.addProperty(key, colorSetting.getColor());
               break;
            case SelectSetting selectSetting:
               moduleData.addProperty(key, selectSetting.getSelected());
               break;
            case MultiSelectSetting multiSelectSetting:
               List<String> selected = multiSelectSetting.getSelected();
               moduleData.addProperty(key, String.join(",", selected));
               break;
            case MultiColorSetting multiColor:
               JsonObject colorObject = new JsonObject();
               colorObject.addProperty("selectedColorIndex", multiColor.getSelectedColorIndex());
               JsonArray colorsArray = new JsonArray();

               for (ColorSetting color : multiColor.getAllColors()) {
                  colorsArray.add(color.getColor());
               }

               colorObject.add("colors", colorsArray);
               moduleData.add(key, colorObject);
               break;
            case GroupSetting group:
               JsonObject groupObject = new JsonObject();
               groupObject.addProperty("state", group.isValue());

               for (Setting subSetting : group.getSubSettings()) {
                  this.saveSetting(subSetting, groupObject);
               }

               moduleData.add(key, groupObject);
               return;
            default:
         }
      }
   }

   public void delete(@NotNull File file) {
      file.delete();
   }

   public void delete(String name) {
      File file = new File(CONFIGS_FOLDER, name + ".soup");
      if (file.exists()) {
         this.delete(file);
      }
   }

   public void saveCurrentConfig() {
      if (this.currentConfig != null) {
         if (!FILES_FOLDER.exists()) {
            FILES_FOLDER.mkdirs();
         }

         File file = new File(FILES_FOLDER, "currentCfg");

         try {
            SafeFileUtil.atomicWrite(file, writer -> writer.write(this.currentConfig.getName().replace(".soup", "")));
         } catch (Exception var3) {
            LoggerUtil.error("Failed to save current config name", var3);
         }
      }
   }

   public File loadCurrentConfig() {
      if (!FILES_FOLDER.exists()) {
         FILES_FOLDER.mkdirs();
      }

      File file = new File(FILES_FOLDER, "currentCfg");
      String name = "default";

      try {
         if (file.exists()) {
            if (!SafeFileUtil.isFileValid(file)) {
               LoggerUtil.warn("currentCfg file corrupted, attempting backup restore...");
               SafeFileUtil.restoreFromBackup(file);
            }

            if (file.exists() && SafeFileUtil.isFileValid(file)) {
               try (Scanner reader = new Scanner(file)) {
                  while (reader.hasNextLine()) {
                     name = reader.nextLine();
                  }
               }
            }
         }
      } catch (Exception var8) {
         LoggerUtil.error("Failed to load current config name", var8);
      }

      this.currentConfig = new File(CONFIGS_FOLDER, name + ".soup");
      return this.currentConfig;
   }

   private void onUnload() {
      for (Module module : this.moduleRepository.modules()) {
         if (module.isEnabled()) {
            module.deactivate();
            module.setStateSilent(false);
         }
      }
   }

   private void onLoad() {
      for (Module module : this.moduleRepository.modules()) {
         if (module.isEnabled()) {
            module.setStateSilent(true);
         }
      }

      KeyBind.reloadGlobalSettings();
   }

   public void createDefaultConfig() {
      File defaultConfig = new File(CONFIGS_FOLDER, "default.soup");
      if (!defaultConfig.exists()) {
         this.save(defaultConfig);
         this.currentConfig = defaultConfig;
         this.saveCurrentConfig();
         LoggerUtil.info("Created default config");
      }
   }

   public void initialize() {
      this.createDefaultConfig();
      File config = this.loadCurrentConfig();
      this.isLoadingConfig = true;

      try {
         if (config.exists()) {
            this.load(config);
         } else {
            this.createDefaultConfig();
         }
      } finally {
         this.isLoadingConfig = false;
      }
   }

   public void shutdown() {
      try {
         if (this.lastChangeTime > 0L) {
            this.saveCurrentActiveConfig();
            this.lastChangeTime = 0L;
         }

         this.autoSaveScheduler.shutdown();
         if (!this.autoSaveScheduler.awaitTermination(5L, TimeUnit.SECONDS)) {
            this.autoSaveScheduler.shutdownNow();
         }
      } catch (InterruptedException var2) {
         this.autoSaveScheduler.shutdownNow();
         Thread.currentThread().interrupt();
      }
   }

   public File getCurrentConfig() {
      return this.currentConfig;
   }

   public void setCurrentConfig(File currentConfig) {
      this.currentConfig = currentConfig;
   }

   public static boolean isFirstLaunch() {
      return firstLaunch;
   }

   public void setDraggableRepository(DraggableRepository draggableRepository) {
      this.draggableRepository = draggableRepository;
   }
}
