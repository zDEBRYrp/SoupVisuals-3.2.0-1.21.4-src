package padej.soup.implement.menu.components.implement.other;

import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.file.impl.FriendFile;
import padej.soup.api.repository.friend.Friend;
import padej.soup.api.repository.friend.FriendUtils;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.core.Main;
import padej.soup.implement.menu.components.AbstractComponent;

public class FriendsListComponent extends AbstractComponent {
   public static boolean typing = false;
   private String inputText = "";
   private int cursorPosition = 0;
   private long lastClickTime = 0L;
   private float xOffset = 0.0F;
   private final Animation hoverAnimation = new DecelerateAnimation().setMs(200).setValue(1.0);
   private final Animation typingAnimation = new DecelerateAnimation().setMs(200).setValue(0.3F);
   private final Map<String, Animation> cardAppearAnimations = new HashMap<>();
   private final Map<String, Boolean> cardDisappearing = new HashMap<>();
   private double listScrollOffset = 0.0;
   private double smoothListScrollOffset = 0.0;
   private static final int HEADER_HEIGHT = 18;
   private static final int INPUT_HEIGHT = 15;
   private static final int FRIEND_ENTRY_HEIGHT = 14;
   private static final int FRIEND_ENTRY_SPACING = 2;
   private static final int PADDING = 9;
   private static final int MIN_CARD_HEIGHT = 130;

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      MatrixStack matrix = context.getMatrices();
      this.height = this.getComponentHeight();
      boolean isHovered = this.isHover(mouseX, mouseY);
      this.hoverAnimation.setDirection(isHovered ? Direction.FORWARDS : Direction.BACKWARDS);
      float scale = 1.0F + (this.hoverAnimation.getOutput().floatValue() - 1.0F) * 0.05F;
      matrix.push();
      matrix.translate(this.x + this.width / 2.0F, this.y + this.height / 2.0F, 0.0F);
      matrix.scale(scale, scale, 1.0F);
      matrix.translate(-(this.x + this.width / 2.0F), -(this.y + this.height / 2.0F), 0.0F);
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, this.width, 18.0).round(5.0F, 0.0F, 5.0F, 0.0F).color(ColorUtil.getGuiRectColor2(1.0F)).build()
      );
      rectangle.render(
         ShapeProperties.create(matrix, this.x, this.y, this.width, this.height)
            .round(5.0F)
            .softness(1.0F)
            .thickness(2.2F)
            .outlineColor(ColorUtil.getOutline(0.56F))
            .color(ColorUtil.getRectDarker(0.55F))
            .build()
      );
      Fonts.getSize(14, Fonts.Type.INTER_BOLD)
         .drawCenteredString(matrix, LocalizationManager.getInstance().get("ui.friends_list"), this.x + this.width / 2.0F, this.y + 7.0F, ColorUtil.getText());
      this.renderInputField(context, mouseX, mouseY);
      this.renderFriendsList(context, mouseX, mouseY);
      matrix.pop();
   }

   private void renderInputField(DrawContext context, int mouseX, int mouseY) {
      MatrixStack matrix = context.getMatrices();
      float inputY = this.y + 18.0F + 9.0F;
      boolean inputHovered = MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, inputY, this.width - 18.0F, 15.0);
      this.typingAnimation.setDirection(!typing && !inputHovered ? Direction.BACKWARDS : Direction.FORWARDS);
      float typingAnim = this.typingAnimation.getOutput().floatValue();
      rectangle.render(
         ShapeProperties.create(matrix, this.x + 9.0F, inputY, this.width - 18.0F, 15.0)
            .round(2.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getGuiRectColor(0.5F + typingAnim * 0.3F))
            .build()
      );
      FontRenderer font = Fonts.getSize(13);
      this.updateXOffset(font, this.cursorPosition);
      String displayText = this.inputText.equalsIgnoreCase("") && !typing ? LocalizationManager.getInstance().get("ui.friends.placeholder") : this.inputText;
      int textColor = this.inputText.isEmpty() && !typing ? ColorUtil.getDescription() : ColorUtil.getText();
      ScissorManager scissorManager = Main.getInstance().getScissorManager();
      scissorManager.push(renderMatrix, this.x + 9.0F + 3.0F, inputY, this.width - 18.0F - 6.0F, 15.0F);
      if (!this.inputText.isEmpty() && typing) {
         this.getSuggestion(this.inputText)
            .ifPresent(
               suggestion -> {
                  String remainingText = suggestion.substring(this.inputText.length());
                  float textWidth = font.getStringWidth(this.inputText);
                  FontRenderer italicFont = Fonts.getSize(13, Fonts.Type.INTER_DEFAULT);
                  italicFont.drawString(
                     context.getMatrices(), remainingText, this.x + 9.0F + 3.0F - this.xOffset + textWidth, inputY + 6.0F, ColorUtil.getDescription(0.47F)
                  );
               }
            );
      }

      font.drawString(matrix, displayText, this.x + 9.0F + 3.0F - this.xOffset, inputY + 6.0F, textColor);
      long currentTime = System.currentTimeMillis();
      boolean focused = typing && currentTime % 1000L < 500L;
      if (focused) {
         float cursorX = font.getStringWidth(this.inputText.substring(0, this.cursorPosition));
         rectangle.render(ShapeProperties.create(matrix, this.x + 9.0F + 3.0F - this.xOffset + cursorX, inputY + 2.0F, 0.5, 9.0).color(-1).build());
      }

      scissorManager.pop();
      String instructionText = this.inputText.isEmpty()
         ? LocalizationManager.getInstance().get("ui.friends.instruction_empty")
         : LocalizationManager.getInstance().get("ui.friends.instruction_add");
      Fonts.getSize(10).drawCenteredString(matrix, instructionText, this.x + this.width / 2.0F, inputY + 15.0F + 4.0F, ColorUtil.getDescription());
   }

   private void renderFriendsList(DrawContext context, int mouseX, int mouseY) {
      List<Friend> friends = FriendUtils.getFriends();
      MatrixStack matrix = context.getMatrices();
      ScissorManager scissorManager = Main.getInstance().getScissorManager();
      String searchText = this.inputText.toLowerCase();
      if (friends.isEmpty()) {
         float emptyY = this.y + 18.0F + 15.0F + 18.0F + 14.0F + 15.0F;
         Fonts.getSize(12)
            .drawCenteredString(
               matrix, LocalizationManager.getInstance().get("ui.friends.empty_title"), this.x + this.width / 2.0F, emptyY, ColorUtil.getDescription()
            );
         Fonts.getSize(10)
            .drawCenteredString(
               matrix,
               LocalizationManager.getInstance().get("ui.friends.empty_subtitle"),
               this.x + this.width / 2.0F,
               emptyY + 12.0F,
               ColorUtil.getDescription(0.4F)
            );
      } else {
         float listStartY = this.y + 18.0F + 15.0F + 18.0F + 14.0F;
         float listHeight = this.height - 74.0F;
         int maxScroll = this.getScroll(searchText, friends, (int)listHeight);
         this.listScrollOffset = MathHelper.clamp(this.listScrollOffset, 0.0, maxScroll);
         this.smoothListScrollOffset = MathUtil.interpolateSmooth(4.0, this.smoothListScrollOffset, this.listScrollOffset);
         scissorManager.push(renderMatrix, this.x + 9.0F, listStartY, this.width - 18.0F, listHeight);
         float currentY = listStartY - (float)this.smoothListScrollOffset;
         int visibleIndex = 0;

         for (int i = 0; i < friends.size(); i++) {
            Friend friend = friends.get(i);
            if (searchText.isEmpty() || friend.getName().toLowerCase().contains(searchText)) {
               float entryY = currentY + visibleIndex * 16;
               if (!(entryY + 14.0F < listStartY) && !(entryY > listStartY + listHeight)) {
                  boolean withinScrollableArea = mouseX >= this.x + 9.0F
                     && mouseX <= this.x + this.width - 9.0F
                     && mouseY >= listStartY
                     && mouseY <= listStartY + listHeight;
                  boolean entryHovered = withinScrollableArea && mouseY >= entryY && mouseY <= entryY + 14.0F;
                  String friendKey = friend.getName() + "_" + i;
                  Animation appearAnimation = this.cardAppearAnimations.computeIfAbsent(friendKey, k -> new DecelerateAnimation().setMs(300).setValue(1.0));
                  float animValue = appearAnimation.getOutput().floatValue();
                  boolean isDisappearing = this.cardDisappearing.getOrDefault(friendKey, false);
                  float effectiveValue = isDisappearing ? 1.0F - animValue : animValue;
                  float scale = 0.8F + effectiveValue * 0.2F;
                  int alpha = (int)(255.0F * effectiveValue);
                  float cardCenterX = this.x + 9.0F + (this.width - 18.0F) / 2.0F;
                  float cardCenterY = entryY + 7.0F;
                  MathUtil.scale(
                     matrix,
                     cardCenterX,
                     cardCenterY,
                     scale,
                     () -> {
                        int bgColor = alpha << 24 | ColorUtil.getGuiRectColor(0.4F) & 16777215;
                        int outlineColor = alpha << 24 | (entryHovered ? ColorUtil.getClientColor(0.8F) : ColorUtil.getOutline()) & 16777215;
                        rectangle.render(
                           ShapeProperties.create(matrix, this.x + 9.0F, entryY, this.width - 18.0F, 14.0)
                              .round(2.0F)
                              .thickness(2.0F)
                              .outlineColor(outlineColor)
                              .color(bgColor)
                              .build()
                        );
                        int textColor = alpha << 24 | ColorUtil.getText() & 16777215;
                        Fonts.getSize(12, Fonts.Type.INTER_BOLD).drawString(matrix, friend.getName(), this.x + 9.0F + 5.0F, entryY + 6.0F, textColor);
                        float iconSize = 8.4F;
                        float deleteX = this.x + this.width - 9.0F - iconSize - 4.0F;
                        float deleteY = entryY + (14.0F - iconSize) / 2.0F;
                        int baseIconColor = entryHovered ? ColorUtil.getClientColor() : ColorUtil.getDescription();
                        int iconColor = alpha << 24 | baseIconColor & 16777215;
                        image.setIcon(61459).render(ShapeProperties.create(matrix, deleteX, deleteY, iconSize, iconSize).color(iconColor).build());
                     }
                  );
                  visibleIndex++;
               } else {
                  visibleIndex++;
               }
            }
         }

         this.cardAppearAnimations.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            Animation animation = entry.getValue();
            boolean isDisappearingx = this.cardDisappearing.getOrDefault(key, false);
            if (isDisappearingx && animation.getOutput().floatValue() <= 0.01F) {
               this.cardDisappearing.remove(key);
               return true;
            } else {
               return false;
            }
         });
         scissorManager.pop();
      }
   }

   private int getScroll(String searchText, List<Friend> friends, int listHeight) {
      int visibleFriendsCount = 0;
      if (searchText.isEmpty()) {
         visibleFriendsCount = friends.size();
      } else {
         for (Friend friend : friends) {
            if (friend.getName().toLowerCase().contains(searchText)) {
               visibleFriendsCount++;
            }
         }
      }

      int totalContentHeight = visibleFriendsCount * 16;
      return Math.max(0, totalContentHeight - listHeight);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         float inputY = this.y + 18.0F + 9.0F;
         if (MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, inputY, this.width - 18.0F, 15.0)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastClickTime < 250L) {
               this.cursorPosition = this.inputText.length();
            } else {
               SearchComponent.typing = false;
               ConfigManagerComponent.typing = false;
               typing = true;
               this.lastClickTime = currentTime;
               this.cursorPosition = this.getCursorIndexAt(mouseX);
            }

            return true;
         } else {
            List<Friend> friends = FriendUtils.getFriends();
            if (!friends.isEmpty()) {
               float listStartY = this.y + 18.0F + 15.0F + 18.0F + 14.0F;
               float listHeight = this.height - 74.0F;
               if (MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, listStartY, this.width - 18.0F, listHeight)) {
                  float currentY = listStartY - (float)this.smoothListScrollOffset;

                  for (int i = 0; i < friends.size(); i++) {
                     Friend friend = friends.get(i);
                     float entryY = currentY + i * 16;
                     if (!(entryY + 14.0F < listStartY) && !(entryY > listStartY + listHeight)) {
                        float iconSize = 8.0F;
                        float deleteX = this.x + this.width - 9.0F - iconSize - 4.0F;
                        float deleteY = entryY + (14.0F - iconSize) / 2.0F;
                        if (mouseX >= deleteX - 2.0F && mouseX <= deleteX + iconSize + 2.0F && mouseY >= deleteY - 2.0F && mouseY <= deleteY + iconSize + 2.0F) {
                           this.removeFriend(friend.getName());
                           return true;
                        }
                     }
                  }

                  return true;
               }
            }

            typing = false;
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      List<Friend> friends = FriendUtils.getFriends();
      if (friends.isEmpty()) {
         return super.mouseScrolled(mouseX, mouseY, amount);
      } else {
         float listStartY = this.y + 18.0F + 15.0F + 18.0F + 14.0F;
         float listHeight = this.height - 74.0F;
         if (MathUtil.isHovered(mouseX, mouseY, this.x + 9.0F, listStartY, this.width - 18.0F, listHeight)) {
            int totalContentHeight = friends.size() * 16;
            int maxScroll = Math.max(0, totalContentHeight - (int)listHeight);
            if (maxScroll > 0) {
               this.listScrollOffset = MathHelper.clamp(this.listScrollOffset - amount * 15.0, 0.0, maxScroll);
               return true;
            }
         }

         return super.mouseScrolled(mouseX, mouseY, amount);
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (typing) {
         switch (keyCode) {
            case 257:
            case 259:
               this.handleTextModification(keyCode);
               break;
            case 258:
               this.handleTabCompletion();
            case 260:
            case 261:
            default:
               break;
            case 262:
            case 263:
               this.moveCursor(keyCode);
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (typing && Fonts.getSize(13).getStringWidth(this.inputText) < this.width - 18.0F - 7.0F) {
         this.inputText = this.inputText.substring(0, this.cursorPosition) + chr + this.inputText.substring(this.cursorPosition);
         this.cursorPosition++;
         return true;
      } else {
         return false;
      }
   }

   private void addFriend() {
      String friendName = this.inputText.trim();
      if (!friendName.isEmpty() && !FriendUtils.isFriend(friendName)) {
         FriendUtils.addFriend(friendName);
         this.inputText = "";
         this.cursorPosition = 0;
         typing = false;
         this.saveToFile();
      }
   }

   private void removeFriend(String friendName) {
      this.cardAppearAnimations.forEach((key, animation) -> {
         if (key.startsWith(friendName + "_")) {
            this.cardDisappearing.put(key, true);
            animation.setDirection(Direction.BACKWARDS);
         }
      });
      FriendUtils.removeFriend(friendName);
      this.saveToFile();
   }

   private void saveToFile() {
      try {
         File configDir = new File("./soupapi/files/");
         if (!configDir.exists()) {
            configDir.mkdirs();
         }

         FriendFile friendFile = new FriendFile();
         friendFile.saveToFile(configDir);
      } catch (Exception var3) {
         LoggerUtil.error("Failed to save friends to file", var3);
      }
   }

   public int getComponentHeight() {
      List<Friend> friends = FriendUtils.getFriends();
      int baseHeight = 20;
      if (friends.isEmpty()) {
         int emptyHeight = baseHeight + 50;
         return Math.max(emptyHeight, 130);
      } else {
         int friendsListHeight = Math.min(friends.size() * 16, 120);
         int totalHeight = baseHeight + friendsListHeight + 10;
         return Math.max(totalHeight, 130);
      }
   }

   private void handleTextModification(int keyCode) {
      if (keyCode == 259) {
         if (this.cursorPosition > 0) {
            this.inputText = this.inputText.substring(0, this.cursorPosition - 1) + this.inputText.substring(this.cursorPosition);
            this.cursorPosition--;
         }
      } else if (keyCode == 257) {
         this.addFriend();
      }
   }

   private void moveCursor(int keyCode) {
      if (keyCode == 263 && this.cursorPosition > 0) {
         this.cursorPosition--;
      } else if (keyCode == 262 && this.cursorPosition < this.inputText.length()) {
         this.cursorPosition++;
      }
   }

   private int getCursorIndexAt(double mouseX) {
      FontRenderer font = Fonts.getSize(13);
      float relativeX = (float)mouseX - this.x - 9.0F - 3.0F + this.xOffset;

      int position;
      for (position = 0; position < this.inputText.length(); position++) {
         float textWidth = font.getStringWidth(this.inputText.substring(0, position + 1));
         if (textWidth > relativeX) {
            break;
         }
      }

      return position;
   }

   private void updateXOffset(FontRenderer font, int cursorPosition) {
      float cursorX = font.getStringWidth(this.inputText.substring(0, cursorPosition));
      if (cursorX < this.xOffset) {
         this.xOffset = cursorX;
      } else if (cursorX - this.xOffset > this.width - 18.0F - 7.0F) {
         this.xOffset = cursorX - (this.width - 18.0F - 7.0F);
      }
   }

   private Optional<String> getSuggestion(String input) {
      if (input.isEmpty()) {
         return Optional.empty();
      } else {
         String lowerInput = input.toLowerCase();
         List<String> suggestions = new ArrayList<>();
         MinecraftClient mc = Main.mc;
         if (mc.getNetworkHandler() != null) {
            suggestions.addAll(
               mc.getNetworkHandler()
                  .getPlayerList()
                  .stream()
                  .<GameProfile>map(PlayerListEntry::getProfile)
                  .map(profile -> profile.getName())
                  .filter(name -> name.toLowerCase().startsWith(lowerInput))
                  .collect(Collectors.toList())
            );
         }

         for (Friend friend : FriendUtils.getFriends()) {
            String friendName = friend.getName();
            if (friendName.toLowerCase().startsWith(lowerInput) && !suggestions.contains(friendName)) {
               suggestions.add(friendName);
            }
         }

         return suggestions.stream().findFirst();
      }
   }

   private void handleTabCompletion() {
      if (!this.inputText.isEmpty()) {
         this.getSuggestion(this.inputText).ifPresent(suggestion -> {
            this.inputText = suggestion;
            this.cursorPosition = this.inputText.length();
         });
      }
   }
}
