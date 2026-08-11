package padej.soup.api.event.events.chat;

import padej.soup.api.event.events.callables.EventCancellable;

public class SendChatMessageEvent extends EventCancellable {
   private String message;

   public SendChatMessageEvent(String message) {
      this.message = message;
   }

   public String getMessage() {
      return this.message;
   }

   public void setMessage(String message) {
      this.message = message;
   }
}
