package thunder.hack.injection;

import net.minecraft.util.Identifier;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SkinTextures.class})
public class MixinSkinTextures {
   @Inject(
      method = {"texture"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getSkinTextureHook(CallbackInfoReturnable<Identifier> cir) {
   }
}
