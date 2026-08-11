package padej.soup.base.util.other;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.feature.module.Module;
import padej.soup.core.Main;

public final class Instance {
   private static final ConcurrentMap<Class<? extends Module>, Module> instanceModules = new ConcurrentHashMap<>();
   private static final ConcurrentMap<Class<? extends AbstractDraggable>, AbstractDraggable> instanceDraggables = new ConcurrentHashMap<>();

   public static <T extends Module> T get(Class<T> clazz) {
      return clazz.cast(instanceModules.computeIfAbsent(clazz, instance -> Main.getInstance().getModuleProvider().get((Class<? extends Module>)instance)));
   }

   public static <T extends Module> T get(String module) {
      return Main.getInstance().getModuleProvider().get(module);
   }

   public static <T extends AbstractDraggable> T getDraggable(Class<T> clazz) {
      return clazz.cast(
         instanceDraggables.computeIfAbsent(clazz, instance -> Main.getInstance().getDraggableRepository().get((Class<? extends AbstractDraggable>)instance))
      );
   }

   private Instance() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
