package padej.soup.implement.features.draggables;

import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.pipeline.HudRenderPipeline;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.render.Render2DUtil;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.core.Main;

public class Armor extends AbstractDraggable {
   public Armor() {
      super("Armor", 0, 0, 82, 22, false);
   }

   @Override
   public boolean visible() {
      if (mc.player == null) {
         return false;
      } else {
         for (ItemStack stack : mc.player.getInventory().armor) {
            if (!stack.isEmpty()) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      int handX = Objects.requireNonNull(mc.player).getMainArm().equals(Arm.RIGHT) ? 100 : -187;
      HudRenderPipeline pipeline = HudRenderPipeline.getInstance();
      if (padej.soup.implement.features.modules.hud.HotBar.getInstance().isEnabled()) {
         this.setX(context.getScaledWindowWidth() / 2 + handX);
         this.setY(context.getScaledWindowHeight() - 27);
         blur.render(
            ShapeProperties.create(matrix, this.getX() - 0.5F, this.getY() - 0.5F, this.getWidth() + 1, 23.0)
               .round(3.0F)
               .thickness(2.0F)
               .softness(1.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getBlurRect(0.7F))
               .build()
         );
         int ox = 3;
         int oy = 3;
         pipeline.recordVanilla(() -> this.drawArmorStacks(context, 3, 3), HudRenderPipeline.VanillaLayer.AFTER_BLUR);
      } else {
         Sprite sprite = context.guiAtlasManager.getSprite(InGameHud.HOTBAR_TEXTURE);
         this.setX(context.getScaledWindowWidth() / 2 + handX);
         this.setY(context.getScaledWindowHeight() - 19);
         pipeline.recordVanilla(() -> {
            ScissorManager scissor = Main.getInstance().getScissorManager();
            int width = 41;
            scissor.push(matrix.peek().getPositionMatrix(), this.getX() - 3, this.getY() - 3, width, 22.0F);
            Render2DUtil.drawSprite(matrix, sprite, this.getX() - 3, this.getY() - 3, 182.0F, 22);
            scissor.pop();
            scissor.push(matrix.peek().getPositionMatrix(), this.getX() + width - 3, this.getY() - 3, width, 22.0F);
            Render2DUtil.drawSprite(matrix, sprite, this.getX() - 103, this.getY() - 3, 182.0F, 22);
            scissor.pop();
            this.drawArmorStacks(context, 0, 0);
         }, HudRenderPipeline.VanillaLayer.AFTER_BLUR);
      }
   }

   public void drawArmorStacks(DrawContext context, int offsetX, int offsetY) {
      for (ItemStack itemStack : Objects.requireNonNull(mc.player).getArmorItems()) {
         context.drawItem(itemStack, this.getX() + offsetX, this.getY() + offsetY, 1);
         context.drawStackOverlay(mc.textRenderer, itemStack, this.getX() + offsetX, this.getY() + offsetY);
         offsetX += 20;
      }
   }
}
