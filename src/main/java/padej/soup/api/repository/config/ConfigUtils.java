package padej.soup.api.repository.config;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import padej.soup.base.util.logger.LoggerUtil;

public class ConfigUtils {
   private static final List<Config> configs = new CopyOnWriteArrayList<>();
   private static volatile String activeConfigName = "default";
   private static final Object LOCK = new Object();
   private static ConfigManager configManager;

   public static void addConfig(Config config) {
      synchronized (LOCK) {
         if (!configs.contains(config)) {
            configs.add(config);
         }
      }
   }

   public static void removeConfig(Config config) {
      synchronized (LOCK) {
         configs.remove(config);
      }
   }

   public static List<Config> getConfigs() {
      return new ArrayList<>(configs);
   }

   public static boolean isConfig(String name) {
      return configs.stream().anyMatch(config -> config.getName().equals(name));
   }

   public static Config getConfig(String name) {
      return configs.stream().filter(config -> config.getName().equals(name)).findFirst().orElse(null);
   }

   public static List<Config> searchConfigs(String searchText) {
      if (searchText != null && !searchText.trim().isEmpty()) {
         String lowerSearch = searchText.toLowerCase();
         return configs.stream()
            .filter(
               config -> config.getName().toLowerCase().contains(lowerSearch)
                  || config.getDescription() != null && config.getDescription().toLowerCase().contains(lowerSearch)
            )
            .collect(Collectors.toList());
      } else {
         return new ArrayList<>(configs);
      }
   }

   public static void clear() {
      configs.clear();
   }

   public static void initializeConfigManager(ConfigManager manager) {
      synchronized (LOCK) {
         configManager = manager;
         configManager.initialize();
         loadConfigsFromDirectory();
         LoggerUtil.info("ConfigManager initialized");
      }
   }

   public static void loadConfigsFromDirectory() {
      synchronized (LOCK) {
         File configDir = ConfigManager.CONFIGS_FOLDER;
         if (!configDir.exists()) {
            configDir.mkdirs();
            LoggerUtil.info("Created config directory: " + configDir.getAbsolutePath());
         }

         File[] configFiles = configDir.listFiles((dir, name) -> name.endsWith(".soup"));
         List<Config> newConfigs = new ArrayList<>();
         if (configFiles != null) {
            for (File file : configFiles) {
               if (!isValidJsonFile(file)) {
                  LoggerUtil.warn("Skipping invalid JSON config file: " + file.getName());
               } else {
                  Config config = getConfig(file);
                  newConfigs.add(config);
               }
            }
         }

         configs.removeIf(existingConfig -> newConfigs.stream().noneMatch(newConfig -> newConfig.getFileName().equals(existingConfig.getFileName())));

         for (Config newConfig : newConfigs) {
            Config existing = configs.stream().filter(c -> c.getFileName().equals(newConfig.getFileName())).findFirst().orElse(null);
            if (existing != null) {
               existing.setName(newConfig.getName());
               existing.setDescription(newConfig.getDescription());
               existing.setLastModified(newConfig.getLastModified());
            } else {
               configs.add(newConfig);
            }
         }

         if (configManager != null && configManager.getCurrentConfig() != null) {
            String configNameFromFile = configManager.getCurrentConfig().getName().replace(".soup", "");
            Config matchingConfig = configs.stream().filter(configx -> configx.getName().equalsIgnoreCase(configNameFromFile)).findFirst().orElse(null);
            if (matchingConfig != null) {
               activeConfigName = matchingConfig.getName();
            } else if (!configs.isEmpty()) {
               activeConfigName = configs.get(0).getName();
            }
         }
      }
   }

   @NotNull
   private static Config getConfig(File file) {
      String fileName = file.getName();
      String configName = fileName.substring(0, fileName.lastIndexOf(46));
      if (!configName.isEmpty()) {
         configName = configName.substring(0, 1).toUpperCase() + configName.substring(1);
      }

      String description = "Custom configuration";
      String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf(46));
      if ("default".equals(fileNameWithoutExt) || "defaultCfg".equals(fileNameWithoutExt)) {
         description = "Default configuration";
      }

