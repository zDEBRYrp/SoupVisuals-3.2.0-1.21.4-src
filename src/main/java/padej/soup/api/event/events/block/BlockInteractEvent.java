package padej.soup.api.event.events.block;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import padej.soup.api.event.events.callables.EventCancellable;

public class BlockInteractEvent extends EventCancellable {
   private final BlockPos blockPos;
   private final Direction side;
   private final Hand hand;
   private final BlockHitResult hitResult;

   public BlockInteractEvent(BlockPos blockPos, Direction side, Hand hand, BlockHitResult hitResult) {
      this.blockPos = blockPos;
      this.side = side;
      this.hand = hand;
      this.hitResult = hitResult;
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public Direction getSide() {
      return this.side;
   }

   public Hand getHand() {
      return this.hand;
   }

   public BlockHitResult getHitResult() {
      return this.hitResult;
   }
}
