package thunder.hack.features.modules.render;

import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

public class Trajectories extends Module {
   private final Setting<Trajectories.Mode> mode = new Setting<>("ColorMode", Trajectories.Mode.Sync);
   private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(575714484), v -> this.mode.getValue() == Trajectories.Mode.Custom);
   private final Setting<Trajectories.Mode> lmode = new Setting<>("LandedColorMode", Trajectories.Mode.Sync);
   private final Setting<ColorSetting> lcolor = new Setting<>(
      "LandedColor", new ColorSetting(575714484), v -> this.lmode.getValue() == Trajectories.Mode.Custom
   );

   public Trajectories() {
      super("Trajectories", Module.Category.RENDER);
   }

   private boolean isThrowable(Item item) {
      return item instanceof EnderPearlItem
         || item instanceof TridentItem
         || item instanceof ExperienceBottleItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || item instanceof SplashPotionItem
         || item instanceof LingeringPotionItem;
   }

   private float getDistance(Item item) {
      return item instanceof BowItem ? 1.0F : 0.4F;
   }

   private float getThrowVelocity(Item item) {
      if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
         return 0.5F;
      } else if (item instanceof ExperienceBottleItem) {
         return 0.59F;
      } else {
         return item instanceof TridentItem ? 2.0F : 1.5F;
      }
   }

   private int getThrowPitch(Item item) {
      return !(item instanceof SplashPotionItem) && !(item instanceof LingeringPotionItem) && !(item instanceof ExperienceBottleItem) ? 0 : 20;
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (!mc.options.hudHidden) {
         if (mc.player != null && mc.world != null && mc.options.getPerspective().isFirstPerson()) {
            ItemStack mainHand = mc.player.getMainHandStack();
            ItemStack offHand = mc.player.getOffHandStack();
            Hand hand;
            if (!(mainHand.getItem() instanceof BowItem) && !(mainHand.getItem() instanceof CrossbowItem) && !this.isThrowable(mainHand.getItem())
               )
             {
               if (!(offHand.getItem() instanceof BowItem) && !(offHand.getItem() instanceof CrossbowItem) && !this.isThrowable(offHand.getItem())
                  )
                {
                  return;
               }

               hand = Hand.OFF_HAND;
            } else {
               hand = Hand.MAIN_HAND;
            }

            boolean prev_bob = (Boolean)mc.options.getBobView().getValue();
            mc.options.getBobView().setValue(false);
            if (offHand.getItem() instanceof CrossbowItem
                  && EnchantmentHelper.getLevel(
                        (RegistryEntry)mc.world.getRegistryManager().get(Enchantments.MULTISHOT.getRegistryRef()).getEntry(Enchantments.MULTISHOT).get(),
                        offHand
                     )
                     != 0
               || mainHand.getItem() instanceof CrossbowItem
                  && EnchantmentHelper.getLevel(
                        (RegistryEntry)mc.world.getRegistryManager().get(Enchantments.MULTISHOT.getRegistryRef()).getEntry(Enchantments.MULTISHOT).get(),
                        mainHand
                     )
                     != 0) {
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw() - 10.0F);
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw());
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw() + 10.0F);
            } else {
               this.calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.player.getYaw());
            }

            mc.options.getBobView().setValue(prev_bob);
         }
      }
   }

   private void calcTrajectory(Item item, float yaw) {
      double x = Render2DEngine.interpolate(mc.player.prevX, mc.player.getX(), (double)Render3DEngine.getTickDelta());
      double y = Render2DEngine.interpolate(mc.player.prevY, mc.player.getY(), (double)Render3DEngine.getTickDelta());
      double z = Render2DEngine.interpolate(mc.player.prevZ, mc.player.getZ(), (double)Render3DEngine.getTickDelta());
      y = y + (double)mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
      if (item == mc.player.getMainHandStack().getItem()) {
         x -= (double)(MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F);
         z -= (double)(MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F);
      } else {
         x += (double)(MathHelper.cos(yaw / 180.0F * (float) Math.PI) * 0.16F);
         z += (double)(MathHelper.sin(yaw / 180.0F * (float) Math.PI) * 0.16F);
      }

      float maxDist = this.getDistance(item);
      double motionX = (double)(
         -MathHelper.sin(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(mc.player.getPitch() / 180.0F * (float) Math.PI) * maxDist
      );
      double motionY = (double)(-MathHelper.sin((mc.player.getPitch() - (float)this.getThrowPitch(item)) / 180.0F * 3.141593F) * maxDist);
      double motionZ = (double)(
         MathHelper.cos(yaw / 180.0F * (float) Math.PI) * MathHelper.cos(mc.player.getPitch() / 180.0F * (float) Math.PI) * maxDist
      );
      float power = (float)mc.player.getItemUseTime() / 20.0F;
      power = (power * power + power * 2.0F) / 3.0F;
      if (power > 1.0F || power == 0.0F) {
         power = 1.0F;
      }

      float distance = MathHelper.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
      motionX /= (double)distance;
      motionY /= (double)distance;
      motionZ /= (double)distance;
      float pow = (item instanceof BowItem ? power * 2.0F : (item instanceof CrossbowItem ? 2.2F : 1.0F)) * this.getThrowVelocity(item);
      motionX *= (double)pow;
      motionY *= (double)pow;
      motionZ *= (double)pow;
      if (!mc.player.isOnGround()) {
         motionY += mc.player.getVelocity().getY();
      }

      for (int i = 0; i < 300; i++) {
         Vec3d lastPos = new Vec3d(x, y, z);
         if (mc.world.getBlockState(new BlockPos((int)(x += motionX), (int)(y += motionY), (int)(z += motionZ))).getBlock() == Blocks.WATER) {
            motionX *= 0.8;
            motionY *= 0.8;
            motionZ *= 0.8;
         } else {
            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;
         }

         double var33;
         double var34;
         double var35;
         motionY = item instanceof BowItem
            ? (var33 = motionY - 0.05F)
            : (mc.player.getMainHandStack().getItem() instanceof CrossbowItem ? (var34 = motionY - 0.05F) : (var35 = motionY - 0.03F));
         Vec3d pos = new Vec3d(x, y, z);

         for (Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof ArrowEntity)
               && !ent.equals(mc.player)
               && ent.getBoundingBox().intersects(new Box(x - 0.3, y - 0.3, z - 0.3, x + 0.3, y + 0.3, z + 0.3))) {
               Render3DEngine.OUTLINE_QUEUE
                  .add(
                     new Render3DEngine.OutlineAction(
                        ent.getBoundingBox(),
                        this.lmode.getValue() == Trajectories.Mode.Sync ? HudEditor.getColor(i * 10) : this.lcolor.getValue().getColorObject(),
                        2.0F
                     )
                  );
               Render3DEngine.FILLED_QUEUE
                  .add(
                     new Render3DEngine.FillAction(
                        ent.getBoundingBox(),
                        this.lmode.getValue() == Trajectories.Mode.Sync
                           ? Render2DEngine.injectAlpha(HudEditor.getColor(i * 10), 100)
                           : this.lcolor.getValue().getColorObject()
                     )
                  );
               break;
            }
         }

         BlockHitResult bhr;
         if ((bhr = mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player))) != null
            && bhr.getType() == Type.BLOCK) {
            Render3DEngine.OUTLINE_SIDE_QUEUE
               .add(
                  new Render3DEngine.OutlineSideAction(
                     new Box(bhr.getBlockPos()),
                     this.lmode.getValue() == Trajectories.Mode.Sync ? HudEditor.getColor(i * 10) : this.lcolor.getValue().getColorObject(),
                     2.0F,
                     bhr.getSide()
                  )
               );
            Render3DEngine.FILLED_SIDE_QUEUE
               .add(
                  new Render3DEngine.FillSideAction(
                     new Box(bhr.getBlockPos()),
                     this.lmode.getValue() == Trajectories.Mode.Sync
                        ? Render2DEngine.injectAlpha(HudEditor.getColor(i * 10), 100)
                        : this.lcolor.getValue().getColorObject(),
                     bhr.getSide()
                  )
               );
            break;
         }

         if (y <= -65.0) {
            break;
         }

         if (motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
            Render3DEngine.drawLine(
               lastPos, pos, this.mode.getValue() == Trajectories.Mode.Sync ? HudEditor.getColor(i) : this.color.getValue().getColorObject()
            );
         }
      }
   }

   private static enum Mode {
      Custom,
      Sync;
   }
}
