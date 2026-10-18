package thunder.hack.utility;

import java.lang.reflect.Method;
import thunder.hack.ThunderHack;

public class BaritoneUtility {
   public static void executeInternal(String command) {
      if (ThunderHack.baritone) {
         try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Method getProvider = apiClass.getMethod("getProvider");
            Object provider = getProvider.invoke(null);
            Method getPrimaryBaritone = provider.getClass().getMethod("getPrimaryBaritone");
            Object baritone = getPrimaryBaritone.invoke(provider);
            Method getCommandManager = baritone.getClass().getMethod("getCommandManager");
            Object commandManager = getCommandManager.invoke(baritone);
            Method execute = commandManager.getClass().getMethod("execute", String.class);
            execute.invoke(commandManager, command);
         } catch (Exception var9) {
            var9.printStackTrace();
         }
      }
   }
}
