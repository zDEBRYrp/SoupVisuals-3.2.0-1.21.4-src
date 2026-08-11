package padej.soup.api.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import padej.soup.base.util.logger.LoggerUtil;

public class DirectoryCreator {
   public void createDirectories(File... files) {
      List<String> createdDirectories = new ArrayList<>();
      Arrays.stream(files).filter(file -> !file.exists()).forEach(file -> {
         if (file.mkdirs()) {
            createdDirectories.add(file.getName());
         }
      });
      LoggerUtil.info("Number of directories created: {}", createdDirectories.size());
      if (!createdDirectories.isEmpty()) {
         LoggerUtil.info("Directories created:");
         createdDirectories.forEach(LoggerUtil::info);
      }
   }
}
