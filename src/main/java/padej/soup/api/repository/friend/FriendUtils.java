package padej.soup.api.repository.friend;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class FriendUtils {
   public static final List<Friend> friends = new ArrayList<>();

   public static void addFriend(String name) {
      friends.add(new Friend(name));
   }

   public static void removeFriend(String name) {
      friends.removeIf(friend -> friend.getName().equalsIgnoreCase(name));
   }

   public static boolean isFriend(Entity entity) {
      return entity instanceof PlayerEntity player ? isFriend(player.getName().getString()) : false;
   }

   public static boolean isFriend(String friend) {
      for (Friend f : friends) {
         if (f.getName().equals(friend)) {
            return true;
         }
      }

      return false;
   }

   public static void clear() {
      friends.clear();
   }

   private FriendUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static List<Friend> getFriends() {
      return friends;
   }
}
