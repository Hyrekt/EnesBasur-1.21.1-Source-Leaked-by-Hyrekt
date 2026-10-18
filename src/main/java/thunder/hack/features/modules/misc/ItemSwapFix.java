package thunder.hack.features.modules.misc;

import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import thunder.hack.features.modules.Module;

public class ItemSwapFix extends Module {
   public ItemSwapFix() {
      super("ItemSwapFix", Module.Category.MISC);
   }

   public void forceSync() {
      if (this.isEnabled() && mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
      }
   }
}
