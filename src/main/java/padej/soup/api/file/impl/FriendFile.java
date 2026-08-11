package padej.soup.api.file.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import padej.soup.api.file.ClientFile;
import padej.soup.api.file.SafeFileUtil;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;
import padej.soup.api.repository.friend.Friend;
import padej.soup.api.repository.friend.FriendUtils;
import padej.soup.base.util.logger.LoggerUtil;

public class FriendFile extends ClientFile {
   public FriendFile() {
      super("friends");
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      File file = new File(path, this.getName() + ".json");

      try {
         SafeFileUtil.atomicWrite(file, writer -> gson.toJson(FriendUtils.getFriends(), writer));
      } catch (IOException var5) {
         throw new FileSaveException(String.format("Failed to save %s to file", this.getName()), var5);
      }
   }

   @Override
   public void loadFromFile(File path) throws FileLoadException {
      Gson gson = new Gson();
      File file = new File(path, this.getName() + ".json");
      if (!file.exists()) {
         LoggerUtil.info("Friends file does not exist, starting with empty list");
      } else {
         try {
            try (Reader reader = SafeFileUtil.safeReader(file)) {
               if (reader != null) {
                  Friend[] friends = (Friend[])gson.fromJson(reader, Friend[].class);
                  if (friends == null) {
                     LoggerUtil.warn("Friends file parsed as null, starting with empty list");
                     return;
                  }

                  FriendUtils.clear();
                  FriendUtils.getFriends().addAll(Arrays.asList(friends));
                  return;
               }

               LoggerUtil.warn("Friends file is corrupted and unrecoverable, starting with empty list");
            }

            return;
         } catch (IOException var9) {
            throw new FileLoadException(String.format("Failed to load %s from file", this.getName()), var9);
         } catch (JsonSyntaxException var10) {
            LoggerUtil.error("JSON syntax error in {}, attempting backup restore...", this.getName());
            if (SafeFileUtil.restoreFromBackup(file)) {
               this.loadFromFile(path);
            } else {
               LoggerUtil.warn("No valid backup for {}, starting with empty friends list", this.getName());
            }
         } catch (JsonIOException var11) {
            throw new FileLoadException(String.format("JSON IO error, %s config cannot be loaded", this.getName()), var11);
         }
      }
   }
}
