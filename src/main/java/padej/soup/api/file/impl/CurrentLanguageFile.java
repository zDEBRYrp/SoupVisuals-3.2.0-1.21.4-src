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

public class CurrentLanguageFile extends ClientFile {
   private String currentLanguage = "en_us";

   public CurrentLanguageFile() {
      super("currentLang");
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
      if (this.currentLanguage == null || this.currentLanguage.trim().isEmpty()) {
         this.currentLanguage = "en_us";
      }

      File file = new File(path, fileName);

      try {
         SafeFileUtil.atomicWrite(file, writer -> writer.write(this.currentLanguage.trim()));
      } catch (IOException var5) {
         throw new FileSaveException("Failed to save current language to file", var5);
      }

      super.saveToFile(path, fileName);
   }

   @Override
   public void loadFromFile(File path, String fileName) throws FileLoadException {
      File file = new File(path, fileName);
      if (!file.exists()) {
         this.currentLanguage = "en_us";
      } else {
         if (!SafeFileUtil.isFileValid(file)) {
            LoggerUtil.warn("Language file corrupted, attempting backup restore...");
            if (!SafeFileUtil.restoreFromBackup(file)) {
               LoggerUtil.warn("No valid backup, defaulting to en_us");
               this.currentLanguage = "en_us";
               return;
            }
         }

         try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
               this.currentLanguage = line.trim();
            } else {
               this.currentLanguage = "en_us";
            }
         } catch (IOException var9) {
            throw new FileLoadException("Failed to load current language from file", var9);
         }

         super.loadFromFile(path, fileName);
      }
   }

   public boolean hasCurrentLanguage() {
      return this.currentLanguage != null && !this.currentLanguage.trim().isEmpty();
   }

   public void setCurrentLanguage(String currentLanguage) {
      this.currentLanguage = currentLanguage;
   }

   public String getCurrentLanguage() {
      return this.currentLanguage;
   }
}
