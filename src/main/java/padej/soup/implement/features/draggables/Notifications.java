package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import padej.soup.api.event.events.container.SetScreenEvent;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.notification.NotificationService;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.implement.features.modules.hud.Icons;

public class Notifications extends AbstractDraggable implements NotificationService {
   private final List<Notifications.Notification> list = new ArrayList<>();
   private final List<Notifications.Stack> stacks = new ArrayList<>();

   public static Notifications getInstance() {
      return Instance.getDraggable(Notifications.class);
   }

   public Notifications() {
      super("Notifications", 0, 50, 100, 15, true);
      NotificationService.Registry.set(this);
   }

   @Override
   public void tick() {
      this.list
         .forEach(
            notif -> {
               if (System.currentTimeMillis() > notif.removeTime
                  || notif.text.getString().contains("Пример Уведомления") && !PlayerIntersectionUtil.isChat(mc.currentScreen)) {
                  notif.anim.setDirection(Direction.BACKWARDS);
               }
            }
         );
      this.list.removeIf(notif -> notif.anim.isFinished(Direction.BACKWARDS));

      while (!this.stacks.isEmpty()) {
         this.addTextIfNotEmpty(Notifications.TypePickUp.INVENTORY, "Подняты предметы: ");
         this.addTextIfNotEmpty(Notifications.TypePickUp.SHULKER_INVENTORY, "Сложены предметы в шалкер: ");
         this.addTextIfNotEmpty(Notifications.TypePickUp.SHULKER, "Поднят шалкер с: ");
      }
   }

   @Override
   public void setScreen(SetScreenEvent e) {
      if (e.getScreen() instanceof ChatScreen) {
         this.addList("Пример Уведомления", 2147483647L);
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(12, Fonts.Type.INTER_DEFAULT);
      this.setX((window.getScaledWidth() - this.getWidth()) / 2);
      float offsetY = 0.0F;
      float iconSize = 8.0F;
      float iconGap = 3.0F;
      float padding = 5.0F;
      padej.soup.implement.features.modules.hud.Notifications notificationsModule = padej.soup.implement.features.modules.hud.Notifications.getInstance();

      for (Notifications.Notification notification : this.list) {
         float anim = notification.anim.getOutput().floatValue();
         float textWidth = font.getStringWidth(notification.text);
         float width = iconSize + iconGap + textWidth + padding * 2.0F;
         float startY = this.getY() + offsetY;
         float startX = this.getX() + (this.getWidth() - width) / 2.0F;
         MathUtil.setAlpha(
            anim,
            () -> {
               blur.render(
                  ShapeProperties.create(matrix, startX, startY, width, this.getHeight())
                     .round(3.0F)
                     .outlineColor(ColorUtil.getOutline())
                     .color(ColorUtil.getBlurRect(0.7F))
                     .build()
               );
               Icons iconsModule = Icons.getInstance();
               int iconColor;
               if (!iconsModule.getColoredIcons().isValue()) {
                  iconColor = ColorUtil.getText();
               } else if (iconsModule.getIconGradient().isValue()) {
                  iconColor = ColorUtil.fade(8);
               } else {
                  iconColor = ColorUtil.getClientColor();
               }

               image.setIcon(61440).render(ShapeProperties.create(matrix, startX + padding, startY + 3.5F, iconSize, iconSize).color(iconColor).build());
               font.drawText(matrix, notification.text, (int)(startX + padding + iconSize + iconGap), startY + 6.5F);
            }
         );
         offsetY += (this.getHeight() + 3) * anim;
      }
   }

   private void addTextIfNotEmpty(Notifications.TypePickUp type, String prefix) {
      MutableText text = Text.empty();
      List<Notifications.Stack> list = this.stacks.stream().filter(stackx -> stackx.type.equals(type)).toList();
      int i = 0;

      for (int size = list.size(); i < size; i++) {
         Notifications.Stack stack = list.get(i);
         if (stack.type == type) {
            text.append(stack.text);
            this.stacks.remove(stack);
            if (text.getString().length() > 150) {
               break;
            }

            if (i + 1 != size) {
               text.append(" ,  ");
            }
         }
      }

      if (!text.equals(Text.empty())) {
         this.addList(Text.empty().append(prefix).append(text), 8000L);
      }
   }

   public void addList(String text, long removeTime) {
      this.addList(text, removeTime, null);
   }

   public void addList(Text text, long removeTime) {
      this.addList(text, removeTime, null);
   }

   public void addList(String text, long removeTime, SoundEvent sound) {
      this.addList(Text.empty().append(text), removeTime, sound);
   }

   public void addList(Text text, long removeTime, SoundEvent sound) {
      this.list.add(new Notifications.Notification(text, new DecelerateAnimation().setMs(300).setValue(1.0), System.currentTimeMillis() + removeTime));
      if (this.list.size() > 12) {
         this.list.removeFirst();
      }

      this.list.sort(Comparator.comparingDouble(notif -> -notif.removeTime));
      if (sound != null) {
         SoundManager.playSound(sound);
      }
   }

   @Override
   public void show(String text, long durationMs) {
      this.addList(text, durationMs, null);
   }

   @Override
   public void show(Text text, long durationMs) {
      this.addList(text, durationMs, null);
   }

   @Override
   public void show(String text, long durationMs, SoundEvent sound) {
      this.addList(text, durationMs, sound);
   }

   @Override
   public void show(Text text, long durationMs, SoundEvent sound) {
      this.addList(text, durationMs, sound);
   }

   public record Notification(Text text, Animation anim, long removeTime) {
   }

   public record Stack(Notifications.TypePickUp type, MutableText text) {
   }

   public static enum TypePickUp {
      INVENTORY,
      SHULKER,
      SHULKER_INVENTORY;
   }
}
