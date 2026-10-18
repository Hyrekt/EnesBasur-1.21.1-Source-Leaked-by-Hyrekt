package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import thunder.hack.core.Core;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventMove;
import thunder.hack.events.impl.EventSprint;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.injection.accesors.ISPacketEntityVelocity;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.MovementUtility;

public class TargetStrafe extends Module {
   public Setting<Boolean> jump = new Setting<>("Jump", true);
   public Setting<Float> distance = new Setting<>("Distance", 1.3F, 0.2F, 7.0F);
   private final Setting<TargetStrafe.Boost> boost = new Setting<>("Boost", TargetStrafe.Boost.None);
   public Setting<Float> setSpeed = new Setting<>("speed", 1.3F, 0.0F, 2.0F, v -> this.boost.getValue() == TargetStrafe.Boost.Elytra);
   private final Setting<Float> velReduction = new Setting<>("Reduction", 6.0F, 0.1F, 10.0F, v -> this.boost.getValue() == TargetStrafe.Boost.Damage);
   private final Setting<Float> maxVelocitySpeed = new Setting<>("MaxVelocity", 0.8F, 0.1F, 2.0F, v -> this.boost.getValue() == TargetStrafe.Boost.Damage);
   public static double oldSpeed;
   public static double contextFriction;
   public static double fovval;
   public static boolean needSwap;
   public static boolean needSprintState;
   public static boolean skip;
   public static boolean switchDir;
   public static boolean disabled;
   public static int noSlowTicks;
   public static int jumpTicks;
   public static int waterTicks;
   static long disableTime;
   private static TargetStrafe instance;

   public TargetStrafe() {
      super("TargetStrafe", Module.Category.COMBAT);
      instance = this;
   }

   public static TargetStrafe getInstance() {
      return instance;
   }

   @Override
   public void onEnable() {
      oldSpeed = 0.0;
      fovval = (Double)mc.options.getFovEffectScale().getValue();
      mc.options.getFovEffectScale().setValue(0.0);
      skip = true;
   }

   public boolean canStrafe() {
      if (mc.player.isSneaking()) {
         return false;
      } else if (mc.player.isInLava()) {
         return false;
      } else if (ModuleManager.speed.isEnabled()) {
         return false;
      } else {
         return !mc.player.isSubmergedInWater() && waterTicks <= 0 ? !mc.player.getAbilities().flying : false;
      }
   }

   public boolean needToSwitch(double x, double z) {
      if (!mc.player.horizontalCollision && (!mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed() || jumpTicks > 0)) {
         for (int i = (int)(mc.player.getY() + 4.0); i >= 0; i--) {
            BlockPos playerPos = new BlockPos((int)Math.floor(x), (int)Math.floor((double)i), (int)Math.floor(z));
            if (mc.world.getBlockState(playerPos).getBlock().equals(Blocks.LAVA)
               || mc.world.getBlockState(playerPos).getBlock().equals(Blocks.FIRE)) {
               return true;
            }

            if (!mc.world.isAir(playerPos)) {
               return false;
            }
         }

         return false;
      } else {
         jumpTicks = 10;
         return true;
      }
   }

   @Override
   public void onDisable() {
      mc.options.getFovEffectScale().setValue(fovval);
   }

