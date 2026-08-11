package padej.soup.implement.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import padej.soup.api.file.ClientFile;
import padej.soup.api.file.SafeFileUtil;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;
import padej.soup.base.util.logger.LoggerUtil;

public class MenuBindFile extends ClientFile {
   private int menuKey = 48;

   public MenuBindFile() {
      super("menuBind");
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
      File file = new File(path, fileName);

      try {
         SafeFileUtil.atomicWrite(file, writer -> writer.write(String.valueOf(this.menuKey)));
      } catch (IOException var5) {
         throw new FileSaveException("Failed to save menu key to file", var5);
      }

      super.saveToFile(path, fileName);
   }

   @Override
   public void loadFromFile(File path, String fileName) throws FileLoadException {
      File file = new File(path, fileName);
      if (!file.exists()) {
         this.menuKey = 48;
      } else {
         if (!SafeFileUtil.isFileValid(file)) {
            LoggerUtil.warn("MenuBind file corrupted, attempting backup restore...");
            if (!SafeFileUtil.restoreFromBackup(file)) {
               LoggerUtil.warn("No valid backup, defaulting to GLFW_KEY_0");
               this.menuKey = 48;
               return;
            }
         }

         try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
               try {
                  this.menuKey = Integer.parseInt(line.trim());
               } catch (NumberFormatException var8) {
                  this.menuKey = 48;
               }
            } else {
               this.menuKey = 48;
            }
         } catch (IOException var10) {
            throw new FileLoadException("Failed to load menu key from file", var10);
         }

         super.loadFromFile(path, fileName);
      }
   }

   public void setMenuKey(int menuKey) {
      this.menuKey = menuKey;
   }

   public int getMenuKey() {
      return this.menuKey;
   }
}
