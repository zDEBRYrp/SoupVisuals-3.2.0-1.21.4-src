package padej.soup.base.util.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.other.RoleCache;

public class BadgeRenderer {
   private static final Identifier BADGE_ICON_DEFAULT = Identifier.of("textures/soup.png");
   private static final Identifier BADGE_ICON_PASTER = Identifier.of("textures/role/rat.png");

   private static Identifier getBadgeIconForRole(String role) {
      return role.equals("PASTER") ? BADGE_ICON_PASTER : BADGE_ICON_DEFAULT;
   }

   public static void render2D(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float x, float y, String username) {
      float iconSize = 10.0F;
      float offsetY = -1.0F;
      String role = RoleCache.getUserRole(username);
      int[] roleColors = ColorUtil.getBadgeColors(role);
      int color1 = roleColors[0];
      int color2 = roleColors[1];
      float time = (float)System.currentTimeMillis() / 1000.0F;
      float phase1 = (float)((Math.sin(time * 1.5) + 1.0) / 2.0);
      float phase2 = (float)((Math.sin(time * 1.5 + (Math.PI / 2)) + 1.0) / 2.0);
      float phase3 = (float)((Math.sin(time * 1.5 + Math.PI) + 1.0) / 2.0);
      float phase4 = (float)((Math.sin(time * 1.5 + (Math.PI * 3.0 / 2.0)) + 1.0) / 2.0);
      int topLeftColor = ColorUtil.multAlpha(ColorUtil.overCol(color1, color2, phase1), 0.9F);
      int topRightColor = ColorUtil.multAlpha(ColorUtil.overCol(color1, color2, phase2), 0.9F);
      int bottomRightColor = ColorUtil.multAlpha(ColorUtil.overCol(color1, color2, phase3), 0.9F);
      int bottomLeftColor = ColorUtil.multAlpha(ColorUtil.overCol(color1, color2, phase4), 0.9F);
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      Identifier badgeTexture = getBadgeIconForRole(role);
      RenderLayer renderLayer = RenderLayer.getGuiTextured(badgeTexture);
      VertexConsumer buffer = vertexConsumers.getBuffer(renderLayer);
      buffer.vertex(matrix4f, x, y + offsetY + iconSize, 0.0F).texture(0.0F, 1.0F).color(bottomLeftColor);
      buffer.vertex(matrix4f, x + iconSize, y + offsetY + iconSize, 0.0F).texture(1.0F, 1.0F).color(bottomRightColor);
      buffer.vertex(matrix4f, x + iconSize, y + offsetY, 0.0F).texture(1.0F, 0.0F).color(topRightColor);
      buffer.vertex(matrix4f, x, y + offsetY, 0.0F).texture(0.0F, 0.0F).color(topLeftColor);
      matrices.pop();
   }

   public static void render3D(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float x, float y, boolean seeThrough, int light, String username) {
      float iconSize = 10.0F;
      String role = RoleCache.getUserRole(username);
      int[] roleColors = ColorUtil.getBadgeColors(role);
      int color1 = roleColors[0];
      int color2 = roleColors[1];
      float time = (float)System.currentTimeMillis() / 1000.0F;
      float phase1 = (float)((Math.sin(time * 1.5) + 1.0) / 2.0);
      float phase2 = (float)((Math.sin(time * 1.5 + (Math.PI / 2)) + 1.0) / 2.0);
      float phase3 = (float)((Math.sin(time * 1.5 + Math.PI) + 1.0) / 2.0);
      float phase4 = (float)((Math.sin(time * 1.5 + (Math.PI * 3.0 / 2.0)) + 1.0) / 2.0);
      int topLeftColor = ColorUtil.multAlpha(ColorUtil.overCol(color1, color2, phase1), 0.9F);
      int topRightColor = ColorUtil.multAlpha(ColorUtil.overCol(color1, color2, phase2), 0.9F);
      int bottomRightColor = ColorUtil.multAlpha(ColorUtil.overCol(color1, color2, phase3), 0.9F);
      int bottomLeftColor = ColorUtil.multAlpha(ColorUtil.overCol(color1, color2, phase4), 0.9F);
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      Identifier badgeTexture = getBadgeIconForRole(role);
      RenderLayer renderLayer = seeThrough ? RenderLayer.getTextSeeThrough(badgeTexture) : RenderLayer.getText(badgeTexture);
      VertexConsumer buffer = vertexConsumers.getBuffer(renderLayer);
      buffer.vertex(matrix4f, x, y + iconSize, 0.0F).texture(0.0F, 1.0F).color(bottomLeftColor).light(light);
      buffer.vertex(matrix4f, x + iconSize, y + iconSize, 0.0F).texture(1.0F, 1.0F).color(bottomRightColor).light(light);
      buffer.vertex(matrix4f, x + iconSize, y, 0.0F).texture(1.0F, 0.0F).color(topRightColor).light(light);
      buffer.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(topLeftColor).light(light);
      matrices.pop();
   }
}
