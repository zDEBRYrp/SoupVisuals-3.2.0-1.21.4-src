package padej.soup.mixins;

import net.minecraft.client.render.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.render.CameraRollEvent;

@ProtIgnore
@Mixin({Camera.class})
public class CameraMixin {
   @Shadow
   private Quaternionf field_21518;
   @Shadow
   private Vector3f field_18714;
   @Shadow
   private Vector3f field_18715;
   @Shadow
   private Vector3f field_18716;

   @Inject(
      method = {"setRotation"},
      at = {@At("TAIL")}
   )
   private void onSetRotation(float yaw, float pitch, CallbackInfo ci) {
      CameraRollEvent event = CameraRollEvent.INSTANCE.set(yaw, pitch, 0.0F);
      EventManager.callEvent(event);
      float roll = event.getRoll();
      if (roll != 0.0F) {
         float yawRad = (180.0F - yaw) * (float) (Math.PI / 180.0);
         float pitchRad = -pitch * (float) (Math.PI / 180.0);
         float rollRad = roll * (float) (Math.PI / 180.0);
         this.field_21518.identity();
         this.field_21518.rotateY(yawRad);
         this.field_21518.rotateX(pitchRad);
         this.field_21518.rotateZ(rollRad);
         this.field_18714.set(0.0F, 0.0F, 1.0F).rotate(this.field_21518);
         this.field_18715.set(0.0F, 1.0F, 0.0F).rotate(this.field_21518);
         this.field_18716.set(1.0F, 0.0F, 0.0F).rotate(this.field_21518);
      }
   }
}
