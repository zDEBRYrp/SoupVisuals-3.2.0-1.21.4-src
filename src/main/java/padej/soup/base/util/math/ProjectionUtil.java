package padej.soup.base.util.math;

import java.util.Objects;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import padej.soup.base.QuickImports;
import padej.soup.base.util.entity.VisibleUtils;
import padej.soup.base.util.render.Render3DUtil;

public final class ProjectionUtil implements QuickImports {
   private static final int[] VIEWPORT = new int[4];
   private static final Matrix4f WORLD_MATRIX = new Matrix4f();
   private static final Matrix4f PROJ_MATRIX = new Matrix4f();
   private static final Vector4f WORLD_POS = new Vector4f();
   private static final Vector4f CLIP_POS = new Vector4f();

   public static Vector4d getVector4D(Entity ent) {
      Vector4d position = null;
      if (ent != null) {
         for (Vec3d vector : getVec3ds(ent, MathUtil.interpolate(ent))) {
            vector = worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
            if (vector.z > 0.0) {
               if (position == null) {
                  position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
               }

               position.x = Math.min(vector.x, position.x);
               position.y = Math.min(vector.y, position.y);
               position.z = Math.max(vector.x, position.z);
               position.w = Math.max(vector.y, position.w);
            }
         }
      }

      return position;
   }

   @NotNull
   public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
      GL11.glGetIntegerv(2978, VIEWPORT);
      WORLD_MATRIX.set(Render3DUtil.lastWorldSpaceMatrix.getPositionMatrix());
      PROJ_MATRIX.set(Render3DUtil.lastProjMat);
      WORLD_POS.set((float)pos.x, (float)pos.y, (float)pos.z, 1.0F);
      WORLD_POS.mul(WORLD_MATRIX);
      PROJ_MATRIX.transform(WORLD_POS, CLIP_POS);
      if (CLIP_POS.w() <= 0.0F) {
         return new Vec3d(0.0, 0.0, -1.0);
      } else {
         float ndcX = CLIP_POS.x() / CLIP_POS.w();
         float ndcY = CLIP_POS.y() / CLIP_POS.w();
         float ndcZ = CLIP_POS.z() / CLIP_POS.w();
         float screenX = (ndcX * 0.5F + 0.5F) * VIEWPORT[2];
         float screenY = (1.0F - (ndcY * 0.5F + 0.5F)) * VIEWPORT[3];
         double depth = ndcZ * 0.5 + 0.5;
         return new Vec3d(screenX / mc.getWindow().getScaleFactor(), screenY / mc.getWindow().getScaleFactor(), Math.max(0.0, Math.min(1.0, depth)));
      }
   }

   @NotNull
   public static Vec3d[] getVec3ds(Entity ent, Vec3d pos) {
      Box axisAlignedBB2 = ent.getBoundingBox();
      Box axisAlignedBB = new Box(
         axisAlignedBB2.minX - ent.getX() + pos.x - 0.1F,
         axisAlignedBB2.minY - ent.getY() + pos.y - 0.1F,
         axisAlignedBB2.minZ - ent.getZ() + pos.z - 0.1F,
         axisAlignedBB2.maxX - ent.getX() + pos.x + 0.1F,
         axisAlignedBB2.maxY - ent.getY() + pos.y + 0.1F,
         axisAlignedBB2.maxZ - ent.getZ() + pos.z + 0.1F
      );
      return new Vec3d[]{
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)
      };
   }

   public static boolean canSee(Box box) {
      Frustum frustum = mc.worldRenderer.frustum;
      return box != null && frustum != null && frustum.isVisible(box);
   }

   public static boolean cantSee(Vector4d vec) {
      return vec == null || vec.x < 0.0 && vec.z < 1.0 || vec.y < 0.0 && vec.w < 1.0;
   }

   public static boolean canSeeEntity(Entity entity) {
      return canSeeEntity(entity, 16.0);
   }

   public static boolean canSeeEntity(Entity entity, double maxDistance) {
      if (entity != null && mc.player != null) {
         double maxDistanceSq = maxDistance * maxDistance;
         double distanceSq = mc.player.getPos().squaredDistanceTo(entity.getPos());
         if (distanceSq > maxDistanceSq) {
            return false;
         } else if (!Objects.requireNonNull(mc.player).canSee(entity)) {
            return false;
         } else {
            return entity instanceof LivingEntity living ? VisibleUtils.canBeTargeted(living) : !entity.isInvisible() || entity.isGlowing();
         }
      } else {
         return false;
      }
   }

   public static double centerX(Vector4d vec) {
      return vec.x + (vec.z - vec.x) / 2.0;
   }

   public static Vector4d getVector4DForAnchor(Entity ent, String anchor) {
      if (ent == null) {
         return null;
      } else {
         Vector4d fullProjection = getVector4D(ent);
         if (fullProjection == null) {
            return null;
         } else {
            Vec3d entityPos = MathUtil.interpolate(ent);
            Vec3d anchorPos = getAnchorPos(ent, anchor, entityPos);
            Vec3d screenPos = worldSpaceToScreenSpace(anchorPos);
            return screenPos.z > 0.0 ? new Vector4d(fullProjection.x, screenPos.y, fullProjection.z, fullProjection.w) : null;
         }
      }
   }

   @NotNull
   private static Vec3d getAnchorPos(Entity ent, String anchor, Vec3d entityPos) {
      Box boundingBox = ent.getBoundingBox();
      double height = boundingBox.maxY - boundingBox.minY;
      double yOffset = 0.0;
      switch (anchor) {
         case "HEAD":
            yOffset = height * 0.85;
            break;
         case "BODY":
            yOffset = height * 0.5;
            break;
         case "FEET":
            yOffset = 0.1;
      }

      return new Vec3d(entityPos.x, entityPos.y + yOffset, entityPos.z);
   }

   private ProjectionUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
