package thunder.hack.features.modules.player;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import thunder.hack.features.modules.Module;

public class MultiTask extends Module {
   public MultiTask() {
      super("MultiTask", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      HitResult ItemStackParticleEffect = mc.crosshairTarget;
      BlockHitResult crossHair;
      if (ItemStackParticleEffect instanceof BlockHitResult
         && (crossHair = (BlockHitResult)ItemStackParticleEffect).getBlockPos() != null
         && mc.options.attackKey.isPressed()
         && !mc.world.getBlockState(crossHair.getBlockPos()).isAir()) {
         mc.interactionManager.attackBlock(crossHair.getBlockPos(), crossHair.getSide());
         mc.player.swingHand(Hand.MAIN_HAND);
      }

      ItemStackParticleEffect = mc.crosshairTarget;
      EntityHitResult ehr;
      if (mc.crosshairTarget instanceof EntityHitResult
         && (ehr = (EntityHitResult)ItemStackParticleEffect).getEntity() != null
         && mc.options.attackKey.isPressed()
         && mc.player.getAttackCooldownProgress(0.5F) > 0.9F) {
         mc.interactionManager.attackEntity(mc.player, ehr.getEntity());
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }
}
