package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class NoPush extends Module {
   public Setting<Boolean> blocks = new Setting<>("Blocks", true);
   public Setting<Boolean> players = new Setting<>("Players", true);
   public Setting<Boolean> water = new Setting<>("Liquids", true);
   public Setting<Boolean> fishingHook = new Setting<>("FishingHook", true);

   public NoPush() {
      super("NoPush", Module.Category.MOVEMENT);
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      Object object = e.getPacket();
      EntityStatusS2CPacket pac;
      if (object instanceof EntityStatusS2CPacket && (pac = (EntityStatusS2CPacket)object).getStatus() == 31 && (object = pac.getEntity(mc.world)) instanceof FishingBobberEntity) {
         FishingBobberEntity hook = (FishingBobberEntity)object;
         if (this.fishingHook.getValue() && hook.getHookedEntity() == mc.player) {
            e.cancel();
         }
      }
   }
}
