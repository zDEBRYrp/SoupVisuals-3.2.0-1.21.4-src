package padej.soup.api.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import padej.soup.base.util.logger.LoggerUtil;

public final class SafeFileUtil {
   private SafeFileUtil() {
   }

   public static void atomicWrite(File file, SafeFileUtil.FileWriteAction writeAction) throws IOException {
      File parentDir = file.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
         parentDir.mkdirs();
      }

      File tempFile = new File(file.getParent(), file.getName() + ".tmp");

      try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
         writeAction.write(writer);
         writer.flush();
      } catch (IOException var11) {
         tempFile.delete();
         throw var11;
      }

      if (tempFile.exists() && tempFile.length() != 0L) {
         if (file.exists() && file.length() > 0L) {
            File backupFile = new File(file.getParent(), file.getName() + ".bak");

            try {
               Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException var9) {
               LoggerUtil.warn("Failed to create backup for {}: {}", file.getName(), var9.getMessage());
            }
         }

         if (file.exists()) {
            file.delete();
         }

         if (!tempFile.renameTo(file)) {
            try {
               Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException var8) {
               throw new IOException("Failed to rename temp file to " + file.getName(), var8);
            }
         }
      } else {
         tempFile.delete();
         throw new IOException("Atomic write failed: temp file is empty or missing for " + file.getName());
      }
   }

   public static boolean isFileValid(File file) {
      if (file.exists() && file.length() != 0L) {
         try {
            boolean var10;
            try (InputStream is = new FileInputStream(file)) {
               byte[] header = new byte[(int)Math.min(file.length(), 32L)];
               int read = is.read(header);
               if (read <= 0) {
                  return false;
               }

               boolean allNull = true;

               for (int i = 0; i < read; i++) {
                  if (header[i] != 0) {
                     allNull = false;
                     break;
                  }
               }

               var10 = !allNull;
            }

            return var10;
         } catch (IOException var8) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean restoreFromBackup(File file) {
      File backupFile = new File(file.getParent(), file.getName() + ".bak");
      if (!backupFile.exists() || backupFile.length() == 0L) {
         return false;
      } else if (!isFileValid(backupFile)) {
         LoggerUtil.warn("Backup file {} is also corrupted, cannot restore", backupFile.getName());
         return false;
      } else {
         try {
            Files.copy(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LoggerUtil.info("Restored {} from backup", file.getName());
            return true;
         } catch (IOException var3) {
            LoggerUtil.error("Failed to restore {} from backup: {}", file.getName(), var3.getMessage());
            return false;
         }
      }
   }

   public static BufferedReader safeReader(File file) throws IOException {
      if (!file.exists()) {
         return null;
      } else if (!isFileValid(file)) {
         LoggerUtil.warn("File {} is corrupted (empty or null bytes), attempting backup restore...", file.getName());
         return restoreFromBackup(file) ? new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) : null;
      } else {
         return new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
      }
   }

   @FunctionalInterface
   public interface FileWriteAction {
      void write(Writer var1) throws IOException;
   }
}
