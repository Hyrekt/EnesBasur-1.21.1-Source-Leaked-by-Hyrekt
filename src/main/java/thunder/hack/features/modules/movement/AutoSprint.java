package thunder.hack.features.modules.movement;

import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;

public class AutoSprint extends Module {
   public static final Setting<Boolean> sprint = new Setting<>("KeepSprint", true);
   public static final Setting<Float> motion = new Setting<>("Motion", 1.0F, 0.0F, 1.0F, v -> sprint.getValue());
   private final Setting<Boolean> stopWhileUsing = new Setting<>("StopWhileUsing", false);
   private final Setting<Boolean> pauseWhileAura = new Setting<>("PauseWhileAura", false);

   public AutoSprint() {
      super("AutoSprint", Module.Category.MOVEMENT);
   }

   @Override
   public void onUpdate() {
      mc.player
         .setSprinting(
            mc.player.getHungerManager().getFoodLevel() > 6
               && !mc.player.horizontalCollision
               && mc.player.input.movementForward > 0.0F
               && !mc.player.isSneaking()
               && (!mc.player.isUsingItem() || !this.stopWhileUsing.getValue())
               && (!ModuleManager.aura.isEnabled() || Aura.target == null || !this.pauseWhileAura.getValue())
         );
   }
}
