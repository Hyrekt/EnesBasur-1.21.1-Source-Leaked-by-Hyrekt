package thunder.hack.features.modules.player;

import thunder.hack.features.modules.Module;
import thunder.hack.injection.accesors.ILivingEntity;
import thunder.hack.setting.Setting;

public class NoJumpDelay extends Module {
   private final Setting<Integer> delay = new Setting<>("Delay", 1, 0, 4);

   public NoJumpDelay() {
      super("NoJumpDelay", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (((ILivingEntity)mc.player).getLastJumpCooldown() > this.delay.getValue()) {
         ((ILivingEntity)mc.player).setLastJumpCooldown(this.delay.getValue());
      }
   }
}
