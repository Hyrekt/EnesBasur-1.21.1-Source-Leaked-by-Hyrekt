package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventMove;
import thunder.hack.events.impl.EventPlayerTravel;
import thunder.hack.events.impl.EventPostTick;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.EventTick;
import thunder.hack.events.impl.PostPlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.injection.accesors.IInteractionManager;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.interfaces.IEntity;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.MovementUtility;
import thunder.hack.utility.player.SearchInvResult;

public class Speed extends Module {
   public final Setting<Speed.Mode> mode = new Setting<>("Mode", Speed.Mode.NCP);
   public Setting<Boolean> useTimer = new Setting<>("Use Timer", false);
   public Setting<Boolean> pauseInLiquids = new Setting<>("PauseInLiquids", false);
   public Setting<Boolean> pauseWhileSneaking = new Setting<>("PauseWhileSneaking", false);
   public final Setting<Integer> hurttime = new Setting<>("HurtTime", 0, 0, 10, v -> this.mode.is(Speed.Mode.MatrixDamage));
   public final Setting<Float> boostFactor = new Setting<>(
      "BoostFactor", 2.0F, 0.0F, 10.0F, v -> this.mode.is(Speed.Mode.MatrixDamage) || this.mode.is(Speed.Mode.Vanilla)
   );
   public final Setting<Boolean> allowOffGround = new Setting<>("AllowOffGround", true, v -> this.mode.is(Speed.Mode.MatrixDamage));
   public final Setting<Integer> shiftTicks = new Setting<>("ShiftTicks", 0, 0, 10, v -> this.mode.is(Speed.Mode.MatrixDamage));
   public final Setting<Integer> fireWorkSlot = new Setting<>("FireSlot", 1, 1, 9, v -> this.mode.getValue() == Speed.Mode.FireWork);
   public final Setting<Integer> delay = new Setting<>("Delay", 8, 1, 20, v -> this.mode.getValue() == Speed.Mode.FireWork);
   public final Setting<Boolean> strict = new Setting<>("Strict", false, v -> this.mode.is(Speed.Mode.GrimIce));
   public final Setting<Float> matrixJBSpeed = new Setting<>("TimerSpeed", 1.088F, 1.0F, 2.0F, v -> this.mode.is(Speed.Mode.MatrixJB));
   public final Setting<Boolean> armorStands = new Setting<>(
      "ArmorStands", false, v -> this.mode.is(Speed.Mode.GrimCombo) || this.mode.is(Speed.Mode.GrimEntity2)
   );
   public double baseSpeed;
   private int stage;
   private int ticks;
   private int prevSlot;
   private float prevForward = 0.0F;
   private Timer elytraDelay = new Timer();
   private Timer startDelay = new Timer();

   public Speed() {
      super("Speed", Module.Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      ThunderHack.TICK_TIMER = 1.0F;
   }

   @Override
   public void onEnable() {
      this.stage = 1;
      this.ticks = 0;
      this.baseSpeed = 0.2873;
      this.startDelay.reset();
      this.prevSlot = -1;
   }

   @EventHandler
   public void onSync(EventSync e) {
      if ((!mc.player.isInFluid() || !this.pauseInLiquids.getValue()) && (!mc.player.isSneaking() || !this.pauseWhileSneaking.getValue())) {
         if (this.mode.getValue() == Speed.Mode.MatrixJB) {
            boolean closeToGround = false;

            for (VoxelShape a : mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(0.5, 0.0, 0.5).offset(0.0, -1.0, 0.0))) {
               if (a != VoxelShapes.empty()) {
                  closeToGround = true;
                  break;
               }
            }

            if (MovementUtility.isMoving() && closeToGround && mc.player.fallDistance <= 0.0F) {
               ThunderHack.TICK_TIMER = 1.0F;
               mc.player.setOnGround(true);
               mc.player.jump();
            } else if (mc.player.fallDistance > 0.0F && this.useTimer.getValue()) {
               ThunderHack.TICK_TIMER = this.matrixJBSpeed.getValue();
               mc.player.addVelocity(0.0, -0.003F, 0.0);
            }
         }
      }
   }

   @EventHandler
   public void modifyVelocity(EventPlayerTravel e) {
      if (this.mode.getValue() == Speed.Mode.GrimEntity && !e.isPre() && ThunderHack.core.getSetBackTime() > 1000L) {
         for (PlayerEntity class_16572 : Managers.ASYNC.getAsyncPlayers()) {
            if (class_16572 != mc.player && mc.player.squaredDistanceTo(class_16572) <= 2.25) {
               float p = mc.world.getBlockState(((IEntity)mc.player).thunderHack_Recode$getVelocityBP()).getBlock().getSlipperiness();
               float f = mc.player.isOnGround() ? p * 0.91F : 0.91F;
               float f2 = mc.player.isOnGround() ? p : 0.99F;
               mc.player
                  .setVelocity(
                     mc.player.getVelocity().getX() / (double)f * (double)f2,
                     mc.player.getVelocity().getY(),
                     mc.player.getVelocity().getZ() / (double)f * (double)f2
                  );
               break;
            }
         }
      }

      if ((this.mode.is(Speed.Mode.GrimEntity2) || this.mode.is(Speed.Mode.GrimCombo))
         && !e.isPre()
         && ThunderHack.core.getSetBackTime() > 1000L
         && MovementUtility.isMoving()) {
         int collisions = 0;

         for (Entity ent : mc.world.getEntities()) {
            if (ent != mc.player
               && (!(ent instanceof ArmorStandEntity) || this.armorStands.getValue())
               && (ent instanceof LivingEntity || ent instanceof BoatEntity)
               && mc.player.getBoundingBox().expand(1.0).intersects(ent.getBoundingBox())) {
               collisions++;
            }
         }

         double[] dArray = MovementUtility.forward(0.08 * (double)collisions);
         mc.player.addVelocity(dArray[0], 0.0, dArray[1]);
      }
   }

