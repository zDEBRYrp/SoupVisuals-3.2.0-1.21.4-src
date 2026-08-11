package padej.soup.mixins;

import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.repository.friend.FriendUtils;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.math.ProjectionUtil;
import padej.soup.base.util.render.Render3DUtil;
import padej.soup.core.Main;
import padej.soup.implement.features.modules.visuals.Hitboxes;

@ProtIgnore
@Mixin({EntityRenderDispatcher.class})
public class EntityRenderDispatcherMixin implements QuickImports {
   @Inject(
      method = {"renderHitbox"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void renderHitboxHook(
      MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, float red, float green, float blue, CallbackInfo ci
   ) {
      Hitboxes hitboxes = Hitboxes.getInstance();
      if (hitboxes != null && hitboxes.isEnabled()) {
         if (!Main.getInstance().getBoxRepository().getEntities().containsKey(entity.getType())) {
            renderBox(entity);
         }

         ci.cancel();
      }
   }

   @Unique
   private static void renderBox(Entity entity) {
      if (entity != mc.player || !mc.options.getPerspective().equals(Perspective.FIRST_PERSON)) {
         Hitboxes hitboxes = Hitboxes.getInstance();
         if (hitboxes == null) {
            return;
         }

         boolean isFriend = FriendUtils.isFriend(entity);
         int color;
         if (hitboxes.getColorMode().isSelected("Custom")) {
            color = isFriend ? hitboxes.getFriendHitboxColor().getColor() : hitboxes.getHitboxColor().getColor();
         } else {
            color = isFriend ? ColorUtil.getFriendColor() : ColorUtil.getClientColor();
         }

         Vec3d offset = MathUtil.interpolate(entity).subtract(entity.getPos());
         Box box = entity.getBoundingBox().offset(offset);
         if (ProjectionUtil.canSee(box)) {
            String mode = hitboxes.getRenderMode().getSelected();
            boolean drawOutline = mode.equals("Outline") || mode.equals("Both");
            boolean drawFill = mode.equals("Fill") || mode.equals("Both");
            if (entity instanceof LivingEntity living) {
               float width = entity.getWidth();
               Vec3d eyeMin = entity.getEyePos().add(offset).add(-width / 2.0F, 0.0, -width / 2.0F);
               float damageIntensity = Math.min(living.hurtTime / 10.0F, 1.0F);
               Render3DUtil.drawBox(box, ColorUtil.gradientToRed(color, damageIntensity), 2.0F, drawOutline, drawFill, true);
               if (!hitboxes.getEyesLine().isValue()) {
                  return;
               }

               Render3DUtil.drawLine(eyeMin, eyeMin.add(width, 0.0, 0.0), ColorUtil.RED, 2.0F, true);
               Render3DUtil.drawLine(eyeMin.add(width, 0.0, 0.0), eyeMin.add(width, 0.0, width), ColorUtil.RED, 2.0F, true);
               Render3DUtil.drawLine(eyeMin, eyeMin.add(0.0, 0.0, width), ColorUtil.RED, 2.0F, true);
               Render3DUtil.drawLine(eyeMin.add(0.0, 0.0, width), eyeMin.add(width, 0.0, width), ColorUtil.RED, 2.0F, true);
            } else {
               Render3DUtil.drawBox(box, color, 2.0F, drawOutline, drawFill, true);
            }
         }
      }
   }
}
