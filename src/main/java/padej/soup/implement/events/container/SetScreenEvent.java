package padej.soup.implement.events.container;

import net.minecraft.client.gui.screen.Screen;

@Deprecated
public class SetScreenEvent extends padej.soup.api.event.events.container.SetScreenEvent {
   public SetScreenEvent(Screen screen) {
      super(screen);
   }
}
