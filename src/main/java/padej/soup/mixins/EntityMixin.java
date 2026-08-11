package padej.soup.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import padej.protect.ProtIgnore;
import padej.soup.api.accessor.IEntity;
import padej.soup.base.QuickImports;
import padej.soup.implement.features.modules.visuals.Trails;

@ProtIgnore
@Mixin({Entity.class})
public class EntityMixin implements QuickImports, IEntity {
   @Unique
   private final List<Trails.Trail> trails = new ArrayList<>();

   @Override
   public List<Trails.Trail> getTrails() {
      return this.trails;
   }

   @ModifyExpressionValue(
      method = {"move"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"
      )}
   )
   private boolean fixFallDistanceCalculation(boolean original) {
      return (Object)this == mc.player ? false : original;
   }
}
