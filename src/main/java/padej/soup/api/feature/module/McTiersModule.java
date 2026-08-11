package padej.soup.api.feature.module;

import padej.soup.base.util.logger.LoggerUtil;

public class McTiersModule extends Module {
   public McTiersModule(String name, ModuleCategory category) {
      super(name, category);
      LoggerUtil.protectedInfo("McTiersModule created: " + name);
   }

   @Override
   public void setState(boolean state) {
      boolean previousState = this.state;
      super.setState(state);
      if (state != previousState) {
         LoggerUtil.protectedInfo("McTiersModule " + this.getName() + " state changed from " + previousState + " to " + state);
      }
   }

   @Override
   public void activate() {
      LoggerUtil.protectedInfo("McTiersModule " + this.getName() + " activated");
      this.logActivationWithSettings();
      super.activate();
   }

   protected void logActivationWithSettings() {
   }

   @Override
   public void deactivate() {
      LoggerUtil.protectedInfo("McTiersModule " + this.getName() + " deactivated");
      super.deactivate();
   }
}
