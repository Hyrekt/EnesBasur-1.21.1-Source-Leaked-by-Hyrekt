package thunder.hack.injection;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.features.modules.movement.ElytraBooster;

@Mixin({ClientPlayerEntity.class})
public class MixinElytraBooster {
   @Inject(
      method = {"tickMovement"},
      at = {@At("HEAD")}
   )
   private void onTickMovement(CallbackInfo info) {
      ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
      if (ElytraBooster.getInstance().isEnabled()) {
         if (player.isFallFlying()) {
            player.input.movementForward = 0.0F;
            player.input.movementSideways = 0.0F;
         }
      }
   }
}
