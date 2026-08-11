package padej.soup.api.addon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.MinecraftClient;
import padej.soup.api.file.SafeFileUtil;
import padej.soup.base.util.logger.LoggerUtil;

public final class AddonConfig {
   private static final File ADDONS_FOLDER = new File(MinecraftClient.getInstance().runDirectory, "SoupAPI/addons");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final String addonId;
   private final File file;
   private JsonObject data = new JsonObject();

   public AddonConfig(String addonId) {
      this.addonId = addonId;
      this.file = new File(ADDONS_FOLDER, addonId + ".json");
   }

   public void load() {
      if (!this.file.exists()) {
         LoggerUtil.info("[AddonConfig:" + this.addonId + "] No config file found, starting fresh.");
      } else {
         if (!SafeFileUtil.isFileValid(this.file)) {
            LoggerUtil.warn("[AddonConfig:" + this.addonId + "] File corrupted, attempting backup restore...");
            if (!SafeFileUtil.restoreFromBackup(this.file)) {
               LoggerUtil.warn("[AddonConfig:" + this.addonId + "] No valid backup, starting fresh.");
               return;
            }
         }

         try (FileReader reader = new FileReader(this.file, StandardCharsets.UTF_8)) {
            JsonElement element = (JsonElement)GSON.fromJson(reader, JsonElement.class);
            if (element != null && element.isJsonObject()) {
               this.data = element.getAsJsonObject();
               LoggerUtil.info("[AddonConfig:" + this.addonId + "] Loaded " + this.data.size() + " entries.");
            }
         } catch (IOException var6) {
            LoggerUtil.error("[AddonConfig:" + this.addonId + "] Failed to load: " + var6.getMessage());
         }
      }
   }

   public void save() {
      try {
         if (!ADDONS_FOLDER.exists() && !ADDONS_FOLDER.mkdirs()) {
            LoggerUtil.error("[AddonConfig:" + this.addonId + "] Failed to create addons folder.");
            return;
         }

         SafeFileUtil.atomicWrite(this.file, writer -> GSON.toJson(this.data, writer));
         LoggerUtil.info("[AddonConfig:" + this.addonId + "] Saved.");
      } catch (IOException var2) {
         LoggerUtil.error("[AddonConfig:" + this.addonId + "] Failed to save: " + var2.getMessage());
      }
   }

   public void set(String key, String value) {
      this.data.addProperty(key, value);
   }

   public void set(String key, int value) {
      this.data.addProperty(key, value);
   }

   public void set(String key, long value) {
      this.data.addProperty(key, value);
   }

   public void set(String key, double value) {
      this.data.addProperty(key, value);
   }

   public void set(String key, boolean value) {
      this.data.addProperty(key, value);
   }

   public void set(String key, Object value) {
      this.data.add(key, GSON.toJsonTree(value));
   }

   public void remove(String key) {
      this.data.remove(key);
   }

   public String getString(String key, String defaultValue) {
      JsonElement el = this.data.get(key);
      return el != null && el.isJsonPrimitive() ? el.getAsString() : defaultValue;
   }

   public int getInt(String key, int defaultValue) {
      JsonElement el = this.data.get(key);

      try {
         return el != null && el.isJsonPrimitive() ? el.getAsInt() : defaultValue;
      } catch (NumberFormatException var5) {
         return defaultValue;
      }
   }

   public long getLong(String key, long defaultValue) {
      JsonElement el = this.data.get(key);

      try {
         return el != null && el.isJsonPrimitive() ? el.getAsLong() : defaultValue;
      } catch (NumberFormatException var6) {
         return defaultValue;
      }
   }

   public double getDouble(String key, double defaultValue) {
      JsonElement el = this.data.get(key);

      try {
         return el != null && el.isJsonPrimitive() ? el.getAsDouble() : defaultValue;
      } catch (NumberFormatException var6) {
         return defaultValue;
      }
   }

   public boolean getBoolean(String key, boolean defaultValue) {
      JsonElement el = this.data.get(key);
      return el != null && el.isJsonPrimitive() ? el.getAsBoolean() : defaultValue;
   }

   public <T> T getObject(String key, Class<T> clazz) {
      JsonElement el = this.data.get(key);
      return (T)(el != null ? GSON.fromJson(el, clazz) : null);
   }

   public boolean has(String key) {
      return this.data.has(key);
   }

   public void clear() {
      this.data = new JsonObject();
   }

   public JsonObject getRaw() {
      return this.data;
   }

   public String getAddonId() {
      return this.addonId;
   }

   public File getFile() {
      return this.file;
   }
}
