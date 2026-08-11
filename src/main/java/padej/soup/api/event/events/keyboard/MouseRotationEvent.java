package padej.soup.api.event.events.keyboard;

import padej.soup.api.event.events.callables.EventCancellable;

public class MouseRotationEvent extends EventCancellable {
   float cursorDeltaX;
   float cursorDeltaY;

   public float getCursorDeltaX() {
      return this.cursorDeltaX;
   }

   public float getCursorDeltaY() {
      return this.cursorDeltaY;
   }

   public void setCursorDeltaX(float cursorDeltaX) {
      this.cursorDeltaX = cursorDeltaX;
   }

   public void setCursorDeltaY(float cursorDeltaY) {
      this.cursorDeltaY = cursorDeltaY;
   }

   public MouseRotationEvent(float cursorDeltaX, float cursorDeltaY) {
      this.cursorDeltaX = cursorDeltaX;
      this.cursorDeltaY = cursorDeltaY;
   }
}
