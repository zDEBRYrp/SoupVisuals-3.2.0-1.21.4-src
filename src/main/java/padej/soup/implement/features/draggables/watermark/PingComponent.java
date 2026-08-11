package padej.soup.implement.features.draggables.watermark;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;

public class PingComponent implements WatermarkComponent, QuickImports {
   private int ping = 0;
   private float smoothedWidth = 0.0F;

   @Override
   public void tick() {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
         PlayerListEntry playerEntry = networkHandler.getPlayerListEntry(mc.player.getUuid());
         if (playerEntry != null) {
            this.ping = playerEntry.getLatency();
         }
      }
   }

   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      String pingText = this.ping + " ms";
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         image.setIcon(61452).render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, pingText, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(pingText);
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      int digitCount = String.valueOf(this.ping).length();
      String widestPing = "8".repeat(digitCount) + " ms";
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      return iconOffset + font.getStringWidth(widestPing);
   }

   @Override
   public float getSmoothedWidth(FontRenderer font, float height) {
      return this.getWidth(font, height);
   }

   @Override
   public String getName() {
      return "Ping";
   }
}
