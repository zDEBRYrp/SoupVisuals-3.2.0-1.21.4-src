package padej.soup.api.event.events.keyboard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil.Type;
import padej.soup.api.event.events.Event;

public record KeyEvent(Screen screen, Type type, int key, int action) implements Event {
   public boolean isKeyDown(int key) {
      return this.isKeyDown(key, MinecraftClient.getInstance().currentScreen == null);
   }

   public boolean isKeyDown(int key, boolean noScreen) {
      return this.key == key && this.action == 1 && noScreen;
   }

   public boolean isKeyReleased(int key) {
      return this.isKeyReleased(key, MinecraftClient.getInstance().currentScreen == null);
   }

   public boolean isKeyReleased(int key, boolean noScreen) {
      return this.key == key && this.action == 0 && noScreen;
   }
}
