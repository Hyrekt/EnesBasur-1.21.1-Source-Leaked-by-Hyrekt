package thunder.hack.features.modules.misc;

import java.util.function.Consumer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.util.hit.HitResult;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.AsyncManager;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.cmd.Command;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.PlayerUtility;

public class MiddleClick extends Module {
   private final Setting<MiddleClick.Action> onBlock = new Setting<>("OnBlock", MiddleClick.Action.Xp);
   private final Setting<MiddleClick.Action> onAir = new Setting<>("OnAir", MiddleClick.Action.Pearl);
   private final Setting<MiddleClick.Action> onEntity = new Setting<>("OnEntity", MiddleClick.Action.Friend);
   private final Setting<MiddleClick.Action> onFlying = new Setting<>("OnElytra", MiddleClick.Action.None);
   private final Setting<Boolean> silent = new Setting<>("Silent", true);
   private final Setting<Boolean> inventory = new Setting<>("Inventory", true);
   private final Setting<Integer> swapDelay = new Setting<>("SwapDelay", 100, 0, 1000, v -> !this.silent.getValue());
   private final Setting<BooleanSettingGroup> antiWaste = new Setting<>("AntiWaste", new BooleanSettingGroup(true));
   private final Setting<Integer> durability = new Setting<>("StopOn", 90, 0, 100).addToGroup(this.antiWaste);
   public final Setting<Boolean> antiPickUp = new Setting<>("AntiPickUp", true);
   private final Setting<Boolean> feetExp = new Setting<>("FeetXP", false);
   private static final Timer timer = new Timer();
   private String state = "none";

   public MiddleClick() {
      super("MiddleClick", Module.Category.MISC);
   }

   @Override
   public String getDisplayInfo() {
      return this.state;
   }

   @EventHandler
   private void onSync(EventSync event) {
      if (mc.currentScreen == null) {
         HitResult target = mc.crosshairTarget;
         if (mc.player.isFallFlying()) {
            if (mc.options.pickItemKey.isPressed()) {
               this.onFlying.getValue().doAction(event);
            }

            this.state = this.onFlying.getValue().toString();
            return;
         }

         EntityHitResult ehr;
         if (target instanceof EntityHitResult && (ehr = (EntityHitResult)target).getEntity() instanceof PlayerEntity) {
            if (mc.options.pickItemKey.isPressed()) {
               this.onEntity.getValue().doAction(event);
            }

            this.state = this.onEntity.getValue().toString();
            return;
         }

         if (target instanceof BlockHitResult bhr) {
            if (mc.world.isAir(bhr.getBlockPos())) {
               if (mc.options.pickItemKey.isPressed()) {
                  this.onAir.getValue().doAction(event);
               }

               this.state = this.onAir.getValue().toString();
            } else {
               if (mc.options.pickItemKey.isPressed()) {
                  this.onBlock.getValue().doAction(event);
               }

               this.state = this.onBlock.getValue().toString();
            }

            return;
         }

         if (mc.options.pickItemKey.isPressed()) {
            this.onAir.getValue().doAction(event);
         }

         this.state = this.onAir.getValue().toString();
      }
   }

   private static boolean needXp() {
      for (ItemStack stack : mc.player.getArmorItems()) {
         if (PlayerUtility.calculatePercentage(stack) < (float)ModuleManager.middleClick.durability.getValue().intValue()) {
            return true;
         }
      }

      return false;
   }

   private static int getHpSlot() {
      for (int i = 0; i < 9; i++) {
         if (isStackPotion(mc.player.getInventory().getStack(i))) {
            return i;
         }
      }

      return -1;
   }

