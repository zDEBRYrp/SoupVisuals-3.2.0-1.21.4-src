package padej.soup.api.file;

import java.io.File;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;

public abstract class ClientFile {
   private final String name;

   public abstract void saveToFile(File var1) throws FileSaveException;

   public abstract void loadFromFile(File var1) throws FileLoadException;

   public void saveToFile(File path, String fileName) throws FileSaveException {
   }

   public void loadFromFile(File path, String fileName) throws FileLoadException {
   }

   public String getName() {
      return this.name;
   }

   public ClientFile(String name) {
      this.name = name;
   }
}
