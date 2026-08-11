package padej.soup.mixins;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import padej.protect.ProtIgnore;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.base.QuickImports;
import padej.soup.core.Main;

@ProtIgnore
@Mixin({ChatScreen.class})
public class ChatScreenMixin extends Screen implements QuickImports {
   @Unique
   List<AbstractDraggable> draggable = Main.getInstance().getDraggableRepository().draggable();

   protected ChatScreenMixin() {
      super(Text.empty());
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      AbstractDraggable active = null;

      for (AbstractDraggable drag : this.draggable) {
         if (drag.canDraw(drag) && drag.isDragging()) {
            active = drag;
         }
      }

      if (active != null) {
         active.render(context, mouseX, mouseY, delta);
      }
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("TAIL")}
   )
   private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      for (AbstractDraggable drag : this.draggable) {
         drag.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      for (AbstractDraggable drag : this.draggable) {
         drag.mouseReleased(mouseX, mouseY, button);
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }
}
