package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.CobwebBlock;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventCollision;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class AntiWeb extends Module {
   public static final Setting<AntiWeb.Mode> mode = new Setting<>("Mode", AntiWeb.Mode.Solid);
   public static final Setting<Boolean> grim = new Setting<>("Grim", false, v -> mode.is(AntiWeb.Mode.Ignore));
   public static final Setting<Float> timer = new Setting<>("Timer", 20.0F, 1.0F, 50.0F, v -> mode.getValue() == AntiWeb.Mode.Timer);
   public Setting<Float> speed = new Setting<>("Speed", 0.3F, 0.0F, 10.0F, v -> mode.getValue() == AntiWeb.Mode.Fly);
   private boolean timerEnabled = false;

   public AntiWeb() {
      super("AntiWeb", Module.Category.MOVEMENT);
   }

   @EventHandler
   public void onPlayerUpdate(PlayerUpdateEvent e) {
      if (Managers.PLAYER.isInWeb()) {
         if (mode.getValue() == AntiWeb.Mode.Timer) {
            if (mc.player.isOnGround()) {
               ThunderHack.TICK_TIMER = 1.0F;
            } else {
               ThunderHack.TICK_TIMER = timer.getValue();
               this.timerEnabled = true;
            }
         }

         if (mode.getValue() == AntiWeb.Mode.Fly) {
            double[] dir = MovementUtility.forward((double)this.speed.getValue().floatValue());
            mc.player.setVelocity(dir[0], 0.0, dir[1]);
            if (mc.options.jumpKey.isPressed()) {
               mc.player.setVelocity(mc.player.getVelocity().add(0.0, (double)this.speed.getValue().floatValue(), 0.0));
            }

            if (mc.options.sneakKey.isPressed()) {
               mc.player.setVelocity(mc.player.getVelocity().add(0.0, (double)(-this.speed.getValue()), 0.0));
            }
         }
      } else if (this.timerEnabled) {
         this.timerEnabled = false;
         ThunderHack.TICK_TIMER = 1.0F;
      }
   }

   @EventHandler
   public void onCollide(EventCollision e) {
      if (e.getState().getBlock() instanceof CobwebBlock && mode.getValue() == AntiWeb.Mode.Solid) {
         e.setState(Blocks.DIRT.getDefaultState());
      }
   }

   public static enum Mode {
      Timer,
      Solid,
      Ignore,
      Fly;
   }
}
