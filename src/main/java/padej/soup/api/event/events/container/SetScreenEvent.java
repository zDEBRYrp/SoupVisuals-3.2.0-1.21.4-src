package padej.soup.api.event.events.container;

import net.minecraft.client.gui.screen.Screen;
import padej.soup.api.event.events.Event;

public class SetScreenEvent implements Event {
   public Screen screen;

   public Screen getScreen() {
      return this.screen;
   }

   public void setScreen(Screen screen) {
      this.screen = screen;
   }

   public SetScreenEvent(Screen screen) {
      this.screen = screen;
   }
}
