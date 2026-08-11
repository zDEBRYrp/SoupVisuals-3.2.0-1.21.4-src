package padej.soup.base.util.world;

import java.util.Objects;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.event.events.packet.PacketEvent;
import padej.soup.base.QuickImports;

public final class ServerUtil implements QuickImports {
   public static String server = "Vanilla";
   public static float TPS = 20.0F;
   public static long timestamp;

   public static void packet(PacketEvent e) {
      if (Objects.requireNonNull(e.getPacket()) instanceof WorldTimeUpdateS2CPacket) {
         long nanoTime = System.nanoTime();
         float maxTPS = 20.0F;
         float rawTPS = maxTPS * (1.0E9F / (float)(nanoTime - timestamp));
         TPS = MathHelper.clamp(rawTPS, 0.0F, maxTPS);
         timestamp = nanoTime;
      }
   }

   private ServerUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
