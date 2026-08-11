package padej.soup.core.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import padej.soup.core.Main;
import padej.soup.core.listener.impl.EventListener;

public class ListenerRepository {
   private final List<Listener> listeners = new ArrayList<>();

   public void setup() {
      this.registerListeners(new EventListener());
   }

   public void registerListeners(Listener... listeners) {
      this.listeners.addAll(List.of(listeners));
      Arrays.stream(listeners).forEach(listener -> Main.getInstance().getEventManager().register(listener));
   }

   public List<Listener> getListeners() {
      return this.listeners;
   }
}
