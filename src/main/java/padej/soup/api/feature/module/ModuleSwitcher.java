package padej.soup.api.feature.module;

import java.util.List;
import net.minecraft.util.Formatting;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.keyboard.KeyEvent;
import padej.soup.api.feature.module.exception.ModuleException;
import padej.soup.api.system.logger.implement.ConsoleLogger;
import padej.soup.base.QuickImports;
import padej.soup.base.QuickLogger;

public class ModuleSwitcher implements QuickLogger, QuickImports {
   private final List<Module> modules;

   public ModuleSwitcher(List<Module> modules, EventManager eventManager) {
      this.modules = modules;
      eventManager.register(this);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      for (Module module : this.modules) {
         if (event.key() == module.getKey() && mc.currentScreen == null) {
            try {
               this.handleModuleState(module, event.action());
            } catch (Exception var5) {
               this.handleException(module.getVisibleName(), var5);
            }
         }
      }
   }

   private void handleModuleState(Module module, int action) {
      if (module.getType() == 1 && action == 1) {
         module.switchState();
      }
   }

   private void handleException(String moduleName, Exception e) {
      ConsoleLogger consoleLogger = new ConsoleLogger();
      if (e instanceof ModuleException) {
         this.logDirect("[" + moduleName + "] " + Formatting.RED + e.getMessage());
      } else {
         consoleLogger.log("Error in module " + moduleName + ": " + e.getMessage());
      }
   }
}
