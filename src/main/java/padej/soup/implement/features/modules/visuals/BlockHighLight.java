package padej.soup.implement.features.modules.visuals;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import padej.soup.api.event.EventHandler;
import padej.soup.api.event.events.render.WorldRenderEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.render.Render3DUtil;

public class BlockHighLight extends Module {
   public static BlockHighLight getInstance() {
      return Instance.get(BlockHighLight.class);
   }

   public BlockHighLight() {
      super("module.blockhighlight.name", ModuleCategory.VISUALS);
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (mc.crosshairTarget instanceof BlockHitResult result && result.getType().equals(Type.BLOCK)) {
         BlockPos pos = result.getBlockPos();
         Render3DUtil.drawShapeAlternative(pos, mc.world.getBlockState(pos).getOutlineShape(mc.world, pos), ColorUtil.getClientColor(), 2.0F, true, true);
      }
   }
}
