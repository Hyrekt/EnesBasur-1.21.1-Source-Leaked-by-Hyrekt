package thunder.hack.features.modules.movement;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import thunder.hack.events.impl.EventClickSlot;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class GuiMove extends Module {
   private final Setting<GuiMove.Bypass> clickBypass = new Setting<>("Bypass", GuiMove.Bypass.None);
   private final Setting<Boolean> rotateOnArrows = new Setting<>("RotateOnArrows", true);
   private final Setting<Boolean> sneak = new Setting<>("sneak", false);
   private final Queue<ClickSlotC2SPacket> storedClicks = new LinkedList<>();
   private AtomicBoolean pause = new AtomicBoolean();

   public GuiMove() {
      super("GuiMove", Module.Category.MOVEMENT);
   }

   @Override
   public void onUpdate() {
      if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen)) {
         for (KeyBinding k : new KeyBinding[]{
            mc.options.forwardKey,
            mc.options.backKey,
            mc.options.leftKey,
            mc.options.rightKey,
            mc.options.jumpKey,
            mc.options.sprintKey
         }) {
            k.setPressed(this.isKeyPressed(InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey()).getCode()));
         }

         float deltaX = 0.0F;
         float deltaY = 0.0F;
         if (this.rotateOnArrows.getValue()) {
            if (this.isKeyPressed(264)) {
               deltaY += 30.0F;
            }

            if (this.isKeyPressed(265)) {
               deltaY -= 30.0F;
            }

            if (this.isKeyPressed(262)) {
               deltaX += 30.0F;
            }

            if (this.isKeyPressed(263)) {
               deltaX -= 30.0F;
            }

            if (deltaX != 0.0F || deltaY != 0.0F) {
               mc.player.changeLookDirection((double)deltaX, (double)deltaY);
            }
         }

         if (this.sneak.getValue()) {
            mc.options.sneakKey.setPressed(this.isKeyPressed(InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode()));
         }
      }
   }

   @EventHandler
   public void onClickSlot(EventClickSlot e) {
      if (this.clickBypass.is(GuiMove.Bypass.DisableClicks) && (MovementUtility.isMoving() || mc.options.jumpKey.isPressed())) {
         e.cancel();
      }
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send e) {
      if (MovementUtility.isMoving() && mc.options.jumpKey.isPressed() && !this.pause.get()) {
         if (e.getPacket() instanceof ClickSlotC2SPacket click) {
            switch ((GuiMove.Bypass)this.clickBypass.getValue()) {
               case StrictNCP:
                  if (mc.player.isOnGround()
                     && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, 0.0656, 0.0)).iterator().hasNext()) {
                     if (mc.player.isSprinting()) {
                        this.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                     }

                     this.sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0656, mc.player.getZ(), false));
                  }
                  break;
               case GrimSwap:
                  if (click.getActionType() != SlotActionType.PICKUP && click.getActionType() != SlotActionType.PICKUP_ALL) {
                     this.sendPacket(new CloseHandledScreenC2SPacket(0));
                  }
                  break;
               case MatrixNcp:
                  this.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                  mc.options.forwardKey.setPressed(false);
                  mc.player.input.movementForward = 0.0F;
                  mc.player.input.pressingForward = false;
                  break;
               case Delay:
                  this.storedClicks.add(click);
                  e.cancel();
                  break;
               case StrictNCP2:
                  if (mc.player.isOnGround()
                     && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, 2.71875E-7, 0.0)).iterator().hasNext()) {
                     if (mc.player.isSprinting()) {
                        this.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                     }

                     this.sendPacket(
                        new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.71875E-7, mc.player.getZ(), false)
                     );
                  }
            }
         }

         if (e.getPacket() instanceof CloseHandledScreenC2SPacket && this.clickBypass.is(GuiMove.Bypass.Delay)) {
            this.pause.set(true);

            while (!this.storedClicks.isEmpty()) {
               this.sendPacket((Packet<?>)this.storedClicks.poll());
            }

            this.pause.set(false);
         }
      }
   }

   @EventHandler
   public void onPacketSendPost(PacketEvent.SendPost e) {
      if (e.getPacket() instanceof ClickSlotC2SPacket && mc.player.isSprinting() && this.clickBypass.is(GuiMove.Bypass.StrictNCP)) {
         this.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_SPRINTING));
      }
   }

   private static enum Bypass {
      DisableClicks,
      None,
      StrictNCP,
      GrimSwap,
      MatrixNcp,
      Delay,
      StrictNCP2;
   }
}
