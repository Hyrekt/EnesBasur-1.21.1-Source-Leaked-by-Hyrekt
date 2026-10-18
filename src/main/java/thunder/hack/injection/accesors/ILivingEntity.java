package thunder.hack.injection.accesors;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({LivingEntity.class})
public interface ILivingEntity {
   @Accessor("lastAttackedTicks")
   int getLastAttackedTicks();

   @Accessor("jumpingCooldown")
   int getLastJumpCooldown();

   @Accessor("jumpingCooldown")
   void setLastJumpCooldown(int var1);

   @Invoker("tryUseTotem")
   boolean invokeTryUseTotem(DamageSource var1);
}
