package padej.soup.implement.features.modules.client;

import java.io.File;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.BindSetting;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.implement.files.MenuBindFile;

public class KeyBind extends Module {
   private static final MenuBindFile menuBindFile = new MenuBindFile();
   public final BindSetting openMenuSetting = new BindSetting("setting.keybind.openmenu.name", "setting.keybind.openmenu.desc") {
      @Override
      public int getKey() {
         return KeyBind.menuBindFile.getMenuKey();
      }

      @Override
      public BindSetting setKey(int key) {
         super.setKey(key);
         KeyBind.menuBindFile.setMenuKey(key);

         try {
            File soupApiDir = new File("./SoupAPI/files/");
            KeyBind.menuBindFile.saveToFile(soupApiDir);
         } catch (Exception var3) {
            LoggerUtil.error("Failed save bind: " + var3.getMessage());
         }

         return this;
      }
   };

   public static KeyBind getInstance() {
      return Instance.get(KeyBind.class);
   }

   public KeyBind() {
      super("module.keybind.name", ModuleCategory.CLIENT, false);
      this.openMenuSetting.setSaveToConfig(false);
      this.setup(new Setting[]{this.openMenuSetting});
   }

   public int getMenuKey() {
      return menuBindFile.getMenuKey();
   }

   public static void reloadGlobalSettings() {
      try {
         File soupApiDir = new File("./SoupAPI/files/");
         menuBindFile.loadFromFile(soupApiDir);
         LoggerUtil.info("Reloaded global menuBind setting from file");
      } catch (Exception var1) {
         LoggerUtil.error("Failed to reload menuBind: " + var1.getMessage());
      }
   }

   public BindSetting getOpenMenuSetting() {
      return this.openMenuSetting;
   }

   static {
      try {
         File soupApiDir = new File("./SoupAPI/files/");
         menuBindFile.loadFromFile(soupApiDir);
         LoggerUtil.info("Loaded menuBind from file: " + menuBindFile.getMenuKey());
      } catch (Exception var1) {
         LoggerUtil.error("Failed to load menuBind in static block: " + var1.getMessage());
      }
   }
}
