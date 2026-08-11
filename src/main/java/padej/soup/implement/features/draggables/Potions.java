package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
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
import padej.soup.base.util.other.StringUtil;
import padej.soup.base.util.render.Render2DUtil;
import padej.soup.implement.features.modules.hud.Icons;

public class Potions extends AbstractDraggable {
   private final List<Potions.Potion> list = new ArrayList<>();

   public Potions() {
      super("Potions", 210, 10, 80, 23, true);
   }

   @Override
   public boolean visible() {
      return !this.list.isEmpty() || PlayerIntersectionUtil.isChatOrMenu(mc.currentScreen);
   }

   @Override
   public void tick() {
      this.list.removeIf(p -> p.anim.isFinished(Direction.BACKWARDS));

      for (Potions.Potion potion : this.list) {
         potion.effect.update(mc.player, null);
      }

      if (mc.player != null) {
         for (StatusEffectInstance playerEffect : mc.player.getStatusEffects()) {
            String playerEffectId = playerEffect.getEffectType().getIdAsString();
            boolean exists = false;

            for (Potions.Potion potion : this.list) {
               if (potion.effect.getEffectType().getIdAsString().equals(playerEffectId)) {
                  exists = true;
                  break;
               }
            }

            if (!exists) {
               this.list.add(new Potions.Potion(playerEffect, new DecelerateAnimation().setMs(150).setValue(1.0)));
            }
         }
      }

      padej.soup.implement.features.modules.hud.Potions potionsModule = padej.soup.implement.features.modules.hud.Potions.getInstance();
      boolean useRoman = potionsModule.getRomanNumerals().isValue();

      for (Potions.Potion potionx : this.list) {
         StatusEffectInstance effect = potionx.effect;
         potionx.cachedName = ((StatusEffect)effect.getEffectType().value()).getName().getString();
         int amplifier = effect.getAmplifier();
         if (amplifier > 0) {
            String levelStr = useRoman ? StringUtil.toRoman(amplifier + 1) : String.valueOf(amplifier + 1);
            potionx.cachedLvl = " " + levelStr;
         } else {
            potionx.cachedLvl = "";
         }

         potionx.cachedDuration = this.getDuration(effect);
         potionx.cachedIsBeneficial = ((StatusEffect)effect.getEffectType().value()).isBeneficial();
         potionx.cachedIsBlinking = effect.getDuration() != -1 && effect.getDuration() <= 120;
      }
   }

