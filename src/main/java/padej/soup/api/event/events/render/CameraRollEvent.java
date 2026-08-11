package padej.soup.api.event.events.render;

import padej.soup.api.event.events.Event;

public class CameraRollEvent implements Event {
   public static final CameraRollEvent INSTANCE = new CameraRollEvent();
   private float yaw;
   private float pitch;
   private float roll;

   public CameraRollEvent set(float yaw, float pitch, float roll) {
      this.yaw = yaw;
      this.pitch = pitch;
      this.roll = roll;
      return this;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public float getRoll() {
      return this.roll;
   }

   public void setRoll(float roll) {
      this.roll = roll;
   }
}
