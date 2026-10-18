package thunder.hack.utility.discord;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface DiscordRPC extends Library {
   void Discord_UpdateHandlers(DiscordEventHandlers var1);

   void Discord_UpdatePresence(DiscordRichPresence var1);

   void Discord_Respond(String var1, int var2);

   void Discord_Register(String var1, String var2);

   void Discord_Shutdown();

   void Discord_UpdateConnection();

   void Discord_RegisterSteamGame(String var1, String var2);

   void Discord_RunCallbacks();

   void Discord_Initialize(String var1, DiscordEventHandlers var2, boolean var3, String var4);

   void Discord_ClearPresence();

   public static class Holder {
      public static final DiscordRPC INSTANCE = load();

      private static DiscordRPC load() {
         try {
            return (DiscordRPC)Native.load("discord-rpc", DiscordRPC.class);
         } catch (Throwable var1) {
            System.err.println("Discord RPC not found: " + var1.getMessage());
            return null;
         }
      }
   }
}
