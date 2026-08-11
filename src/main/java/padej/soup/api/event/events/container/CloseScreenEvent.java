package padej.soup.api.event.events.container;

import net.minecraft.client.gui.screen.Screen;
import padej.soup.api.event.events.callables.EventCancellable;

public class CloseScreenEvent extends EventCancellable {
   private Screen screen;

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof CloseScreenEvent other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (!super.equals(o)) {
         return false;
      } else {
         Object this$screen = this.getScreen();
         Object other$screen = other.getScreen();
         return this$screen == null ? other$screen == null : this$screen.equals(other$screen);
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof CloseScreenEvent;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = super.hashCode();
      Object $screen = this.getScreen();
      return result * 59 + ($screen == null ? 43 : $screen.hashCode());
   }

   public Screen getScreen() {
      return this.screen;
   }

   public void setScreen(Screen screen) {
      this.screen = screen;
   }

   @Override
   public String toString() {
      return "CloseScreenEvent(screen=" + this.getScreen() + ")";
   }

   public CloseScreenEvent(Screen screen) {
      this.screen = screen;
   }
}