   @EventHandler
   public void onTick(EventTick e) {
      if ((this.mode.is(Speed.Mode.GrimIce) || this.mode.is(Speed.Mode.GrimCombo)) && mc.player.isOnGround()) {
         BlockPos pos = ((IEntity)mc.player).thunderHack_Recode$getVelocityBP();
         SearchInvResult result = InventoryUtility.findBlockInHotBar(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);
         if (mc.world.isAir(pos) || !result.found() || !mc.options.jumpKey.isPressed()) {
            return;
         }

         this.prevSlot = mc.player.getInventory().selectedSlot;
         result.switchTo();
         this.sendPacket(
            new Full(
               mc.player.getX(),
               mc.player.getY(),
               mc.player.getZ(),
               mc.player.getYaw(),
               90.0F,
               mc.player.isOnGround()
            )
         );
         if (this.strict.getValue()) {
            this.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, Direction.UP));
            this.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, pos, Direction.UP));
         }

         this.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
         this.sendSequencedPacket(
            id -> new PlayerInteractBlockC2SPacket(
                  Hand.MAIN_HAND,
                  new BlockHitResult(pos.down().toCenterPos().add(0.0, 0.5, 0.0), Direction.UP, pos.down(), false),
                  id
               )
         );
         mc.world.setBlockState(pos, Blocks.ICE.getDefaultState());
      }
   }

   @EventHandler
   public void onPostTick(EventPostTick e) {
      if ((this.mode.is(Speed.Mode.GrimIce) || this.mode.is(Speed.Mode.GrimCombo)) && this.prevSlot != -1) {
         mc.player.getInventory().selectedSlot = this.prevSlot;
         ((IInteractionManager)mc.interactionManager).syncSlot();
         this.prevSlot = -1;
      }
   }

   @Override
   public void onUpdate() {
      if ((!mc.player.isInFluid() || !this.pauseInLiquids.getValue()) && (!mc.player.isSneaking() || !this.pauseWhileSneaking.getValue())) {
         if (this.mode.getValue() == Speed.Mode.FireWork) {
            this.ticks--;
            int ellySlot = InventoryUtility.getElytra();
            int fireSlot = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET).slot();
            boolean inOffHand = mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET;
            int fireInInv;
            if (fireSlot == -1 && (fireInInv = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET).slot()) != -1) {
               mc.interactionManager
                  .clickSlot(mc.player.currentScreenHandler.syncId, fireInInv, this.fireWorkSlot.getValue() - 1, SlotActionType.SWAP, mc.player);
            }

            if (ellySlot != -1 && (fireSlot != -1 || inOffHand) && !mc.player.isOnGround() && mc.player.fallDistance > 0.0F && this.ticks <= 0) {
               if (ellySlot != -2) {
                  mc.interactionManager.clickSlot(0, ellySlot, 1, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(0, 6, 1, SlotActionType.PICKUP, mc.player);
               }

               mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               int prevSlot = mc.player.getInventory().selectedSlot;
               if (prevSlot != fireSlot && !inOffHand) {
                  this.sendPacket(new UpdateSelectedSlotC2SPacket(fireSlot));
               }

               mc.interactionManager.interactItem(mc.player, inOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
               if (prevSlot != fireSlot && !inOffHand) {
                  this.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
               }

               if (ellySlot != -2) {
                  mc.interactionManager.clickSlot(0, 6, 1, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(0, ellySlot, 1, SlotActionType.PICKUP, mc.player);
               }

               mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
               this.ticks = this.delay.getValue();
            }
         }

         if (this.mode.getValue() == Speed.Mode.ElytraLowHop) {
            if (mc.player.isOnGround()) {
               mc.player.jump();
               return;
            }

            if (mc.world
                  .getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.29, 0.0, -0.29).offset(0.0, -3.0, 0.0))
                  .iterator()
                  .hasNext()
               && this.elytraDelay.passedMs(150L)
               && this.startDelay.passedMs(500L)) {
               int elytra = InventoryUtility.getElytra();
               if (elytra == -1) {
                  this.disable(
                     ClientSettings.isRu()
                        ? "\u0414\u043b\u044f \u044d\u0442\u043e\u0433\u043e \u0440\u0435\u0436\u0438\u043c\u0430 \u043d\u0443\u0436\u043d\u0430 \u044d\u043b\u0438\u0442\u0440\u0430!"
                        : "You need elytra for this mode!"
                  );
               } else {
                  Strafe.disabler(elytra);
               }

               mc.player.setVelocity(mc.player.getVelocity().getX(), 0.0, mc.player.getVelocity().getZ());
               if (MovementUtility.isMoving()) {
                  MovementUtility.setMotion(0.85);
               }

               this.elytraDelay.reset();
            }
         }
      }
   }

   @EventHandler
   public void onPostPlayerUpdate(PostPlayerUpdateEvent event) {
      if (this.mode.getValue() == Speed.Mode.MatrixDamage && MovementUtility.isMoving() && mc.player.hurtTime > this.hurttime.getValue()) {
         if (mc.player.isOnGround()) {
            MovementUtility.setMotion((double)(0.387F * this.boostFactor.getValue()));
         } else if (mc.player.isTouchingWater()) {
            MovementUtility.setMotion((double)(0.346F * this.boostFactor.getValue()));
         } else if (!mc.player.isOnGround() && this.allowOffGround.getValue()) {
            MovementUtility.setMotion((double)(0.448F * this.boostFactor.getValue()));
         }

         if (this.shiftTicks.getValue() > 0) {
            event.cancel();
            event.setIterations(this.shiftTicks.getValue());
         }
      }
   }

   @EventHandler
   public void onMove(EventMove event) {
      if ((!mc.player.isInFluid() || !this.pauseInLiquids.getValue()) && (!mc.player.isSneaking() || !this.pauseWhileSneaking.getValue())) {
         if (this.mode.getValue() == Speed.Mode.NCP || this.mode.getValue() == Speed.Mode.StrictStrafe) {
            if (!mc.player.getAbilities().flying) {
               if (!mc.player.isFallFlying()) {
                  if (mc.player.getHungerManager().getFoodLevel() > 6) {
                     if (!event.isCancelled()) {
                        event.cancel();
                        if (MovementUtility.isMoving()) {
                           ThunderHack.TICK_TIMER = this.useTimer.getValue() ? 1.088F : 1.0F;
                           float currentSpeed = this.mode.getValue() == Speed.Mode.NCP
                                 && mc.player.input.movementForward <= 0.0F
                                 && this.prevForward > 0.0F
                              ? Managers.PLAYER.currentPlayerSpeed * 0.66F
                              : Managers.PLAYER.currentPlayerSpeed;
                           boolean canJump = !mc.player.horizontalCollision;
                           if (this.stage == 1 && mc.player.isOnGround() && canJump) {
                              mc.player
                                 .setVelocity(mc.player.getVelocity().x, MovementUtility.getJumpSpeed(), mc.player.getVelocity().z);
                              event.setY(MovementUtility.getJumpSpeed());
                              this.baseSpeed *= 2.149;
                              this.stage = 2;
                           } else if (this.stage == 2) {
                              this.baseSpeed = (double)currentSpeed - 0.66 * ((double)currentSpeed - MovementUtility.getBaseMoveSpeed());
                              this.stage = 3;
                           } else {
                              if (mc.world
                                    .getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().getY(), 0.0))
                                    .iterator()
                                    .hasNext()
                                 || mc.player.verticalCollision) {
                                 this.stage = 1;
                              }

                              this.baseSpeed = (double)currentSpeed - (double)currentSpeed / 159.0;
                           }

                           this.baseSpeed = Math.max(this.baseSpeed, MovementUtility.getBaseMoveSpeed());
                           double ncpSpeed = this.mode.getValue() != Speed.Mode.StrictStrafe && !(mc.player.input.movementForward < 1.0F) ? 0.576 : 0.465;
                           double ncpBypassSpeed = this.mode.getValue() != Speed.Mode.StrictStrafe && !(mc.player.input.movementForward < 1.0F)
                              ? 0.57
                              : 0.44;
                           if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                              double amplifier = (double)mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
                              ncpSpeed *= 1.0 + 0.2 * (amplifier + 1.0);
                              ncpBypassSpeed *= 1.0 + 0.2 * (amplifier + 1.0);
                           }

                           if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                              double amplifier = (double)mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
                              ncpSpeed /= 1.0 + 0.2 * (amplifier + 1.0);
                              ncpBypassSpeed /= 1.0 + 0.2 * (amplifier + 1.0);
                           }

                           this.baseSpeed = Math.min(this.baseSpeed, this.ticks > 25 ? ncpSpeed : ncpBypassSpeed);
                           if (this.ticks++ > 50) {
                              this.ticks = 0;
                           }

                           MovementUtility.modifyEventSpeed(event, this.baseSpeed);
                           this.prevForward = mc.player.input.movementForward;
                        } else {
                           ThunderHack.TICK_TIMER = 1.0F;
                           event.setX(0.0);
                           event.setZ(0.0);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static enum Mode {
      StrictStrafe,
      MatrixJB,
      NCP,
      ElytraLowHop,
      MatrixDamage,
      GrimEntity,
      GrimEntity2,
      FireWork,
      Vanilla,
      GrimIce,
      GrimCombo;
   }
}