      return new Config(configName, description, fileName, file.lastModified());
   }

   public static void loadCurrentConfigFromFile() {
      synchronized (LOCK) {
         if (configManager != null) {
            File config = configManager.loadCurrentConfig();
            if (config != null && config.exists()) {
               String configName = config.getName().replace(".soup", "");
               if (isConfig(configName)) {
                  activeConfigName = configName;
                  return;
               }
            }
         }

         setFirstConfigAsActive();
      }
   }

   public static void saveCurrentConfigToFile() {
      synchronized (LOCK) {
         if (configManager != null) {
            configManager.saveCurrentConfig();
         }
      }
   }

   private static void setFirstConfigAsActive() {
      if (!configs.isEmpty()) {
         activeConfigName = configs.get(0).getName();
         saveCurrentConfigToFile();
      } else {
         activeConfigName = "default";
      }
   }

   public static File getConfigDirectory() {
      return ConfigManager.CONFIGS_FOLDER;
   }

   public static void setActiveConfig(String configName) {
      synchronized (LOCK) {
         if (isConfig(configName)) {
            activeConfigName = configName;
            if (configManager != null) {
               configManager.load(configName);
            }

            saveCurrentConfigToFile();
         }
      }
   }

   public static void setActiveConfigName(String configName) {
      synchronized (LOCK) {
         activeConfigName = configName;
      }
   }

   public static void saveActiveConfig() {
      synchronized (LOCK) {
         if (configManager != null) {
            configManager.save(activeConfigName);
         }
      }
   }

   public static void deleteConfig(String configName) {
      synchronized (LOCK) {
         if (!isConfig(configName)) {
            LoggerUtil.warn("Cannot delete non-existent config: " + configName);
         } else if (!canDeleteConfig()) {
            LoggerUtil.warn("Cannot delete the last config");
         } else {
            if (isActiveConfig(configName)) {
               String nextConfig = getNextConfigForDeletion(configName);
               if (nextConfig != null) {
                  setActiveConfig(nextConfig);
               }
            }

            if (configManager != null) {
               configManager.delete(configName);
            }

            Config config = getConfig(configName);
            if (config != null) {
               removeConfig(config);
            }
         }
      }
   }

   public static boolean isActiveConfig(String configName) {
      return activeConfigName.equals(configName);
   }

   public static boolean canDeleteConfig() {
      return configs.size() > 1;
   }

   public static String getNextConfigForDeletion(String configToDelete) {
      List<Config> availableConfigs = configs.stream().filter(config -> !config.getName().equals(configToDelete)).toList();
      return !availableConfigs.isEmpty() ? availableConfigs.getFirst().getName() : null;
   }

   private static boolean isValidJsonFile(File file) {
      try {
         boolean var2;
         try (FileReader reader = new FileReader(file)) {
            if (file.length() == 0L) {
               return false;
            }

            JsonParser.parseReader(reader);
            var2 = true;
         }

         return var2;
      } catch (JsonSyntaxException var6) {
         LoggerUtil.warn("Invalid JSON in file: " + file.getName());
         return false;
      } catch (Exception var7) {
         LoggerUtil.warn("Error reading file: " + file.getName() + " - " + var7.getMessage());
         return false;
      }
   }

   public static int getConfigCount() {
      return configs.size();
   }

   public static boolean hasConfigs() {
      return !configs.isEmpty();
   }

   @Nullable
   public static Config getConfigByIndex(int index) {
      return index >= 0 && index < configs.size() ? configs.get(index) : null;
   }

   public static String getConfigSize(String configName) {
      Config config = getConfig(configName);
      if (config == null) {
         return "N/A";
      } else {
         File configFile = new File(ConfigManager.CONFIGS_FOLDER, config.getFileName());
         if (!configFile.exists()) {
            return "N/A";
         } else {
            long sizeInBytes = configFile.length();
            double sizeInKB = sizeInBytes / 1024.0;
            if (sizeInKB < 1.0) {
               return String.format("%.2f B", (double)sizeInBytes);
            } else {
               return sizeInKB < 1024.0 ? String.format("%.2f KB", sizeInKB) : String.format("%.2f MB", sizeInKB / 1024.0);
            }
         }
      }
   }

   public static String getConfigLastModified(String configName) {
      Config config = getConfig(configName);
      if (config == null) {
         return "N/A";
      } else {
         long timestamp = config.getLastModified();
         if (timestamp <= 0L) {
            return "N/A";
         } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
            return sdf.format(new Date(timestamp));
         }
      }
   }

   public static String getActiveConfigName() {
      return activeConfigName;
   }

   public static ConfigManager getConfigManager() {
      return configManager;
   }
}
