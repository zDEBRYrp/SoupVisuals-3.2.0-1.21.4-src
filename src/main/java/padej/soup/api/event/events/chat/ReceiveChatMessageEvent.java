package padej.soup.api.event.events.chat;

import net.minecraft.text.Text;
import padej.soup.api.event.events.callables.EventCancellable;

public class ReceiveChatMessageEvent extends EventCancellable {
   private final Text message;

   public ReceiveChatMessageEvent(Text message) {
      this.message = message;
   }

   public Text getMessage() {
      return this.message;
   }
}
