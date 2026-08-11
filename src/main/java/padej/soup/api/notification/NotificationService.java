package padej.soup.api.notification;

import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

public interface NotificationService {
   static NotificationService getInstance() {
      return NotificationService.Registry.get();
   }

   void show(String var1, long var2);

   void show(Text var1, long var2);

   void show(String var1, long var2, SoundEvent var4);

   void show(Text var1, long var2, SoundEvent var4);

   public static final class Registry {
      private static NotificationService instance;

      private Registry() {
      }

      public static void set(NotificationService service) {
         instance = service;
      }

      static NotificationService get() {
         return instance;
      }
   }
}
