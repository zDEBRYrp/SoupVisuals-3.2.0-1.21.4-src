package padej.soup.api.system.discord.utils;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;
import padej.protect.ProtIgnore;

@ProtIgnore
public class DiscordUser extends Structure {
   public String userId;
   public String username;
   @Deprecated
   public String discriminator;
   public String avatar;

   protected List<String> getFieldOrder() {
      return Arrays.asList("userId", "username", "discriminator", "avatar");
   }
}