   @Override
   public void packet(PacketEvent e) {
      switch (e.getPacket()) {
         case EntityStatusEffectS2CPacket effect:
            if (!PlayerIntersectionUtil.nullCheck() && effect.getEntityId() == Objects.requireNonNull(mc.player).getId()) {
               RegistryEntry<StatusEffect> effectId = effect.getEffectId();
               String effectTypeId = effectId.getIdAsString();

               for (Potions.Potion potionx : this.list) {
                  if (potionx.effect.getEffectType().getIdAsString().equals(effectTypeId)) {
                     potionx.anim.setDirection(Direction.BACKWARDS);
                  }
               }

               this.list
                  .add(
                     new Potions.Potion(
                        new StatusEffectInstance(
                           effectId, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()
                        ),
                        new DecelerateAnimation().setMs(150).setValue(1.0)
                     )
                  );
            }
            break;
         case RemoveEntityStatusEffectS2CPacket effectx:
            String effectTypeId = effectx.effect().getIdAsString();

            for (Potions.Potion potion : this.list) {
               if (potion.effect.getEffectType().getIdAsString().equals(effectTypeId)) {
                  potion.anim.setDirection(Direction.BACKWARDS);
               }
            }
            break;
         case PlayerRespawnS2CPacket ignored:
            this.list.clear();
            break;
         case GameJoinS2CPacket ignoredx:
            this.list.clear();
            break;
         default:
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(15, Fonts.Type.INTER_DEFAULT);
      FontRenderer fontPotion = Fonts.getSize(13, Fonts.Type.INTER_DEFAULT);
      padej.soup.implement.features.modules.hud.Potions potionsModule = padej.soup.implement.features.modules.hud.Potions.getInstance();
      float headerHeight = 16.0F;
      float padding = 5.0F;
      boolean showHeader = potionsModule.getShowHeader().isValue();
      boolean darkenHeader = potionsModule.getDarkenHeader().isValue();
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
      int offset = showHeader ? (int)(headerHeight + 7.0F) : 7;
      int maxWidth = 80;
      if (showHeader) {
         font.drawString(matrix, this.getName(), (int)(this.getX() + padding), (int)(this.getY() + 6.5F), ColorUtil.getText());
         float iconSize = 8.0F;
         float iconPadding = 4.0F;
         Icons iconsModule = Icons.getInstance();
         int iconColor;
         if (!iconsModule.getColoredIcons().isValue()) {
            iconColor = ColorUtil.getText();
         } else if (iconsModule.getIconGradient().isValue()) {
            iconColor = ColorUtil.fade(8);
         } else {
            iconColor = ColorUtil.getClientColor();
         }

         image.setIcon(61453)
            .render(
               ShapeProperties.create(matrix, this.getX() + this.getWidth() - iconSize - iconPadding, this.getY() + 4, iconSize, iconSize)
                  .color(iconColor)
                  .build()
            );
      }

      boolean coloredEffects = potionsModule.getColoredEffects().isValue();

      for (Potions.Potion potion : this.list) {
         StatusEffectInstance effect = potion.effect;
         float animation = potion.anim.getOutput().floatValue();
         float centerY = this.getY() + offset;
         String name = potion.cachedName;
         String lvl = potion.cachedLvl;
         String duration = potion.cachedDuration;
         MathUtil.scale(
            matrix,
            centerX,
            centerY,
            1.0F,
            animation,
            () -> {
               HudRenderPipeline.getInstance()
                  .recordVanilla(
                     () -> Render2DUtil.drawSprite(
                        matrix, mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType()), this.getX() + 5, (int)centerY - 2, 8.0F, 8
                     ),
                     HudRenderPipeline.VanillaLayer.AFTER_RECT
                  );
               rectangle.render(ShapeProperties.create(matrix, this.getX() + 14, centerY - 1.0F, 0.5, 7.0).color(ColorUtil.getOutline(0.75F, 0.5F)).build());
               int nameColor;
               if (coloredEffects) {
                  nameColor = potion.cachedIsBeneficial ? -11141291 : -43691;
               } else {
                  nameColor = ColorUtil.getText();
               }

               int durationColor;
               if (potion.cachedIsBlinking) {
                  float blinkProgress = MathUtil.blinking(1000.0, 8.0F);
                  durationColor = ColorUtil.overCol(ColorUtil.getText(), -43691, blinkProgress);
               } else {
                  durationColor = ColorUtil.getText();
               }

               fontPotion.drawString(matrix, name + lvl, this.getX() + 18, centerY + 1.0F, nameColor);
               fontPotion.drawString(matrix, duration, this.getX() + this.getWidth() - 5 - fontPotion.getStringWidth(duration), centerY + 1.0F, durationColor);
            }
         );
         int width = (int)fontPotion.getStringWidth(name + lvl + duration) + 30;
         maxWidth = Math.max(width, maxWidth);
         offset += (int)(11.0F * animation);
      }

      this.setWidth(maxWidth);
      this.setHeight(offset);
   }

   private String getDuration(StatusEffectInstance pe) {
      int var1 = pe.getDuration();
      int mins = var1 / 1200;
      return !pe.isInfinite() && mins <= 60 ? mins + ":" + String.format("%02d", var1 % 1200 / 20) : "**:**";
   }

   private static class Potion {
      final StatusEffectInstance effect;
      final Animation anim;
      String cachedName = "";
      String cachedLvl = "";
      String cachedDuration = "";
      boolean cachedIsBeneficial = false;
      boolean cachedIsBlinking = false;

      Potion(StatusEffectInstance effect, Animation anim) {
         this.effect = effect;
         this.anim = anim;
      }
   }
}
