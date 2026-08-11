package padej.soup.implement.menu.components.implement.other;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.discord.DiscordManager;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.RoleCache;
import padej.soup.base.util.render.Render2DUtil;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.core.Main;
import padej.soup.implement.menu.MenuScreen;
import padej.soup.implement.menu.components.AbstractComponent;

public class UserComponent extends AbstractComponent {
   private final Animation hoverAnimation = new DecelerateAnimation().setMs(200).setValue(0.15F);
   private final Animation alphaAnimation = new DecelerateAnimation().setMs(300).setValue(1.0);

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      DiscordManager discord = Main.getInstance().getDiscordManager();
      boolean isUserCategory = MenuScreen.INSTANCE.getCategory() == ModuleCategory.PERSONAL_INFO;
      this.alphaAnimation.setDirection(isUserCategory ? Direction.FORWARDS : Direction.BACKWARDS);
      boolean isHovered = MathUtil.isHovered(mouseX, mouseY, this.x + 5.0F, this.y - 30.0F, 54.0, 16.0);
      this.hoverAnimation.setDirection(isHovered ? Direction.FORWARDS : Direction.BACKWARDS);
      float anim = this.alphaAnimation.getOutput().floatValue();
      float hoverScale = 1.0F + this.hoverAnimation.getOutput().floatValue();
      float centerX = this.x + 5.0F + 27.0F;
      float centerY = this.y - 30.0F + 8.0F;
      MathUtil.scale(
         matrix,
         centerX,
         centerY,
         hoverScale,
         () -> {
            rectangle.render(
               ShapeProperties.create(matrix, this.x + 5.0F, this.y - 30.0F, 54.0, 16.0)
                  .round(4.0F)
                  .thickness(2.0F)
                  .softness(0.5F)
                  .outlineColor(ColorUtil.getOutline())
                  .color(ColorUtil.getGuiRectColor(0.5F))
                  .build()
            );
            if (anim != 0.0F) {
               rectangle.render(
                  ShapeProperties.create(matrix, this.x + 5.0F, this.y - 30.0F, 54.0, 16.0)
                     .round(4.0F)
                     .thickness(2.0F)
                     .outlineColor(ColorUtil.getClientColor(anim))
                     .color(ColorUtil.getGuiRectColor2(anim / 2.0F))
                     .build()
               );
            }

            Identifier skinTexture = mc.player != null ? mc.player.getSkinTextures().texture() : null;
            if (skinTexture != null) {
               Render2DUtil.drawDSAvatar(context, skinTexture, this.x + 8.0F, this.y - 27.0F, 10.0F, 5.0F, ColorUtil.getGuiRectColor(1.0F), -1);
            } else {
               rectangle.render(
                  ShapeProperties.create(matrix, this.x + 8.0F, this.y - 27.0F, 10.0, 10.0).round(5.0F).color(ColorUtil.getGuiRectColor(0.3F)).build()
               );
            }

            rectangle.render(
               ShapeProperties.create(matrix, this.x + 15.0F, this.y - 20.5F, 3.5, 3.5).round(1.75F).color(ColorUtil.getGuiRectColor(1.0F)).build()
            );
            rectangle.render(ShapeProperties.create(matrix, this.x + 15.75F, this.y - 19.75F, 2.0, 2.0).round(1.0F).color(-14236020).build());
            ScissorManager scissor = Main.getInstance().getScissorManager();
            scissor.push(renderMatrix, this.x + 5.5F, this.y - 29.5F, 53.0F, 15.0F);
            String username = mc.player != null ? mc.player.getName().getString() : mc.getSession().getUsername();
            int textColor = isUserCategory ? ColorUtil.getClientColor() : ColorUtil.getDescription();
            Fonts.getSize(8, Fonts.Type.INTER_BOLD).drawString(matrix, username, this.x + 20.0F, this.y - 24.0F, textColor);
            int baseColor = ColorUtil.getClientColor();
            Fonts.getSize(8)
               .drawWaveGradientString(
                  matrix, RoleCache.getUserRole(username), this.x + 20.0F, this.y - 19.0F, ColorUtil.darker(baseColor), ColorUtil.lighter(baseColor)
               );
            scissor.pop();
         }
      );
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.x + 5.0F, this.y - 30.0F, 54.0, 16.0) && button == 0) {
         if (MenuScreen.INSTANCE.getCategory() != ModuleCategory.PERSONAL_INFO) {
            SoundManager.playSound(SoundManager.CHANGE_CATEGORY, 2.0F, 1.3F);
         }

         MenuScreen.INSTANCE.setCategory(ModuleCategory.PERSONAL_INFO);
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }
}
