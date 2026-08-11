package padej.soup.implement.events.render;

@Deprecated
public class CameraRollEvent extends padej.soup.api.event.events.render.CameraRollEvent {
   public CameraRollEvent(float yaw, float pitch, float roll) {
      this.set(yaw, pitch, roll);
   }
}
