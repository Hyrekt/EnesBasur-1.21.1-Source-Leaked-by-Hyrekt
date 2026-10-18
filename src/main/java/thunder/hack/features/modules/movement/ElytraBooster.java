package thunder.hack.features.modules.movement;

import thunder.hack.features.modules.Module;

public class ElytraBooster extends Module {
   private static ElytraBooster INSTANCE = new ElytraBooster();

   public ElytraBooster() {
      super("ElytraBooster", Module.Category.MOVEMENT);
      INSTANCE = this;
   }

   public static ElytraBooster getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ElytraBooster();
      }

      return INSTANCE;
   }
}
