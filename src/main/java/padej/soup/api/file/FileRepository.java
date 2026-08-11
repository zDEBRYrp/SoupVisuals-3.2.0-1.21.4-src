package padej.soup.api.file;

import java.util.ArrayList;
import java.util.List;
import padej.soup.api.file.impl.CurrentLanguageFile;
import padej.soup.api.file.impl.FriendFile;
import padej.soup.core.Main;

public class FileRepository {
   private final List<ClientFile> clientFiles = new ArrayList<>();

   public void setup(Main main) {
      this.register(new FriendFile(), new CurrentLanguageFile());
   }

   public void register(ClientFile... clientFIle) {
      this.clientFiles.addAll(List.of(clientFIle));
   }

   public List<ClientFile> getClientFiles() {
      return this.clientFiles;
   }
}
