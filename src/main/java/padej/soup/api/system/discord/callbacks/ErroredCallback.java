package padej.soup.api.system.discord.callbacks;

import com.sun.jna.Callback;
import padej.protect.ProtIgnore;

@ProtIgnore
public interface ErroredCallback extends Callback {
   void apply(int var1, String var2);
}
