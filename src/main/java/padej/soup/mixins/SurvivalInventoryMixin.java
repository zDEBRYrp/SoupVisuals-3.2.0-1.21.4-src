package padej.soup.mixins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.base.util.render.PhysicsIconRenderer;
import padej.soup.implement.features.modules.other.Trinket;

@ProtIgnore
@Mixin({InventoryScreen.class})
public abstract class SurvivalInventoryMixin extends HandledScreen {
   @Unique
   private static PhysicsIconRenderer physicsIcon;

   protected SurvivalInventoryMixin(Text title) {
      super(null, null, title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void hookInit(CallbackInfo ci) {
      if (physicsIcon == null) {
         physicsIcon = new PhysicsIconRenderer(this.width, this.height);
      } else {
         physicsIcon.updateScreenSize(this.width, this.height);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void hookRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (Trinket.getInstance().isEnabled()) {
         if (physicsIcon != null) {
            List<PhysicsIconRenderer.Rectangle> collisionRects = this.calculateCollisionRectangles();
            physicsIcon.setUIRectangles(collisionRects);
            physicsIcon.render(context, mouseX, mouseY, delta);
         }
      }
   }

   @Unique
   private List<PhysicsIconRenderer.Rectangle> calculateCollisionRectangles() {
      List<PhysicsIconRenderer.Rectangle> rectangles = new ArrayList<>();
      int invWidth = 176;
      int invHeight = 166;
      int invX = this.x;
      int invY = this.y;
      rectangles.add(new PhysicsIconRenderer.Rectangle(new Vec2f(invX, invY), new Vec2f(invX + invWidth, invY + invHeight)));
      int centerX = (this.width - invWidth) / 2;
      boolean recipeBookOpen = invX > centerX + 10;
      if (recipeBookOpen) {
         int recipeBookWidth = 147;
         int recipeBookX = invX - recipeBookWidth;
         rectangles.add(new PhysicsIconRenderer.Rectangle(new Vec2f(recipeBookX, invY), new Vec2f(recipeBookX + recipeBookWidth, invY + invHeight)));
      }

      rectangles.addAll(this.calculateStatusEffectRectangles());
      return rectangles;
   }

   @Unique
   private List<PhysicsIconRenderer.Rectangle> calculateStatusEffectRectangles() {
      List<PhysicsIconRenderer.Rectangle> effectRects = new ArrayList<>();
      if (this.client != null && this.client.player != null) {
         Collection<StatusEffectInstance> collection = this.client.player.getStatusEffects();
         if (collection.isEmpty()) {
            return effectRects;
         } else {
            int invX = this.x;
            int invWidth = 176;
            int effectStartX = invX + invWidth + 2;
            int availableWidth = this.width - effectStartX;
            if (availableWidth < 32) {
               return effectRects;
            } else {
               boolean wide = availableWidth >= 120;
               int rectWidth = wide ? 120 : 32;
               int k = 33;
               if (collection.size() > 5) {
                  k = 132 / (collection.size() - 1);
               }

               int yPos = this.y;

               for (StatusEffectInstance ignored : collection) {
                  effectRects.add(new PhysicsIconRenderer.Rectangle(new Vec2f(effectStartX, yPos), new Vec2f(effectStartX + rectWidth, yPos + 32)));
                  yPos += k;
               }

               return effectRects;
            }
         }
      } else {
         return effectRects;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (physicsIcon != null) {
         physicsIcon.handleMouseClick((int)mouseX, (int)mouseY, button);
         if (physicsIcon.isMouseOver((int)mouseX, (int)mouseY)) {
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (physicsIcon != null) {
         physicsIcon.handleMouseRelease((int)mouseX, (int)mouseY, button);
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (physicsIcon != null) {
         physicsIcon.handleMouseDrag((int)mouseX, (int)mouseY);
      }

      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   public void close() {
      super.close();
   }
}
