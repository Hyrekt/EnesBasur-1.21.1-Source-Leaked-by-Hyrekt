package thunder.hack.features.modules.combat;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class ItemSwapFix extends Module {
   public static ItemSwapFix instance;
   public final Setting<Boolean> cancelServerSlot = new Setting<>("Cancel Server Slot", true);
   public final Setting<Boolean> autoResync = new Setting<>("Auto Resync", true);

   public ItemSwapFix() {
      super("ItemSwapFix", Module.Category.COMBAT);
      instance = this;
   }
}
