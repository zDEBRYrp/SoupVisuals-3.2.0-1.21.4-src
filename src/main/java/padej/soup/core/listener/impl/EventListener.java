package padej.soup.core.listener.impl;

import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.packet.PacketEvent;
import padej.soup.api.event.events.player.TickEvent;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.base.util.world.ServerUtil;
import padej.soup.core.Main;
import padej.soup.core.listener.Listener;

public class EventListener implements Listener {
   @EventHandler
   public void onTick(TickEvent e) {
      for (AbstractDraggable draggable : Main.getInstance().getDraggableRepository().draggable()) {
         draggable.tick();
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      ServerUtil.packet(e);

      for (AbstractDraggable draggable : Main.getInstance().getDraggableRepository().draggable()) {
         draggable.packet(e);
      }
   }
}
