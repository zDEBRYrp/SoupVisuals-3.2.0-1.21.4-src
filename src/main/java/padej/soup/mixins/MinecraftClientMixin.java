package padej.soup.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.event.events.container.SetScreenEvent;
import padej.soup.api.event.events.world.PlayerLeaveWorldEvent;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.file.exception.FileProcessingException;
import padej.soup.api.system.font.Fonts;
import padej.soup.base.QuickImports;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.core.Main;

@ProtIgnore
@Environment(EnvType.CLIENT)
@Mixin({MinecraftClient.class})
public abstract class MinecraftClientMixin implements QuickImports {
   @Shadow
   @Nullable
   public ClientPlayerEntity field_1724;
   @Shadow
   @Nullable
   public Screen field_1755;

   @Inject(
      at = {@At("TAIL")},
      method = {"<init>"}
   )
   private void onInit(RunArgs args, CallbackInfo ci) {
      Fonts.init();
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"stop"}
   )
   private void stop(CallbackInfo ci) {
      if (Main.getInstance().isInitialized()) {
         if (Main.getInstance().getActivityManager() != null) {
            Main.getInstance().getActivityManager().endSession();
         }

         try {
            Main.getInstance().getFileController().saveFiles();
         } catch (FileProcessingException var6) {
            LoggerUtil.error("Error occurred while saving files: " + var6.getMessage() + " " + var6.getCause());
         } finally {
            Main.getInstance().getFileController().stopAutoSave();
         }
      }
   }

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void setScreenHook(Screen screen, CallbackInfo ci) {
      SetScreenEvent event = new SetScreenEvent(screen);
      EventManager.callEvent(event);

      for (AbstractDraggable draggable : Main.getInstance().getDraggableRepository().draggable()) {
         draggable.setScreen(event);
      }

      Screen eventScreen = event.getScreen();
      if (screen != eventScreen) {
         mc.setScreen(eventScreen);
         ci.cancel();
      }
   }

   @Inject(
      method = {"disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V"},
      at = {@At("HEAD")},
      require = 0
   )
   private void onDisconnect(Screen screen, boolean transferring, CallbackInfo ci) {
      if (this.field_1724 != null) {
         EventManager.callEvent(new PlayerLeaveWorldEvent());
      }
   }
}
