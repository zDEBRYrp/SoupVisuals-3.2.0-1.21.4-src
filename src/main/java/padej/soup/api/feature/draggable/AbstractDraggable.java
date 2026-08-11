package padej.soup.api.feature.draggable;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.event.events.container.SetScreenEvent;
import padej.soup.api.event.events.packet.PacketEvent;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleProvider;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.base.QuickImports;
import padej.soup.base.QuickLogger;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.render.Render2DUtil;

public abstract class AbstractDraggable implements Draggable, QuickImports, QuickLogger {
   private String name;
   private int x;
   private int y;
   private int width;
   private int height;
   private boolean dragging;
   private boolean canDrag;
   private float dragX;
   private float dragY;
   private static Runnable saveCallback;
   public Animation scaleAnimation = new DecelerateAnimation().setValue(1.0).setMs(200);

   public static void setSaveCallback(Runnable callback) {
      saveCallback = callback;
   }

   public AbstractDraggable(String name, int x, int y, int width, int height, boolean canDrag) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.canDrag = canDrag;
   }

   @Override
   public boolean visible() {
      return true;
   }

   @Override
   public void tick() {
   }

   @Override
   public void packet(PacketEvent e) {
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      int windowWidth = window.getScaledWidth();
      int windowHeight = window.getScaledHeight();
      int radius = 3;
      float renderWidth = this.getRenderWidth();
      float renderHeight = this.getRenderHeight();
      if (this.dragging) {
         float mouseCenterX = mouseX + this.dragX;
         float mouseCenterY = mouseY + this.dragY;
         float mouseDragX = mouseCenterX - renderWidth / 2.0F;
         float mouseDragY = mouseCenterY - renderHeight / 2.0F;
         float clampedX = Math.max(0.0F, Math.min(mouseDragX, windowWidth - renderWidth));
         float clampedY = Math.max(0.0F, Math.min(mouseDragY, windowHeight - renderHeight));
         DraggableRepository repo = DraggableRepository.getInstance();
         if (repo != null) {
            for (AbstractDraggable drag : repo.draggable()) {
               if (this.canDraw(drag) && drag.canDrag && drag != this) {
                  float dragX = drag.getRenderX();
                  float dragY = drag.getRenderY();
                  float dragWidth = drag.getRenderWidth();
                  float dragHeight = drag.getRenderHeight();
                  float x1 = dragX + dragWidth + radius;
                  float x2 = dragX - renderWidth - radius;
                  float y1 = dragY + dragHeight + radius;
                  float y2 = dragY - renderHeight - radius;
                  if (Math.abs(x1 - mouseDragX) <= radius) {
                     this.drawRect(x1 - 1.5F, 0.0F, 1.0F, windowHeight);
                     clampedX = x1;
                  }

                  if (Math.abs(x2 - mouseDragX) <= radius) {
                     this.drawRect(x2 + renderWidth + 1.0F, 0.0F, 1.0F, windowHeight);
                     clampedX = x2;
                  }

                  if (Math.abs(y1 - mouseDragY) <= radius) {
                     this.drawRect(0.0F, y1 - 1.5F, windowWidth, 1.0F);
                     clampedY = y1;
                  }

                  if (Math.abs(y2 - mouseDragY) <= radius) {
                     this.drawRect(0.0F, y2 + renderHeight + 1.0F, windowWidth, 1.0F);
                     clampedY = y2;
                  }

                  if (Math.abs(dragY - mouseDragY) <= radius) {
                     this.drawRect(0.0F, dragY - 1.5F, windowWidth, 1.0F);
                     clampedY = dragY;
                  }
               }
            }
         }

         if (Math.abs(mouseDragX - (windowWidth - renderWidth) / 2.0F) <= radius) {
            this.drawRect(windowWidth / 2.0F - 0.5F, 0.0F, 1.0F, windowHeight);
            clampedX = (windowWidth - renderWidth) / 2.0F;
         }

         if (Math.abs(mouseDragY - (windowHeight - renderHeight) / 2.0F) <= radius) {
            this.drawRect(0.0F, windowHeight / 2.0F - 0.5F, windowWidth, 1.0F);
            clampedY = (windowHeight - renderHeight) / 2.0F;
         }

         this.setPositionFromRender(clampedX, clampedY);
      }
   }

   @Override
   public void setScreen(SetScreenEvent e) {
      if (PlayerIntersectionUtil.isChat(e.getScreen())) {
         this.dragging = false;
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (MathUtil.isHovered(mouseX, mouseY, this.getRenderX(), this.getRenderY(), this.getRenderWidth(), this.getRenderHeight())
         && button == 0
         && this.canDrag) {
         this.dragging = true;
         this.dragX = this.getRenderX() + this.getRenderWidth() / 2.0F - (float)mouseX;
         this.dragY = this.getRenderY() + this.getRenderHeight() / 2.0F - (float)mouseY;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.dragging) {
         this.dragging = false;
         if (saveCallback != null) {
            saveCallback.run();
         }

         return true;
      } else {
         return false;
      }
   }

   public abstract void drawDraggable(DrawContext var1);

   public void drawRect(float x, float y, float width, float height) {
      Render2DUtil.drawQuad(x, y, width, height, ColorUtil.getText(0.5F));
   }

   public void stopAnimation() {
      this.scaleAnimation.setDirection(Direction.BACKWARDS);
   }

   public void startAnimation() {
      this.scaleAnimation.setDirection(Direction.FORWARDS);
   }

   public void validPosition() {
   }

   public boolean canDraw(AbstractDraggable draggable) {
      ModuleProvider provider = ModuleProvider.getInstance();
      Module module = provider != null ? provider.get(draggable.getName()) : null;
      return module != null && module.isEnabled() && this.visible();
   }

   public float getDraggableScale() {
      return this.getInteractionScale();
   }

   protected float getInteractionScale() {
      ModuleProvider provider = ModuleProvider.getInstance();
      Module module = provider != null ? provider.get(this.getName()) : null;
      if (module == null) {
         return 1.0F;
      } else {
         String settingKey = "setting." + module.getIdentifier() + ".draggablescale.name";
         return module.get(settingKey) instanceof ValueSetting valueSetting ? MathHelper.clamp(valueSetting.getValue(), 0.5F, 2.0F) : 1.0F;
      }
   }

   public float getRenderX() {
      return this.x + (this.width - this.getRenderWidth()) / 2.0F;
   }

   public float getRenderY() {
      return this.y + (this.height - this.getRenderHeight()) / 2.0F;
   }

   public float getRenderWidth() {
      return this.width * this.getInteractionScale();
   }

   public float getRenderHeight() {
      return this.height * this.getInteractionScale();
   }

   private void setPositionFromRender(float renderX, float renderY) {
      float renderWidth = this.getRenderWidth();
      float renderHeight = this.getRenderHeight();
      this.x = Math.round(renderX - (this.width - renderWidth) / 2.0F);
      this.y = Math.round(renderY - (this.height - renderHeight) / 2.0F);
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public void setDragging(boolean dragging) {
      this.dragging = dragging;
   }

   public void setCanDrag(boolean canDrag) {
      this.canDrag = canDrag;
   }

   public void setDragX(float dragX) {
      this.dragX = dragX;
   }

   public void setDragY(float dragY) {
      this.dragY = dragY;
   }

   public void setScaleAnimation(Animation scaleAnimation) {
      this.scaleAnimation = scaleAnimation;
   }

   public String getName() {
      return this.name;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public boolean isDragging() {
      return this.dragging;
   }

   public boolean isCanDrag() {
      return this.canDrag;
   }

   public float getDragX() {
      return this.dragX;
   }

   public float getDragY() {
      return this.dragY;
   }

   public Animation getScaleAnimation() {
      return this.scaleAnimation;
   }
}
