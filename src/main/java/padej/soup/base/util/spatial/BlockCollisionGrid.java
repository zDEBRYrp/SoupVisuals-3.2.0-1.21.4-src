package padej.soup.base.util.spatial;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockCollisionGrid {
   private final SpatialGrid3D<BlockPos> grid;
   private final Set<BlockPos> trackedBlocks;
   private final float cellSize = 1.0F;
   private final float collisionRadius;
   private final float repulsionStrength;
   private double lastUpdateX = Double.NaN;
   private double lastUpdateY = Double.NaN;
   private double lastUpdateZ = Double.NaN;
   private int ticksSinceUpdate = 0;
   private static final int UPDATE_INTERVAL = 5;
   private static final double MOVEMENT_THRESHOLD = 3.0;

   public BlockCollisionGrid(float collisionRadius, float repulsionStrength) {
      this.grid = new SpatialGrid3D<>(1.0F);
      this.trackedBlocks = new HashSet<>();
      this.collisionRadius = collisionRadius;
      this.repulsionStrength = repulsionStrength;
   }

   public void updateBlocks(World world, double centerX, double centerY, double centerZ, int radius) {
      if (world != null) {
         this.ticksSinceUpdate++;
         if (this.ticksSinceUpdate < 5) {
            if (Double.isNaN(this.lastUpdateX)) {
               return;
            }

            double dx = centerX - this.lastUpdateX;
            double dy = centerY - this.lastUpdateY;
            double dz = centerZ - this.lastUpdateZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < 9.0) {
               return;
            }
         }

         this.ticksSinceUpdate = 0;
         this.lastUpdateX = centerX;
         this.lastUpdateY = centerY;
         this.lastUpdateZ = centerZ;
         int optimizedRadius = Math.min(radius, 10);
         Set<BlockPos> newBlocks = new HashSet<>();
         int minX = (int)Math.floor(centerX - optimizedRadius);
         int maxX = (int)Math.floor(centerX + optimizedRadius);
         int minY = (int)Math.floor(centerY - optimizedRadius);
         int maxY = (int)Math.floor(centerY + optimizedRadius);
         int minZ = (int)Math.floor(centerZ - optimizedRadius);
         int maxZ = (int)Math.floor(centerZ + optimizedRadius);
         minY = Math.max(-64, minY);
         maxY = Math.min(319, maxY);
         double radiusSq = optimizedRadius * optimizedRadius;

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  double dx = x + 0.5 - centerX;
                  double dy = y + 0.5 - centerY;
                  double dz = z + 0.5 - centerZ;
                  if (!(dx * dx + dy * dy + dz * dz > radiusSq)) {
                     BlockPos pos = new BlockPos(x, y, z);
                     BlockState state = world.getBlockState(pos);
                     if (!state.isAir() && state.isSolidBlock(world, pos)) {
                        newBlocks.add(pos);
                     }
                  }
               }
            }
         }

         for (BlockPos pos : this.trackedBlocks) {
            if (!newBlocks.contains(pos)) {
               this.grid.remove(pos, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
            }
         }

         for (BlockPos posx : newBlocks) {
            if (!this.trackedBlocks.contains(posx)) {
               this.grid.insert(posx, posx.getX() + 0.5F, posx.getY() + 0.5F, posx.getZ() + 0.5F);
            }
         }

         this.trackedBlocks.clear();
         this.trackedBlocks.addAll(newBlocks);
      }
   }

   public float[] calculateRepulsion(float particleX, float particleY, float particleZ) {
      float[] force = new float[]{0.0F, 0.0F, 0.0F};
      List<SpatialGrid3D.GridEntry<BlockPos>> nearbyBlocks = this.grid.queryRadiusWithPositions(particleX, particleY, particleZ, this.collisionRadius);
      if (nearbyBlocks.isEmpty()) {
         return force;
      } else {
         for (SpatialGrid3D.GridEntry<BlockPos> entry : nearbyBlocks) {
            float blockX = entry.getX();
            float blockY = entry.getY();
            float blockZ = entry.getZ();
            float dx = particleX - blockX;
            float dy = particleY - blockY;
            float dz = particleZ - blockZ;
            float distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < 0.01F) {
               distSq = 0.01F;
            }

            float dist = (float)Math.sqrt(distSq);
            float strength = this.repulsionStrength * (1.0F - dist / this.collisionRadius);
            if (strength > 0.0F) {
               float invDist = 1.0F / dist;
               force[0] += dx * invDist * strength;
               force[1] += dy * invDist * strength;
               force[2] += dz * invDist * strength;
            }
         }

         return force;
      }
   }

   public boolean hasCollision(float x, float y, float z) {
      List<BlockPos> nearby = this.grid.queryRadius(x, y, z, this.collisionRadius);
      return !nearby.isEmpty();
   }

   public void clear() {
      this.grid.clear();
      this.trackedBlocks.clear();
   }

   public int getBlockCount() {
      return this.trackedBlocks.size();
   }
}
