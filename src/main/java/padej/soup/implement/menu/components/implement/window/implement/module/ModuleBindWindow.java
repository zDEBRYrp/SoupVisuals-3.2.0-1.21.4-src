package padej.soup.implement.menu.components.implement.window.implement.module;

import padej.soup.api.feature.module.Module;
import padej.soup.implement.menu.components.implement.window.implement.AbstractBindWindow;

public class ModuleBindWindow extends AbstractBindWindow {
   private final Module module;

   public Module getModule() {
      return this.module;
   }

   @Override
   protected int getKey() {
      return this.module.getKey();
   }

   @Override
   protected void setKey(int key) {
      this.module.setKey(key);
   }

   @Override
   protected int getType() {
      return this.module.getType();
   }

   @Override
   protected void setType(int type) {
      this.module.setType(type);
   }

   public ModuleBindWindow(Module module) {
      this.module = module;
   }
}
