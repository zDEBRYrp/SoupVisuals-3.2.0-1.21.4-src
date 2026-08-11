package padej.soup.core.client;

import java.io.File;

public interface ClientInfoProvider {
   File clientDir();

   File filesDir();

   File configsDir();
}
