package thunder.hack.features.modules.combat;

import java.awt.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.PendingUpdateManager;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.player.ElytraHelper;
import thunder.hack.injection.accesors.IClientWorldMixin;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render3DEngine;

public class ElytraTarget extends Module {
   public final Setting<Boolean> drawBox = new Setting<>("Draw Box", true);
   public final Setting<ColorSetting> boxColor = new Setting<>("Box Color", new ColorSetting(new Color(0, 0, 255, 120).getRGB()));
   public final Setting<Float> distanceInFront = new Setting<>("DistanceInFront", 2.5F, 0.0F, 20.0F);
   public final Setting<Float> distance = new Setting<>("Distance", 100.0F, 10.0F, 400.0F);
   public final Setting<Boolean> autoFirework = new Setting<>("Auto Firework", true);
   public final Setting<Integer> fireworkDelay = new Setting<>("Firework Delay", 1000, 200, 5000, v -> this.autoFirework.getValue());
   public static PlayerEntity target = null;
   public static Vec3d predictedPos = null;
   private World lastWorld;
   private long lastFireworkTime = 0L;

   public ElytraTarget() {
      super("ElytraTarget", Module.Category.COMBAT);
   }

   @EventHandler
   public void onTick(EventTick event) {
      if (mc.player != null && mc.world != null) {
         if (mc.world != this.lastWorld) {
            target = null;
            this.lastWorld = mc.world;
         }

         target = this.findTarget();
         if (target != null) {
            predictedPos = this.getFrontPosition(target, (double)this.distanceInFront.getValue().floatValue());
            this.useFireworkIfNeeded();
         } else {
            predictedPos = null;
         }
      } else {
         target = null;
         predictedPos = null;
      }
   }

   private PlayerEntity findTarget() {
      PlayerEntity bestTarget = null;
      float closestDistance = this.distance.getValue();

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player != mc.player && player.isAlive() && !player.isRemoved() && !(player.getHealth() <= 0.0F) && !Managers.FRIEND.isFriend(player)) {
            boolean isFlying = player.isFallFlying() || player.getInventory().getArmorStack(2).isOf(Items.ELYTRA) && !player.isOnGround();
            if (isFlying) {
               float d = mc.player.distanceTo(player);
               if (d < closestDistance) {
                  bestTarget = player;
                  closestDistance = d;
               }
            }
         }
      }

      return bestTarget;
   }

   private boolean skipEntity(Entity entity) {
      return !(entity instanceof PlayerEntity player)
         ? true
         : player.isRemoved()
            || !player.isAlive()
            || player.getHealth() <= 0.0F
            || !player.isFallFlying()
            || Managers.FRIEND.isFriend(player)
            || mc.player.distanceTo(player) > this.distance.getValue();
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (target != null && predictedPos != null && this.drawBox.getValue()) {
         Box targetBox = target.getBoundingBox();
         double w = (targetBox.maxX - targetBox.minX) / 2.0;
         double h = targetBox.maxY - targetBox.minY;
         Box box = new Box(
            predictedPos.x - w,
            predictedPos.y,
            predictedPos.z - w,
            predictedPos.x + w,
            predictedPos.y + h,
            predictedPos.z + w
         );
         Color color = this.boxColor.getValue().getColorObject();
         Render3DEngine.drawFilledBox(stack, box, color);
         Render3DEngine.drawBoxOutline(box, color, 2.0F);
      }
   }

   private Vec3d getFrontPosition(Entity target, double distanceInFront) {
      Vec3d targetPos = target.getPos();
      Vec3d lookVec = target.getRotationVector();
      Vec3d horizontalLookVec = new Vec3d(lookVec.x, 0.0, lookVec.z).normalize();
      Vec3d frontPos = targetPos.add(horizontalLookVec.multiply(distanceInFront));
      return new Vec3d(frontPos.x, targetPos.y + 1.2, frontPos.z);
   }

   private void useFireworkIfNeeded() {
      if (mc.player != null && mc.world != null && this.autoFirework.getValue()) {
         if (mc.player.isFallFlying()) {
            if (System.currentTimeMillis() - this.lastFireworkTime >= (long)this.fireworkDelay.getValue().intValue()) {
               int fireworkSlot = -1;

               for (int i = 0; i < 9; i++) {
                  if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                     fireworkSlot = i;
                     break;
                  }
               }

               if (fireworkSlot != -1) {
                  String fireworkMode = ElytraHelper.getInstance().getFireWorkMode();
                  boolean shouldSwing = ElytraHelper.getInstance().isSwingEnabled();
                  int oldSlot = mc.player.getInventory().selectedSlot;
                  if (fireworkMode.equalsIgnoreCase("Legit")) {
                     mc.player.getInventory().selectedSlot = fireworkSlot;
                     mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                     if (shouldSwing) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                     }
                  } else if (fireworkMode.equalsIgnoreCase("Silent")) {
                     mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(fireworkSlot));
                     PendingUpdateManager pendingUpdateManager = ((IClientWorldMixin)mc.world).getPendingUpdateManager().incrementSequence();

                     try {
                        int sequence = pendingUpdateManager.getSequence();
                        mc.player
                           .networkHandler
                           .sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch()));
                     } catch (Throwable var9) {
                        if (pendingUpdateManager != null) {
                           try {
                              pendingUpdateManager.close();
                           } catch (Throwable var8) {
                              var9.addSuppressed(var8);
                           }
                        }

                        throw var9;
                     }

                     if (pendingUpdateManager != null) {
                        pendingUpdateManager.close();
                     }

                     if (shouldSwing) {
                        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                     }

                     mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
                  }

                  this.lastFireworkTime = System.currentTimeMillis();
               }
            }
         }
      }
   }
}
