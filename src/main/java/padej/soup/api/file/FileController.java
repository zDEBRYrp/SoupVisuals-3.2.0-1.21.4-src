package padej.soup.api.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;
import padej.soup.api.file.impl.ModuleFile;
import padej.soup.base.util.logger.LoggerUtil;

public class FileController {
   private final List<ClientFile> clientFiles;
   private final File directory;
   private final File moduleConfigDirectory;
   private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

   public FileController(List<ClientFile> clientFiles, File directory, File moduleConfigDirectory) {
      this.clientFiles = clientFiles;
      this.directory = directory;
      this.moduleConfigDirectory = moduleConfigDirectory;
   }

   public void startAutoSave() {
      this.scheduler.scheduleAtFixedRate(() -> {
         try {
            this.saveFiles();
         } catch (FileSaveException var2) {
            LoggerUtil.error("Failed to auto-save files: {}", var2.getMessage());
         }
      }, 1L, 1L, TimeUnit.MINUTES);
   }

   public void stopAutoSave() {
      this.scheduler.shutdown();

      try {
         if (!this.scheduler.awaitTermination(1L, TimeUnit.MINUTES)) {
            this.scheduler.shutdownNow();
         }
      } catch (InterruptedException var2) {
         this.scheduler.shutdownNow();
      }
   }

   public void saveFiles() throws FileSaveException {
      if (!this.clientFiles.isEmpty()) {
         List<String> failedFiles = new ArrayList<>();

         for (ClientFile clientFile : this.clientFiles) {
            try {
               clientFile.saveToFile(this.directory);
            } catch (FileSaveException var5) {
               LoggerUtil.error("Failed to save file: {} — {}", clientFile.getName(), var5.getMessage());
               failedFiles.add(clientFile.getName());
            }
         }

         if (!failedFiles.isEmpty()) {
            throw new FileSaveException("Failed to save files: " + String.join(", ", failedFiles));
         }
      }
   }

   public void loadFiles() throws FileLoadException {
      if (this.clientFiles.isEmpty()) {
         LoggerUtil.warn("No files to load from directory: {}", this.directory.getPath());
      } else {
         List<String> failedFiles = new ArrayList<>();

         for (ClientFile clientFile : this.clientFiles) {
            try {
               clientFile.loadFromFile(this.directory);
            } catch (FileLoadException var5) {
               LoggerUtil.error("Failed to load file: {} — {}", clientFile.getName(), var5.getMessage());
               failedFiles.add(clientFile.getName());
            } catch (Exception var6) {
               LoggerUtil.error("Unexpected error loading file: {} — {}", clientFile.getName(), var6.getMessage());
               failedFiles.add(clientFile.getName());
            }
         }

         if (!failedFiles.isEmpty()) {
            LoggerUtil.warn("Some files failed to load: {}. Mod will continue with defaults for those.", String.join(", ", failedFiles));
         }
      }
   }

   public void saveFile(String fileName) throws FileSaveException {
      for (ClientFile clientFile : this.clientFiles) {
         if (clientFile instanceof ModuleFile) {
            try {
               clientFile.saveToFile(this.moduleConfigDirectory, fileName);
            } catch (FileSaveException var5) {
               throw new FileSaveException("Failed to save file: " + fileName, var5);
            }
         }
      }
   }

   public void loadFile(String fileName) throws FileLoadException {
      for (ClientFile clientFile : this.clientFiles) {
         if (clientFile instanceof ModuleFile) {
            try {
               clientFile.loadFromFile(this.moduleConfigDirectory, fileName);
            } catch (FileLoadException var5) {
               throw new FileLoadException("Failed to load file: " + fileName, var5);
            }
         }
      }
   }

   public List<ClientFile> getClientFiles() {
      return this.clientFiles;
   }
}
