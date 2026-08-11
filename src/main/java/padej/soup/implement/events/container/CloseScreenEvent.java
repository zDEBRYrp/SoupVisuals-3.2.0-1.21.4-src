package padej.soup.implement.events.container;

import net.minecraft.client.gui.screen.Screen;

@Deprecated
public class CloseScreenEvent extends padej.soup.api.event.events.container.CloseScreenEvent {
   public CloseScreenEvent(Screen screen) {
      super(screen);
   }
}
