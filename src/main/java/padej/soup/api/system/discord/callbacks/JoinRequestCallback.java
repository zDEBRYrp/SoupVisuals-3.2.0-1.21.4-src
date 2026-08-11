package padej.soup.api.system.discord.callbacks;

import com.sun.jna.Callback;
import padej.protect.ProtIgnore;
import padej.soup.api.system.discord.utils.DiscordUser;

@ProtIgnore
public interface JoinRequestCallback extends Callback {
   void apply(DiscordUser var1);
}
