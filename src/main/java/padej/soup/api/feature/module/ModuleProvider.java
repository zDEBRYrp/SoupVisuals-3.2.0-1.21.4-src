package padej.soup.api.feature.module;

import java.util.List;

public class ModuleProvider {
   private final List<Module> modules;
   private static ModuleProvider instance;

   public static ModuleProvider getInstance() {
      return instance;
   }

   public static void setInstance(ModuleProvider provider) {
      instance = provider;
   }

   public <T extends Module> T get(String name) {
      for (Module module : this.modules) {
         if (module.getIdentifier().equalsIgnoreCase(name)) {
            return (T)module;
         }
      }

      return null;
   }

   public <T extends Module> T get(Class<T> clazz) {
      for (Module module : this.modules) {
         if (clazz.isAssignableFrom(module.getClass())) {
            return clazz.cast(module);
         }
      }

      return null;
   }

   public List<Module> getModules() {
      return this.modules;
   }

   public ModuleProvider(List<Module> modules) {
      this.modules = modules;
   }
}
