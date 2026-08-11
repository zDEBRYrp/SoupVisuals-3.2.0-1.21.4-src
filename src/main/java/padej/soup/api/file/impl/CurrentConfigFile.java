package padej.soup.api.file.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import padej.soup.api.file.ClientFile;
import padej.soup.api.file.SafeFileUtil;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;
import padej.soup.base.util.logger.LoggerUtil;

public class CurrentConfigFile extends ClientFile {
   private String currentConfigName = null;

   public CurrentConfigFile() {
      super("currentCfg");
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      this.saveToFile(path, this.getName());
   }

   @Override
   public void loadFromFile(File path) throws FileLoadException {
      this.loadFromFile(path, this.getName());
   }

   @Override
   public void saveToFile(File path, String fileName) throws FileSaveException {
      if (this.currentConfigName != null && !this.currentConfigName.trim().isEmpty()) {
         File file = new File(path, fileName);

         try {
            SafeFileUtil.atomicWrite(file, writer -> writer.write(this.currentConfigName.trim()));
         } catch (IOException var5) {
            throw new FileSaveException("Failed to save current config name to file", var5);
         }

         super.saveToFile(path, fileName);
      }
   }

   @Override
   public void loadFromFile(File path, String fileName) throws FileLoadException {
      File file = new File(path, fileName);
      if (!file.exists()) {
         this.currentConfigName = null;
      } else {
         if (!SafeFileUtil.isFileValid(file)) {
            LoggerUtil.warn("CurrentConfig file corrupted, attempting backup restore...");
            if (!SafeFileUtil.restoreFromBackup(file)) {
               LoggerUtil.warn("No valid backup for currentCfg, will use null");
               this.currentConfigName = null;
               return;
            }
         }

         try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
               this.currentConfigName = line.trim();
            } else {
               this.currentConfigName = null;
            }
         } catch (IOException var9) {
            throw new FileLoadException("Failed to load current config name from file", var9);
         }

         super.loadFromFile(path, fileName);
      }
   }

   public boolean hasCurrentConfig() {
      return this.currentConfigName != null && !this.currentConfigName.trim().isEmpty();
   }

   public void setCurrentConfigName(String currentConfigName) {
      this.currentConfigName = currentConfigName;
   }

   public String getCurrentConfigName() {
      return this.currentConfigName;
   }
}