   public double calculateSpeed(EventMove move) {
      jumpTicks--;
      float speedAttributes = this.getAIMoveSpeed();
      float frictionFactor = mc.world
            .getBlockState(
               new Mutable()
                  .set(
                     mc.player.getX(), this.getBoundingBox().getMin(Axis.Y) - move.getY(), mc.player.getZ()
                  )
            )
            .getBlock()
            .getSlipperiness()
         * 0.91F;
      float n6 = (float)(oldSpeed > 0.32 && mc.player.isUsingItem() ? 0.88 : 0.91F);
      if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) && mc.player.isUsingItem()) {
         float var10000 = 0.88F;
      }

      if (mc.player.isOnGround()) {
         n6 = frictionFactor;
      }

      float n7 = (float)(0.1631F / Math.pow((double)n6, 3.0));
      float n8;
      if (mc.player.isOnGround()) {
         n8 = speedAttributes * n7;
         if (move.getY() > 0.0) {
            n8 += this.boost.getValue() == TargetStrafe.Boost.Elytra && InventoryUtility.getElytra() != -1 && disabled ? 0.65F : 0.2F;
         }

         disabled = false;
      } else {
         n8 = 0.0255F;
      }

      boolean noslow = false;
      double max2 = oldSpeed + (double)n8;
      double max = 0.0;
      if (mc.player.isUsingItem() && move.getY() <= 0.0) {
         double d = 0.2873;
         double n10 = oldSpeed + (double)n8 * 0.25;
         double motionY2 = move.getY();
         if (motionY2 != 0.0 && Math.abs(motionY2) < 0.08) {
            n10 += 0.055;
         }

         max = Math.max(0.043, n10);
         if (max2 > d) {
            noslow = true;
            noSlowTicks++;
         } else {
            noSlowTicks = Math.max(noSlowTicks - 1, 0);
         }
      } else {
         noSlowTicks = 0;
      }

      max2 = noSlowTicks > 3 ? max - 0.019 : Math.max(noslow ? 0.0 : 0.25, max2) - (mc.player.age % 2 == 0 ? 0.001 : 0.002);
      contextFriction = (double)n6;
      if (!mc.player.isOnGround()) {
         needSprintState = !((IClientPlayerEntity)mc.player).getLastSprinting();
         needSwap = true;
      } else {
         needSprintState = false;
      }

      return max2;
   }

   public Box getBoundingBox() {
      return new Box(
         mc.player.getX() - 0.1,
         mc.player.getY(),
         mc.player.getZ() - 0.1,
         mc.player.getX() + 0.1,
         mc.player.getY() + 1.0,
         mc.player.getZ() + 0.1
      );
   }

   public float getAIMoveSpeed() {
      boolean prevSprinting = mc.player.isSprinting();
      mc.player.setSprinting(false);
      float speed = mc.player.getMovementSpeed() * 1.3F;
      mc.player.setSprinting(prevSprinting);
      return speed;
   }

   public static void disabler(int elytra) {
      if (elytra != -1) {
         if (System.currentTimeMillis() - disableTime > 190L) {
            if (elytra != -2) {
               mc.interactionManager.clickSlot(0, elytra, 1, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(0, 6, 1, SlotActionType.PICKUP, mc.player);
            }

            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
            if (elytra != -2) {
               mc.interactionManager.clickSlot(0, 6, 1, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(0, elytra, 1, SlotActionType.PICKUP, mc.player);
            }

            disableTime = System.currentTimeMillis();
         }

         disabled = true;
      }
   }

   private double wrapDS(double x, double z) {
      double diffX = x - mc.player.getX();
      double diffZ = z - mc.player.getZ();
      return Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;
   }

   @EventHandler
   public void onMove(EventMove event) {
      int elytraSlot = InventoryUtility.getElytra();
      if (this.boost.getValue() == TargetStrafe.Boost.Elytra
         && elytraSlot != -1
         && MovementUtility.isMoving()
         && !mc.player.isOnGround()
         && mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, event.getY(), 0.0)).iterator().hasNext()
         && disabled) {
         oldSpeed = (double)this.setSpeed.getValue().floatValue();
      }

      if (this.canStrafe()) {
         if (Aura.target != null && ModuleManager.aura.isEnabled()) {
            double speed = this.calculateSpeed(event);
            double wrap = Math.atan2(mc.player.getZ() - Aura.target.getZ(), mc.player.getX() - Aura.target.getX());
            double var11;
            double x = Aura.target.getX()
               + (double)this.distance.getValue().floatValue()
                  * Math.cos(
                     var11 = wrap
                        + (switchDir ? speed / Math.sqrt(mc.player.squaredDistanceTo(Aura.target)) : -(speed / Math.sqrt(mc.player.squaredDistanceTo(Aura.target))))
                  );
            double z;
            if (this.needToSwitch(x, z = Aura.target.getZ() + (double)this.distance.getValue().floatValue() * Math.sin(var11))) {
               switchDir = !switchDir;
               x = Aura.target.getX()
                  + (double)this.distance.getValue().floatValue()
                     * Math.cos(
                        wrap = var11
                           + 2.0
                              * (
                                 switchDir
                                    ? speed / Math.sqrt(mc.player.squaredDistanceTo(Aura.target))
                                    : -(speed / Math.sqrt(mc.player.squaredDistanceTo(Aura.target)))
                              )
                     );
               z = Aura.target.getZ() + (double)this.distance.getValue().floatValue() * Math.sin(wrap);
            }

            event.setX(speed * -Math.sin(Math.toRadians(this.wrapDS(x, z))));
            event.setZ(speed * Math.cos(Math.toRadians(this.wrapDS(x, z))));
            event.cancel();
         }
      } else {
         oldSpeed = 0.0;
      }
   }

   @EventHandler
   public void updateValues(EventSync e) {
      oldSpeed = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ) * contextFriction;
      if (mc.player.isOnGround() && this.jump.getValue() && Aura.target != null) {
         mc.player.jump();
      }

      waterTicks = mc.player.isSubmergedInWater() ? 10 : --waterTicks;
   }

   @EventHandler
   public void onUpdate(PlayerUpdateEvent event) {
      if (this.boost.getValue() == TargetStrafe.Boost.Elytra
         && InventoryUtility.getElytra() != -1
         && !mc.player.isOnGround()
         && mc.player.fallDistance > 0.0F
         && !disabled) {
         disabler(InventoryUtility.getElytra());
      }
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
         oldSpeed = 0.0;
      }

      EntityVelocityUpdateS2CPacket velocity;
      if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket
         && (velocity = e.getPacket()).getEntityId() == mc.player.getId()
         && this.boost.getValue() == TargetStrafe.Boost.Damage) {
         if (mc.player.isOnGround()) {
            return;
         }

         double vX = velocity.getVelocityX();
         double vZ = velocity.getVelocityZ();
         if (vX < 0.0) {
            vX *= -1.0;
         }

         if (vZ < 0.0) {
            vZ *= -1.0;
         }

         oldSpeed = (vX + vZ) / (double)(this.velReduction.getValue() * 1000.0F);
         oldSpeed = Math.min(oldSpeed, (double)this.maxVelocitySpeed.getValue().floatValue());
         ((ISPacketEntityVelocity)velocity).setMotionX(0);
         ((ISPacketEntityVelocity)velocity).setMotionY(0);
         ((ISPacketEntityVelocity)velocity).setMotionZ(0);
      }
   }

   @EventHandler
   public void actionEvent(EventSprint eventAction) {
      if (this.canStrafe() && Core.serverSprint != needSprintState) {
         eventAction.setSprintState(!Core.serverSprint);
      }

      if (needSwap) {
         eventAction.setSprintState(!((IClientPlayerEntity)mc.player).getLastSprinting());
         needSwap = false;
      }
   }

   private static enum Boost {
      None,
      Elytra,
      Damage;
   }
}
