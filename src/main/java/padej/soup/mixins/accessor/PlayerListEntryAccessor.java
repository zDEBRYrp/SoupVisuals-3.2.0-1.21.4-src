package padej.soup.mixins.accessor;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import padej.protect.ProtIgnore;

@ProtIgnore
@Mixin({PlayerListEntry.class})
public interface PlayerListEntryAccessor {
   @Accessor("profile")
   GameProfile getProfile();
}
