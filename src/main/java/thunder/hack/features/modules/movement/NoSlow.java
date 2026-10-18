package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import thunder.hack.events.impl.EventKeyboardInput;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.SettingGroup;

public class NoSlow extends Module {
   public final Setting<NoSlow.Mode> mode = new Setting<>("Mode", NoSlow.Mode.NCP);
   private final Setting<Boolean> mainHand = new Setting<>("MainHand", true);
   private final Setting<SettingGroup> selection = new Setting<>("Selection", new SettingGroup(false, 0));
   private final Setting<Boolean> food = new Setting<>("Food", true).addToGroup(this.selection);
   private final Setting<Boolean> projectiles = new Setting<>("Projectiles", true).addToGroup(this.selection);
   private final Setting<Boolean> shield = new Setting<>("Shield", true).addToGroup(this.selection);
   public final Setting<Boolean> soulSand = new Setting<>("SoulSand", true).addToGroup(this.selection);
   public final Setting<Boolean> honey = new Setting<>("Honey", true).addToGroup(this.selection);
   public final Setting<Boolean> slime = new Setting<>("Slime", true).addToGroup(this.selection);
   public final Setting<Boolean> ice = new Setting<>("Ice", true).addToGroup(this.selection);
   public final Setting<Boolean> sweetBerryBush = new Setting<>("SweetBerryBush", true).addToGroup(this.selection);
   public final Setting<Boolean> sneak = new Setting<>("Sneak", false).addToGroup(this.selection);
   public final Setting<Boolean> crawl = new Setting<>("Crawl", false).addToGroup(this.selection);
   private boolean returnSneak;

   public NoSlow() {
      super("NoSlow", Module.Category.MOVEMENT);
   }

   @Override
   public void onUpdate() {
      if (this.returnSneak) {
         mc.options.sneakKey.setPressed(false);
         mc.player.setSprinting(true);
         this.returnSneak = false;
      }

      if (mc.player.isUsingItem() && !mc.player.isRiding() && !mc.player.isFallFlying()) {
         switch ((NoSlow.Mode)this.mode.getValue()) {
            case StrictNCP:
               this.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
               break;
            case Matrix:
               if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
                  mc.player
                     .setVelocity(
                        mc.player.getVelocity().x * 0.3, mc.player.getVelocity().y, mc.player.getVelocity().z * 0.3
                     );
               } else if (mc.player.fallDistance > 0.2F) {
                  mc.player
                     .setVelocity(
                        mc.player.getVelocity().x * 0.95F,
                        mc.player.getVelocity().y,
                        mc.player.getVelocity().z * 0.95F
                     );
               }
               break;
            case Grim:
               if (mc.player.getActiveHand() == Hand.OFF_HAND) {
                  this.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot % 8 + 1));
                  this.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot % 7 + 2));
                  this.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
               } else if (this.mainHand.getValue()) {
                  this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
               }
               break;
            case MusteryGrief:
               if (mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
                  mc.options.sneakKey.setPressed(true);
                  this.returnSneak = true;
               }
               break;
            case GrimNew:
               if (mc.player.getActiveHand() == Hand.OFF_HAND) {
                  this.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot % 8 + 1));
                  this.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot % 7 + 2));
                  this.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
               } else if (this.mainHand.getValue() && (mc.player.getItemUseTime() <= 3 || mc.player.age % 2 == 0)) {
                  this.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
               }
               break;
            case Matrix2:
               if (mc.player.isOnGround()) {
                  if (mc.player.age % 2 == 0) {
                     mc.player
                        .setVelocity(
                           mc.player.getVelocity().x * 0.5,
                           mc.player.getVelocity().y,
                           mc.player.getVelocity().z * 0.5
                        );
                  } else {
                     mc.player
                        .setVelocity(
                           mc.player.getVelocity().x * 0.95F,
                           mc.player.getVelocity().y,
                           mc.player.getVelocity().z * 0.95F
                        );
                  }
               }
               break;
            case LFCraft:
               if (mc.player.getItemUseTime() <= 3) {
                  this.sendSequencedPacket(
                     id -> new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, mc.player.getBlockPos().up(), Direction.NORTH, id)
                  );
               }
         }
      }
   }

   @EventHandler
   public void onKeyboardInput(EventKeyboardInput e) {
      if (this.mode.getValue() == NoSlow.Mode.Matrix3 && mc.player.isUsingItem() && !mc.player.isFallFlying()) {
         mc.player.input.movementForward *= 5.0F;
         mc.player.input.movementSideways *= 5.0F;
         float mult = 1.0F;
         if (mc.player.isOnGround()) {
            if (mc.player.input.movementForward != 0.0F && mc.player.input.movementSideways != 0.0F) {
               mc.player.input.movementForward *= 0.35F;
               mc.player.input.movementSideways *= 0.35F;
            } else {
               mc.player.input.movementForward *= 0.5F;
               mc.player.input.movementSideways *= 0.5F;
            }
         } else {
            mult = mc.player.input.movementForward != 0.0F && mc.player.input.movementSideways != 0.0F ? 0.47F : 0.67F;
         }

         mc.player.input.movementForward *= mult;
         mc.player.input.movementSideways *= mult;
      }
   }

   public boolean canNoSlow() {
      if (this.mode.getValue() == NoSlow.Mode.Matrix3) {
         return false;
      } else if (!this.food.getValue() && mc.player.getActiveItem().getComponents().contains(DataComponentTypes.FOOD)) {
         return false;
      } else if (!this.shield.getValue() && mc.player.getActiveItem().getItem() == Items.SHIELD) {
         return false;
      } else if (this.projectiles.getValue()
         || mc.player.getActiveItem().getItem() != Items.CROSSBOW
            && mc.player.getActiveItem().getItem() != Items.BOW
            && mc.player.getActiveItem().getItem() != Items.TRIDENT) {
         if (this.mode.getValue() == NoSlow.Mode.MusteryGrief && mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
            return false;
         } else {
            return !this.mainHand.getValue() && mc.player.getActiveHand() == Hand.MAIN_HAND
               ? false
               : !mc.player.getOffHandStack().getComponents().contains(DataComponentTypes.FOOD)
                     && mc.player.getOffHandStack().getItem() != Items.SHIELD
                  || this.mode.getValue() != NoSlow.Mode.GrimNew && this.mode.getValue() != NoSlow.Mode.Grim
                  || mc.player.getActiveHand() != Hand.MAIN_HAND;
         }
      } else {
         return false;
      }
   }

   public static enum Mode {
      NCP,
      StrictNCP,
      Matrix,
      Grim,
      MusteryGrief,
      GrimNew,
      Matrix2,
      LFCraft,
      Matrix3;
   }
}
