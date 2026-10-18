package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;

public class TpsSync extends Module {
   public TpsSync() {
      super("TpsSync", Module.Category.PLAYER);
   }

   @EventHandler(
      priority = 100
   )
   public void onTick(EventTick e) {
      ThunderHack.TICK_TIMER = Managers.SERVER.getTPS() > 1.0F ? Managers.SERVER.getTPS() / 20.0F : 1.0F;
   }

   @Override
   public void onDisable() {
      ThunderHack.TICK_TIMER = 1.0F;
   }
}
