package padej.soup.api.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import padej.soup.api.event.events.Event;
import padej.soup.api.event.events.EventStoppable;
import padej.soup.api.event.types.Priority;
import padej.soup.api.feature.module.exception.ModuleException;
import padej.soup.api.system.logger.implement.ConsoleLogger;
import padej.soup.api.system.logger.implement.MinecraftLogger;
import padej.soup.base.util.logger.LoggerUtil;

public final class EventManager {
   private static final Map<Class<? extends Event>, List<EventManager.MethodData>> REGISTRY_MAP = new HashMap<>();
   private static EventManager instance;

   public static EventManager getInstance() {
      return instance;
   }

   public static void setInstance(EventManager manager) {
      instance = manager;
   }

   public void register(Object object) {
      for (Method method : object.getClass().getDeclaredMethods()) {
         if (!isMethodBad(method)) {
            this.register(method, object);
         }
      }
   }

   public void unregister(Object object) {
      for (List<EventManager.MethodData> dataList : REGISTRY_MAP.values()) {
         dataList.removeIf(data -> data.source().equals(object));
      }

      cleanMap(true);
   }

   private void register(Method method, Object object) {
      Class<? extends Event> indexClass = (Class<? extends Event>)method.getParameterTypes()[0];
      final EventManager.MethodData data = new EventManager.MethodData(object, method, method.getAnnotation(EventHandler.class).value());
      if (!data.target().isAccessible()) {
         data.target().setAccessible(true);
      }

      if (REGISTRY_MAP.containsKey(indexClass)) {
         if (!REGISTRY_MAP.get(indexClass).contains(data)) {
            REGISTRY_MAP.get(indexClass).add(data);
            sortListValue(indexClass);
         }
      } else {
         REGISTRY_MAP.put(indexClass, new CopyOnWriteArrayList<EventManager.MethodData>() {
            private static final long serialVersionUID = 100L;

            {
               this.add(data);
            }
         });
      }
   }

   public static void cleanMap(boolean onlyEmptyEntries) {
      Iterator<Entry<Class<? extends Event>, List<EventManager.MethodData>>> mapIterator = REGISTRY_MAP.entrySet().iterator();

      while (mapIterator.hasNext()) {
         if (!onlyEmptyEntries || mapIterator.next().getValue().isEmpty()) {
            mapIterator.remove();
         }
      }
   }

   private static void sortListValue(Class<? extends Event> indexClass) {
      List<EventManager.MethodData> sortedList = new CopyOnWriteArrayList<>();

      for (byte priority : Priority.VALUE_ARRAY) {
         for (EventManager.MethodData data : REGISTRY_MAP.get(indexClass)) {
            if (data.priority() == priority) {
               sortedList.add(data);
            }
         }
      }

      REGISTRY_MAP.put(indexClass, sortedList);
   }

   private static boolean isMethodBad(Method method) {
      return method.getParameterTypes().length != 1 || !method.isAnnotationPresent(EventHandler.class);
   }

   public static Event callEvent(Event event) {
      List<EventManager.MethodData> dataList = REGISTRY_MAP.get(event.getClass());
      if (dataList != null) {
         if (event instanceof EventStoppable stoppable) {
            for (EventManager.MethodData data : dataList) {
               invoke(data, event);
               if (stoppable.isStopped()) {
                  break;
               }
            }
         } else {
            for (EventManager.MethodData datax : dataList) {
               try {
                  invoke(datax, event);
               } catch (Exception var6) {
                  LoggerUtil.error("Failed to invoke event handler", var6);
               }
            }
         }
      }

      return event;
   }

   private static void invoke(EventManager.MethodData data, Event argument) {
      try {
         data.target().invoke(data.source(), argument);
      } catch (IllegalAccessException var8) {
         ConsoleLogger consoleLogger = new ConsoleLogger();
         String errorMessage = "Illegal access to method. ";
         errorMessage = errorMessage + "Method: " + data.target().getName() + ", ";
         errorMessage = errorMessage + "Argument: " + argument.toString() + ", ";
         errorMessage = errorMessage + "Log: " + var8.fillInStackTrace();
         consoleLogger.log(errorMessage);
      } catch (IllegalArgumentException var9) {
         ConsoleLogger consoleLoggerx = new ConsoleLogger();
         String errorMessagex = "Illegal arguments passed to method. ";
         errorMessagex = errorMessagex + "Method: " + data.target().getName() + ", ";
         errorMessagex = errorMessagex + "Argument: " + argument.toString() + ", ";
         errorMessagex = errorMessagex + "Log: " + var9.getCause();
         consoleLoggerx.log(errorMessagex);
      } catch (InvocationTargetException var10) {
         Throwable cause = var10.getCause();
         ConsoleLogger consoleLoggerxx = new ConsoleLogger();
         MinecraftLogger minecraftLogger = new MinecraftLogger();
         if (cause instanceof ModuleException moduleException) {
            minecraftLogger.minecraftLog(Text.literal("[" + moduleException.getModuleName() + "] " + Formatting.RED + moduleException.getMessage()));
         } else {
            String errorMessagexx = "Exception occurred within invoked method. ";
            errorMessagexx = errorMessagexx + "Method: " + data.target().getName() + ", ";
            errorMessagexx = errorMessagexx + "Argument: " + argument.toString() + ", ";
            errorMessagexx = errorMessagexx + "Log: " + var10.getCause();
            consoleLoggerxx.log(errorMessagexx);
         }
      }
   }

   private record MethodData(Object source, Method target, byte priority) {
   }
}
