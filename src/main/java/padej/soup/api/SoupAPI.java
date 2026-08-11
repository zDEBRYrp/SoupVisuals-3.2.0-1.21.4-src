package padej.soup.api;

import padej.soup.api.addon.AddonConfig;
import padej.soup.api.event.EventManager;
import padej.soup.api.feature.draggable.DraggableRepository;
import padej.soup.api.feature.module.ModuleProvider;
import padej.soup.api.feature.module.ModuleRepository;
import padej.soup.api.notification.NotificationService;
import padej.soup.api.system.localization.LocalizationManager;

public interface SoupAPI {
   EventManager getEventManager();

   ModuleRepository getModuleRepository();

   DraggableRepository getDraggableRepository();

   ModuleProvider getModuleProvider();

   default NotificationService getNotifications() {
      return NotificationService.getInstance();
   }

   default AddonConfig getAddonConfig(String addonId) {
      return new AddonConfig(addonId);
   }

   default LocalizationManager getLocalizationManager() {
      return LocalizationManager.getInstance();
   }
}
