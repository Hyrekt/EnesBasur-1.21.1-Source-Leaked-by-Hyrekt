package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Formatting;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class Flight extends Module {
   private final Setting<Flight.Mode> mode = new Setting<>("Mode", Flight.Mode.Vanilla);
   private final Setting<Float> hSpeed = new Setting<>("Horizontal", 1.0F, 0.0F, 10.0F, v -> !this.mode.is(Flight.Mode.StormBreak));
   private final Setting<Float> vSpeed = new Setting<>("Vertical", 0.78F, 0.0F, 5.0F, v -> !this.mode.is(Flight.Mode.StormBreak));
   private final Setting<Float> boostValue = new Setting<>("Boost", 1.0F, 0.1F, 1.0F, v -> this.mode.is(Flight.Mode.StormBreak));
   private final Setting<Boolean> autoToggle = new Setting<>("AutoToggle", false, v -> this.mode.is(Flight.Mode.MatrixJump));
   private final Setting<Integer> boostTicks = new Setting<>("Ticks", 8, 0, 40, v -> this.mode.is(Flight.Mode.Damage));
   private final Setting<Boolean> antiKick = new Setting<>("AntiKick", false, v -> this.mode.is(Flight.Mode.Creative) || this.mode.is(Flight.Mode.Vanilla));
   private double prevX;
   private double prevY;
   private double prevZ;
   private double velocityMotion;
   public boolean onPosLook = false;
   private int flyTicks = 0;

   public Flight() {
      super("Flight", Module.Category.MOVEMENT);
   }

   @EventHandler
   public void onEventSync(EventSync event) {
      switch ((Flight.Mode)this.mode.getValue()) {
         case Vanilla:
            if (MovementUtility.isMoving()) {
               double[] dir = MovementUtility.forward((double)this.hSpeed.getValue().floatValue());
               mc.player.setVelocity(dir[0], 0.0, dir[1]);
            } else {
               mc.player.setVelocity(0.0, 0.0, 0.0);
            }

            if (mc.options.jumpKey.isPressed()) {
               mc.player.setVelocity(mc.player.getVelocity().add(0.0, (double)this.vSpeed.getValue().floatValue(), 0.0));
            }

            if (mc.options.sneakKey.isPressed()) {
               mc.player.setVelocity(mc.player.getVelocity().add(0.0, (double)(-this.vSpeed.getValue()), 0.0));
            }
         case MatrixJump:
         default:
            break;
         case AirJump:
            if (MovementUtility.isMoving()
               && mc.world
                  .getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(0.5, 0.0, 0.5).offset(0.0, -1.0, 0.0))
                  .iterator()
                  .hasNext()) {
               mc.player.setOnGround(true);
               mc.player.jump();
            }
            break;
         case MatrixGlide:
            if (mc.player.isOnGround()) {
               mc.player.jump();
               this.flyTicks = 5;
            } else if (this.flyTicks > 0) {
               if (MovementUtility.isMoving()) {
                  double[] dir = MovementUtility.forward((double)this.hSpeed.getValue().floatValue());
                  mc.player.setVelocity(dir[0], -0.04, dir[1]);
               } else {
                  mc.player.setVelocity(0.0, -0.04, 0.0);
               }

               this.flyTicks--;
            }
            break;
         case StormBreak:
            if (mc.player.age % 60 == 0) {
               this.sendMessage(
                  Formatting.RED
                     + (
                        ClientSettings.isRu()
                           ? "\u0412 \u044d\u0442\u043e\u043c \u0440\u0435\u0436\u0438\u043c\u0435 \u043d\u0443\u0436\u043d\u043e \u043b\u043e\u043c\u0430\u0442\u044c \u0431\u043b\u043e\u043a\u0438!"
                           : "In this mode you need to break blocks!"
                     )
               );
            }
      }

      if (this.antiKick.getValue() && (this.mode.is(Flight.Mode.Creative) || this.mode.is(Flight.Mode.Vanilla))) {
         mc.player.setVelocity(mc.player.getVelocity().add(0.0, -0.08, 0.0));
      }
   }

   @Override
   public void onUpdate() {
      if (this.mode.is(Flight.Mode.Damage) && this.flyTicks-- > this.boostTicks.getValue()) {
         mc.player.setVelocity(mc.player.getVelocity().x, this.velocityMotion, mc.player.getVelocity().getZ());
      }

      if (this.mode.is(Flight.Mode.MatrixJump)) {
         if (mc.player.fallDistance == 0.0F) {
            return;
         }

         mc.player.getAbilities().flying = false;
         mc.player.setVelocity(0.0, 0.0, 0.0);
         if (mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().add(0.0, (double)this.vSpeed.getValue().floatValue(), 0.0));
         }

         if (mc.options.sneakKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().add(0.0, (double)(-this.vSpeed.getValue()), 0.0));
         }

         double[] dir = MovementUtility.forward((double)this.hSpeed.getValue().floatValue());
         mc.player.setVelocity(dir[0], mc.player.getVelocity().getY(), dir[1]);
      }

      if (this.mode.is(Flight.Mode.Creative)) {
         mc.player.getAbilities().flying = true;
         mc.player.getAbilities().setFlySpeed(this.hSpeed.getValue() / 10.0F);
      }
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (this.mode.is(Flight.Mode.MatrixJump)) {
         if (fullNullCheck()) {
            return;
         }

         if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.onPosLook = true;
            this.prevX = mc.player.getVelocity().getX();
            this.prevY = mc.player.getVelocity().getY();
            this.prevZ = mc.player.getVelocity().getZ();
         }
      }

      EntityVelocityUpdateS2CPacket v;
      Object t;
      if (this.mode.is(Flight.Mode.Damage) && (t = e.getPacket()) instanceof EntityVelocityUpdateS2CPacket && (v = (EntityVelocityUpdateS2CPacket)t).getVelocityY() / 8000.0 > 0.2) {
         this.velocityMotion = v.getVelocityY() / 8000.0;
         this.flyTicks = this.boostTicks.getValue();
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (this.mode.is(Flight.Mode.MatrixJump) && e.getPacket() instanceof Full && this.onPosLook) {
         mc.player.setVelocity(this.prevX, this.prevY, this.prevZ);
         this.onPosLook = false;
         if (this.autoToggle.getValue()) {
            this.disable();
         }
      }

      PlayerActionC2SPacket pac;
      Object t;
      if (this.mode.is(Flight.Mode.StormBreak)
         && (t = e.getPacket()) instanceof PlayerActionC2SPacket
         && (
            (pac = (PlayerActionC2SPacket)t).getAction() == Action.STOP_DESTROY_BLOCK
               || pac.getAction() == Action.START_DESTROY_BLOCK && mc.world.getBlockState(pac.getPos()).isReplaceable()
         )) {
         double[] dir = MovementUtility.forward((double)(2.0F * this.boostValue.getValue()));
         mc.player.setVelocity(dir[0], (double)(3.0F * this.boostValue.getValue()), dir[1]);
      }
   }

   @Override
   public void onDisable() {
      mc.player.getAbilities().flying = false;
      mc.player.getAbilities().setFlySpeed(0.05F);
   }

   private static enum Mode {
      Vanilla,
      MatrixJump,
      AirJump,
      MatrixGlide,
      StormBreak,
      Damage,
      Creative;
   }
}
