package padej.soup.mixins.accessor;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import padej.protect.ProtIgnore;

@ProtIgnore
@Mixin({EndCrystalEntityModel.class})
public interface EndCrystalEntityModelAccessor {
   @Accessor("base")
   ModelPart getBase();

   @Accessor("outerGlass")
   ModelPart getOuterGlass();

   @Accessor("innerGlass")
   ModelPart getInnerGlass();

   @Accessor("cube")
   ModelPart getCube();
}
