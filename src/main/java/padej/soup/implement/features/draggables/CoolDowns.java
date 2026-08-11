package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import padej.soup.api.event.events.packet.PacketEvent;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.pipeline.HudRenderPipeline;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.other.StopWatch;
import padej.soup.base.util.other.StringUtil;
import padej.soup.base.util.render.Render2DUtil;
import padej.soup.implement.features.modules.hud.Icons;

public class CoolDowns extends AbstractDraggable {
   public final List<CoolDowns.CoolDown> list = new ArrayList<>();

   public static CoolDowns getInstance() {
      return Instance.getDraggable(CoolDowns.class);
   }

   public CoolDowns() {
      super("CoolDowns", 120, 10, 80, 23, true);
   }

   @Override
   public boolean visible() {
      return !this.list.isEmpty() || PlayerIntersectionUtil.isChatOrMenu(mc.currentScreen);
   }

   @Override
   public void tick() {
      this.list.removeIf(c -> c.anim.isFinished(Direction.BACKWARDS));
      if (mc.player != null && !this.list.isEmpty()) {
         for (CoolDowns.CoolDown coolDown : this.list) {
            if (!mc.player.getItemCooldownManager().isCoolingDown(coolDown.stack)) {
               coolDown.anim.setDirection(Direction.BACKWARDS);
            }
         }
      }
   }

   @Override
   public void packet(PacketEvent e) {
      if (!PlayerIntersectionUtil.nullCheck()) {
         switch (e.getPacket()) {
            case CooldownUpdateS2CPacket c:
               Item item = (Item)Registries.ITEM.get(c.cooldownGroup());

               for (CoolDowns.CoolDown coolDown : this.list) {
                  if (coolDown.stack.getItem().equals(item)) {
                     coolDown.anim.setDirection(Direction.BACKWARDS);
                  }
               }

               if (c.cooldown() != 0) {
                  ItemStack stack = this.findItemStack(item);
                  this.list.add(new CoolDowns.CoolDown(stack, new StopWatch().setMs(-c.cooldown() * 50L), new DecelerateAnimation().setMs(150).setValue(1.0)));
               }
               break;
            case PlayerRespawnS2CPacket p:
               this.list.clear();
               break;
            default:
         }
      }
   }

   private ItemStack findItemStack(Item item) {
      if (mc.player == null) {
         return item.getDefaultStack();
      } else if (mc.player.getMainHandStack().getItem().equals(item)) {
         return mc.player.getMainHandStack();
      } else if (mc.player.getOffHandStack().getItem().equals(item)) {
         return mc.player.getOffHandStack();
      } else {
         for (ItemStack stack : mc.player.getInventory().main) {
            if (stack.getItem().equals(item)) {
               return stack;
            }
         }

         return item.getDefaultStack();
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(15, Fonts.Type.INTER_DEFAULT);
      FontRenderer fontCoolDown = Fonts.getSize(13, Fonts.Type.INTER_DEFAULT);
      padej.soup.implement.features.modules.hud.CoolDowns coolDownsModule = padej.soup.implement.features.modules.hud.CoolDowns.getInstance();
      float headerHeight = 16.0F;
      float padding = 5.0F;
      boolean showHeader = coolDownsModule.getShowHeader().isValue();
      boolean darkenHeader = coolDownsModule.getDarkenHeader().isValue();
      blur.render(
         ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), this.getHeight())
            .round(4.0F)
            .softness(1.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getBlurRect(0.7F))
            .build()
      );
      if (showHeader && darkenHeader) {
         rectangle.render(
            ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), headerHeight)
               .round(4.0F, 0.0F, 4.0F, 0.0F)
               .softness(-0.5F)
               .thickness(0.0F)
               .color(ColorUtil.getRectDarker(0.9F))
               .build()
         );
      }

      float centerX = this.getX() + this.getWidth() / 2.0F;
      if (showHeader) {
         font.drawString(matrix, this.getName(), (int)(this.getX() + padding), (int)(this.getY() + 6.5F), ColorUtil.getText());
         float iconSize = headerHeight / 2.0F;
         float iconPadding = iconSize / 2.0F;
         Icons iconsModule = Icons.getInstance();
         int iconColor;
         if (!iconsModule.getColoredIcons().isValue()) {
            iconColor = ColorUtil.getText();
         } else if (iconsModule.getIconGradient().isValue()) {
            iconColor = ColorUtil.fade(8);
         } else {
            iconColor = ColorUtil.getClientColor();
         }

         image.setIcon(61443)
            .render(
               ShapeProperties.create(matrix, this.getX() + this.getWidth() - iconSize - iconPadding, this.getY() + 4, iconSize, iconSize)
                  .color(iconColor)
                  .build()
            );
      }

      int offset = showHeader ? (int)(headerHeight + 7.0F) : 7;
      int maxWidth = 80;

      for (CoolDowns.CoolDown coolDown : this.list) {
         float animation = coolDown.anim.getOutput().floatValue();
         float centerY = this.getY() + offset;
         int time = -coolDown.time.elapsedTime() / 1000;
         Text nameText = coolDown.stack.getName();
         String duration = StringUtil.getDuration(time);
         MathUtil.scale(
            matrix,
            centerX,
            centerY,
            1.0F,
            animation,
            () -> {
               HudRenderPipeline.getInstance()
                  .recordVanilla(
                     () -> Render2DUtil.defaultDrawStack(context, coolDown.stack, this.getX() + 4, centerY - 3.0F, false, false, 0.5F),
                     HudRenderPipeline.VanillaLayer.AFTER_RECT
                  );
               rectangle.render(ShapeProperties.create(matrix, this.getX() + 15, centerY - 1.0F, 0.5, 6.0).color(ColorUtil.getOutline(1.0F, 0.5F)).build());
               fontCoolDown.drawText(matrix, nameText, this.getX() + 18, centerY + 1.0F);
               int durationColor;
               if (time <= 5) {
                  float blinkProgress = MathUtil.blinking(1000.0, 8.0F);
                  durationColor = ColorUtil.overCol(ColorUtil.getText(), -11141291, blinkProgress);
               } else {
                  durationColor = ColorUtil.getText();
               }

               fontCoolDown.drawString(
                  matrix, duration, this.getX() + this.getWidth() - 5 - fontCoolDown.getStringWidth(duration), centerY + 1.0F, durationColor
               );
            }
         );
         int width = (int)(fontCoolDown.getStringWidth(nameText) + fontCoolDown.getStringWidth(duration)) + 30;
         maxWidth = Math.max(width, maxWidth);
         offset += (int)(11.0F * animation);
      }

      this.setWidth(maxWidth);
      this.setHeight(offset);
   }

   public record CoolDown(ItemStack stack, StopWatch time, Animation anim) {
   }
}
