package padej.soup.api.system.discord.callbacks;

import com.sun.jna.Callback;
import padej.protect.ProtIgnore;

@ProtIgnore
public interface SpectateGameCallback extends Callback {
   void apply(String var1);
}