   private static int findHpInInventory() {
      for (int i = 36; i >= 0; i--) {
         if (isStackPotion(mc.player.getInventory().getStack(i))) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   private static boolean isStackPotion(ItemStack stack) {
      if (stack == null) {
         return false;
      } else {
         if (stack.getItem() == Items.SPLASH_POTION) {
            PotionContentsComponent potionContentsComponent = (PotionContentsComponent)stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
               if (effect.getEffectType().value() == StatusEffects.INSTANT_HEALTH.value()) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public static enum Action {
      HealingPot(
         e -> {
            if (MiddleClick.timer.every(250L)) {
               if (MiddleClick.getHpSlot() != -1) {
                  int hpSlot = MiddleClick.getHpSlot();
                  int originalSlot = Module.mc.player.getInventory().selectedSlot;
                  if (hpSlot != -1) {
                     InventoryUtility.switchTo(hpSlot);
                     InteractionUtility.sendSequencedPacket(
                        id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Module.mc.player.getYaw(), Module.mc.player.getPitch())
                     );
                     InventoryUtility.switchTo(originalSlot);
                  }
               } else {
                  int hpSlot = MiddleClick.findHpInInventory();
                  if (hpSlot != -1) {
                     Module.mc
                        .interactionManager
                        .clickSlot(
                           Module.mc.player.currentScreenHandler.syncId,
                           hpSlot,
                           Module.mc.player.getInventory().selectedSlot,
                           SlotActionType.SWAP,
                           Module.mc.player
                        );
                     InteractionUtility.sendSequencedPacket(
                        id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Module.mc.player.getYaw(), Module.mc.player.getPitch())
                     );
                     Module.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                     Module.mc
                        .interactionManager
                        .clickSlot(
                           Module.mc.player.currentScreenHandler.syncId,
                           hpSlot,
                           Module.mc.player.getInventory().selectedSlot,
                           SlotActionType.SWAP,
                           Module.mc.player
                        );
                  }
               }
            }
         }
      ),
      Firework(
         e -> {
            if (MiddleClick.timer.every(250L)) {
               int epSlot1 = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET).slot();
               if (ModuleManager.middleClick.silent.getValue()) {
                  if (!ModuleManager.middleClick.inventory.getValue() || ModuleManager.middleClick.inventory.getValue() && epSlot1 != -1) {
                     int originalSlot = Module.mc.player.getInventory().selectedSlot;
                     if (epSlot1 != -1) {
                        Module.mc.player.getInventory().selectedSlot = epSlot1;
                        Module.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(epSlot1));
                        InteractionUtility.sendSequencedPacket(
                           id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Module.mc.player.getYaw(), Module.mc.player.getPitch())
                        );
                        Module.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        Module.mc.player.getInventory().selectedSlot = originalSlot;
                        Module.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
                     }
                  } else {
                     int epSlot = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET).slot();
                     if (epSlot != -1) {
                        Module.mc
                           .interactionManager
                           .clickSlot(
                              Module.mc.player.currentScreenHandler.syncId,
                              epSlot,
                              Module.mc.player.getInventory().selectedSlot,
                              SlotActionType.SWAP,
                              Module.mc.player
                           );
                        InteractionUtility.sendSequencedPacket(
                           id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Module.mc.player.getYaw(), Module.mc.player.getPitch())
                        );
                        Module.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        Module.mc
                           .interactionManager
                           .clickSlot(
                              Module.mc.player.currentScreenHandler.syncId,
                              epSlot,
                              Module.mc.player.getInventory().selectedSlot,
                              SlotActionType.SWAP,
                              Module.mc.player
                           );
                     }
                  }
               } else if (!ModuleManager.middleClick.inventory.getValue() || ModuleManager.middleClick.inventory.getValue() && epSlot1 != -1) {
                  if (epSlot1 != -1) {
                     new MiddleClick.PearlThread(
                           Module.mc.player, epSlot1, Module.mc.player.getInventory().selectedSlot, ModuleManager.middleClick.swapDelay.getValue(), false
                        )
                        .start();
                  }
               } else {
                  int epSlot = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET).slot();
                  if (epSlot != -1) {
                     new MiddleClick.PearlThread(
                           Module.mc.player, epSlot, Module.mc.player.getInventory().selectedSlot, ModuleManager.middleClick.swapDelay.getValue(), true
                        )
                        .start();
                  }
               }
            }
         }
      ),
      Friend(
         e -> {
            if (MiddleClick.timer.every(800L)) {
               HitResult patt0$temp = Module.mc.crosshairTarget;
               EntityHitResult ehr;
               if (Module.mc.crosshairTarget instanceof EntityHitResult && (ehr = (EntityHitResult)patt0$temp).getEntity() instanceof PlayerEntity) {
                  if (Managers.FRIEND.isFriend(ehr.getEntity().getName().getString())) {
                     Managers.FRIEND.removeFriend(ehr.getEntity().getName().getString());
                     Command.sendMessage(
                        ClientSettings.isRu()
                           ? "\u00a7b"
                              + ehr.getEntity().getName().getString()
                              + "\u00a7r \u0443\u0434\u0430\u043b\u0435\u043d \u0438\u0437 \u0434\u0440\u0443\u0437\u0435\u0439!"
                           : "\u00a7b" + ehr.getEntity().getName().getString() + "\u00a7r removed from friends!"
                     );
                  } else {
                     Managers.FRIEND.addFriend(ehr.getEntity().getName().getString());
                     Command.sendMessage(
                        ClientSettings.isRu()
                           ? "\u0414\u043e\u0431\u0430\u0432\u043b\u0435\u043d \u0434\u0440\u0443\u0433 \u00a7b" + ehr.getEntity().getName().getString()
                           : "Added friend \u00a7b" + ehr.getEntity().getName().getString()
                     );
                  }
               }
            }
         }
      ),
      Pearl(
         e -> {
            if (MiddleClick.timer.every(500L)) {
               if (ModuleManager.aura.isEnabled() && Aura.target != null) {
                  Module.mc
                     .player
                     .networkHandler
                     .sendPacket(
                        new LookAndOnGround(Module.mc.player.getYaw(), Module.mc.player.getPitch(), Module.mc.player.isOnGround())
                     );
               }

               int epSlot1 = InventoryUtility.findItemInHotBar(Items.ENDER_PEARL).slot();
               if (ModuleManager.middleClick.silent.getValue()) {
                  if (!ModuleManager.middleClick.inventory.getValue() || ModuleManager.middleClick.inventory.getValue() && epSlot1 != -1) {
                     int originalSlot = Module.mc.player.getInventory().selectedSlot;
                     if (epSlot1 != -1) {
                        Module.mc.player.getInventory().selectedSlot = epSlot1;
                        Module.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(epSlot1));
                        InteractionUtility.sendSequencedPacket(
                           id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Module.mc.player.getYaw(), Module.mc.player.getPitch())
                        );
                        Module.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        Module.mc.player.getInventory().selectedSlot = originalSlot;
                        Module.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
                     }
                  } else {
                     int epSlot = InventoryUtility.findItemInInventory(Items.ENDER_PEARL).slot();
                     if (epSlot != -1) {
                        Module.mc
                           .interactionManager
                           .clickSlot(
                              Module.mc.player.currentScreenHandler.syncId,
                              epSlot,
                              Module.mc.player.getInventory().selectedSlot,
                              SlotActionType.SWAP,
                              Module.mc.player
                           );
                        InteractionUtility.sendSequencedPacket(
                           id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Module.mc.player.getYaw(), Module.mc.player.getPitch())
                        );
                        Module.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        Module.mc
                           .interactionManager
                           .clickSlot(
                              Module.mc.player.currentScreenHandler.syncId,
                              epSlot,
                              Module.mc.player.getInventory().selectedSlot,
                              SlotActionType.SWAP,
                              Module.mc.player
                           );
                     }
                  }
               } else if (!ModuleManager.middleClick.inventory.getValue() || ModuleManager.middleClick.inventory.getValue() && epSlot1 != -1) {
                  if (epSlot1 != -1) {
                     new MiddleClick.PearlThread(
                           Module.mc.player, epSlot1, Module.mc.player.getInventory().selectedSlot, ModuleManager.middleClick.swapDelay.getValue(), false
                        )
                        .start();
                  }
               } else {
                  int epSlot = InventoryUtility.findItemInInventory(Items.ENDER_PEARL).slot();
                  if (epSlot != -1) {
                     new MiddleClick.PearlThread(
                           Module.mc.player, epSlot, Module.mc.player.getInventory().selectedSlot, ModuleManager.middleClick.swapDelay.getValue(), true
                        )
                        .start();
                  }
               }
            }
         }
      ),
      Xp(
         e -> {
            if (ModuleManager.middleClick.feetExp.getValue() && (!ModuleManager.middleClick.antiWaste.getValue().isEnabled() || MiddleClick.needXp())) {
               Module.mc.player.setPitch(90.0F);
            }

            e.addPostAction(
               () -> {
                  if (!ModuleManager.middleClick.antiWaste.getValue().isEnabled() || MiddleClick.needXp()) {
                     int slot;
                     if (Module.mc.options.pickItemKey.isPressed() && (slot = InventoryUtility.findItemInHotBar(Items.EXPERIENCE_BOTTLE).slot()) != -1) {
                        int lastSlot = Module.mc.player.getInventory().selectedSlot;
                        InventoryUtility.switchTo(slot);
                        InteractionUtility.sendSequencedPacket(
                           id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Module.mc.player.getYaw(), Module.mc.player.getPitch())
                        );
                        if (ModuleManager.middleClick.silent.getValue()) {
                           InventoryUtility.switchTo(lastSlot);
                        }
                     }
                  }
               }
            );
         }
      ),
      None(e -> {
      });

      private final Consumer<EventSync> r;

      private Action(Consumer<EventSync> r) {
         this.r = r;
      }

      public void doAction(EventSync e) {
         this.r.accept(e);
      }
   }

   public static class PearlThread extends Thread {
      public ClientPlayerEntity player;
      int epSlot;
      int originalSlot;
      int delay;
      boolean inv;

      public PearlThread(ClientPlayerEntity entityPlayer, int epSlot, int originalSlot, int delay, boolean inventory) {
         this.player = entityPlayer;
         this.epSlot = epSlot;
         this.originalSlot = originalSlot;
         this.delay = delay;
         this.inv = inventory;
      }

      @Override
      public void run() {
         if (!this.inv) {
            InventoryUtility.switchTo(this.epSlot);
            AsyncManager.sleep(this.delay);
            InteractionUtility.sendSequencedPacket(
               id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Module.mc.player.getYaw(), Module.mc.player.getPitch())
            );
            Module.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            AsyncManager.sleep(this.delay);
            InventoryUtility.switchTo(this.originalSlot);
         } else {
            Module.mc
               .interactionManager
               .clickSlot(Module.mc.player.currentScreenHandler.syncId, this.epSlot, this.originalSlot, SlotActionType.SWAP, Module.mc.player);
            AsyncManager.sleep(this.delay);
            if (ModuleManager.aura.isEnabled() && Aura.target != null) {
               Module.mc
                  .player
                  .networkHandler
                  .sendPacket(new LookAndOnGround(Module.mc.player.getYaw(), Module.mc.player.getPitch(), Module.mc.player.isOnGround()));
            }

            InteractionUtility.sendSequencedPacket(
               id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Module.mc.player.getYaw(), Module.mc.player.getPitch())
            );
            Module.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            AsyncManager.sleep(this.delay);
            Module.mc
               .interactionManager
               .clickSlot(Module.mc.player.currentScreenHandler.syncId, this.epSlot, this.originalSlot, SlotActionType.SWAP, Module.mc.player);
         }

         super.run();
      }
   }
}
