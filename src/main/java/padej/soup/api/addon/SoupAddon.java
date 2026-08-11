package padej.soup.api.addon;

import padej.soup.api.SoupAPI;

public interface SoupAddon {
   String getId();

   String getName();

   String getVersion();

   void onInitialize(SoupAPI var1);

   default void onDisable() {
   }
}
