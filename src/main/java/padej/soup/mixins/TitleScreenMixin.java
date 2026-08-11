package padej.soup.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.core.server.ServerApi;

@ProtIgnore
@Mixin({TitleScreen.class})
public class TitleScreenMixin extends Screen {
   protected TitleScreenMixin(Text title) {
      super(title);
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   public void hookRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      int onlineCount = ServerApi.getCurrentOnlineCount();
      if (onlineCount > 0) {
         String onlineText = LocalizationManager.getInstance().get("ui.online");
         String text = "§a⏺ " + onlineText + ": " + onlineCount;
         int textWidth = this.textRenderer.getWidth(text);
         int x = this.width - textWidth - 5;
         int y = 5;
         context.drawTextWithShadow(this.textRenderer, text, x, y, -1);
      }
   }
}
