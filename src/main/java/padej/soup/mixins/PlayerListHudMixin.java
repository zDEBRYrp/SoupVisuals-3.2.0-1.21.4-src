package padej.soup.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.repository.client.OnlinePlayersManager;
import padej.soup.api.repository.friend.FriendUtils;
import padej.soup.base.util.render.BadgeRenderer;
import padej.soup.core.Main;
import padej.soup.implement.features.modules.hud.TabList;
import padej.soup.mixins.accessor.PlayerListEntryAccessor;

@ProtIgnore
@Mixin(
   value = {PlayerListHud.class},
   priority = 10000
)
public abstract class PlayerListHudMixin {
   @Unique
   private static final int ICON_WIDTH = 12;
   @Unique
   private final Map<String, PlayerListEntry> soupapi$entryByText = new HashMap<>();
   @Unique
   private final Map<String, int[]> soupapi$friendBackgrounds = new HashMap<>();

   @Shadow
   public abstract Text method_1918(PlayerListEntry var1);

   @ModifyVariable(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/PlayerListHud;getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;",
         shift = Shift.BEFORE
      ),
      ordinal = 0
   )
   private PlayerListEntry capturePlayerEntry(PlayerListEntry playerListEntry) {
      Text playerName = this.method_1918(playerListEntry);
      if (playerName != null) {
         this.soupapi$entryByText.put(playerName.getString(), playerListEntry);
      }

      return playerListEntry;
   }

   @WrapOperation(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",
         ordinal = 2
      )}
   )
   private void captureFillCoordinates(DrawContext instance, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
      original.call(new Object[]{instance, x1, y1, x2, y2, color});
      int[] coords = new int[]{x1, y1, x2, y2};
      this.soupapi$friendBackgrounds.put("y_" + y1, coords);
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"},
      at = {@At("HEAD")}
   )
   private void clearFriendBackgrounds(CallbackInfo ci) {
      this.soupapi$friendBackgrounds.clear();
      this.soupapi$entryByText.clear();
   }

   @WrapOperation(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"
      )}
   )
   private int wrapDrawText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, Operation<Integer> original) {
      String textString = text.getString();
      PlayerListEntry entry = this.soupapi$entryByText.get(textString);
      if (entry != null) {
         GameProfile profile = ((PlayerListEntryAccessor)entry).getProfile();
         String realUsername = profile.getName();
         TabList tabList = TabList.getInstance();
         if (tabList != null && tabList.isEnabled() && FriendUtils.isFriend(realUsername)) {
            int[] coords = this.soupapi$friendBackgrounds.get("y_" + y);
            if (coords != null) {
               int friendBgColor = tabList.getFriendBackgroundColor();
               context.fill(coords[0], coords[1], coords[2], coords[3], friendBgColor);
            }
         }

         if (OnlinePlayersManager.isPlayerOnline(realUsername)) {
            MinecraftClient mc = Main.mc;
            Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
            BadgeRenderer.render2D(context.getMatrices(), vertexConsumers, x, y, realUsername);
            vertexConsumers.draw();
            return (Integer)original.call(new Object[]{context, textRenderer, text, x + 12, y, color});
         }
      }

      return (Integer)original.call(new Object[]{context, textRenderer, text, x, y, color});
   }

   @WrapOperation(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Lnet/minecraft/text/StringVisitable;)I"
      )}
   )
   private int expandTabWidthForBadges(TextRenderer textRenderer, StringVisitable stringVisitable, Operation<Integer> original) {
      int width = (Integer)original.call(new Object[]{textRenderer, stringVisitable});
      if (stringVisitable instanceof Text text) {
         PlayerListEntry entry = this.soupapi$entryByText.get(text.getString());
         if (entry == null) {
            return width;
         } else {
            String realUsername = ((PlayerListEntryAccessor)entry).getProfile().getName();
            return OnlinePlayersManager.isPlayerOnline(realUsername) ? width + 12 : width;
         }
      } else {
         return width;
      }
   }

   @ModifyConstant(
      constant = {@Constant(
         longValue = 80L
      )},
      method = {"collectPlayerEntries"}
   )
   private long modifyCount(long count) {
      return TabList.getInstance().getExtendedTab().isValue() ? 180L : 80L;
   }
}
