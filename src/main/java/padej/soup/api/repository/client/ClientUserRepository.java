package padej.soup.api.repository.client;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientUserRepository {
   private static final Set<String> USERS = ConcurrentHashMap.newKeySet();

   public static void addUser(String username) {
      if (username != null && !username.isEmpty()) {
         USERS.add(username);
      }
   }

   public static void removeUser(String username) {
      USERS.remove(username);
   }

   public static boolean hasUser(String username) {
      return USERS.contains(username);
   }

   public static boolean containsUser(String text) {
      if (text != null && !text.isEmpty()) {
         for (String username : USERS) {
            if (text.contains(username)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static void updateUsers(String userList) {
      USERS.clear();
      if (userList != null && !userList.isEmpty()) {
         String[] users = userList.split(",");

         for (String user : users) {
            String trimmed = user.trim();
            if (!trimmed.isEmpty()) {
               USERS.add(trimmed);
            }
         }
      }
   }

   public static void clear() {
      USERS.clear();
   }

   public static int size() {
      return USERS.size();
   }

   public static Set<String> getAllUsers() {
      return Set.copyOf(USERS);
   }
}
