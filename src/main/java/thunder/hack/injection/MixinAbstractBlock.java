package thunder.hack.injection;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.player.AutoTool;

@Mixin({AbstractBlock.class})
public abstract class MixinAbstractBlock {
   @Inject(
      method = {"calcBlockBreakingDelta"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void calcBlockBreakingDeltaHook(BlockState state, PlayerEntity player, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> ci) {
      if (ModuleManager.autoTool.isEnabled() && AutoTool.silent.getValue()) {
         float f = state.getHardness(world, pos);
         if (f < 0.0F) {
            ci.setReturnValue(0.0F);
         } else {
            float dig_speed = this.getDigSpeed(state, player, player.getInventory().getStack(AutoTool.itemIndex)) / f;
            ci.setReturnValue(player.getInventory().getStack(AutoTool.itemIndex).isSuitableFor(state) ? dig_speed / 30.0F : dig_speed / 100.0F);
         }
      }
   }

   public float getDigSpeed(BlockState state, PlayerEntity player, ItemStack stack) {
      float str = stack.getMiningSpeedMultiplier(state);
      if (str > 1.0F) {
         int effect = EnchantmentHelper.getLevel(
            (RegistryEntry)player.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.EFFICIENCY).get(), stack
         );
         if (effect > 0) {
            str += (float)(effect * effect + 1);
         }
      }

      return Math.max(str, 0.0F);
   }
}
