package thunder.hack.features.modules.player;

import java.awt.Color;
import net.minecraft.util.Hand;
import net.minecraft.item.BlockItem;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.util.math.MatrixStack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.render.Render3DEngine;

public class AirPlace extends Module {
   private final Setting<Float> range = new Setting<>("Range", 5.0F, 0.0F, 6.0F);
   private final Setting<SettingGroup> renderGroup = new Setting<>("Render", new SettingGroup(false, 0));
   private final Setting<Boolean> swing = new Setting<>("Swing", true).addToGroup(this.renderGroup);
   private final Setting<ColorSetting> fillColor = new Setting<>("Fill Color", new ColorSetting(new Color(100, 50, 255, 50))).addToGroup(this.renderGroup);
   private final Setting<ColorSetting> lineColor = new Setting<>("Line Color", new ColorSetting(new Color(100, 50, 255, 150))).addToGroup(this.renderGroup);
   private final Setting<Integer> lineWidth = new Setting<>("Line Width", 2, 1, 5).addToGroup(this.renderGroup);
   private BlockHitResult hit;
   private int cooldown;

   public AirPlace() {
      super("AirPlace", Module.Category.PLAYER);
   }

   @Override
   public void onUpdate() {
      if (this.cooldown > 0) {
         this.cooldown--;
      }

      HitResult hitResult;
      if ((hitResult = mc.getCameraEntity().raycast((double)this.range.getValue().floatValue(), 0.0F, false)) instanceof BlockHitResult) {
         BlockHitResult bhr;
         this.hit = bhr = (BlockHitResult)hitResult;
         boolean main = mc.player.getMainHandStack().getItem() instanceof BlockItem;
         boolean off = mc.player.getOffHandStack().getItem() instanceof BlockItem;
         if (mc.options.useKey.isPressed() && (main || off) && this.cooldown <= 0) {
            mc.interactionManager.interactBlock(mc.player, main ? Hand.MAIN_HAND : Hand.OFF_HAND, this.hit);
            if (this.swing.getValue()) {
               mc.player.swingHand(main ? Hand.MAIN_HAND : Hand.OFF_HAND);
            } else {
               this.sendPacket(new HandSwingC2SPacket(main ? Hand.MAIN_HAND : Hand.OFF_HAND));
            }

            this.cooldown = !ModuleManager.fastUse.isEnabled() || !ModuleManager.fastUse.blocks.getValue() && !ModuleManager.fastUse.all.getValue() ? 4 : 0;
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (this.hit != null
         && mc.world.getBlockState(this.hit.getBlockPos()).getBlock().equals(Blocks.AIR)
         && (mc.player.getMainHandStack().getItem() instanceof BlockItem || mc.player.getOffHandStack().getItem() instanceof BlockItem)) {
         Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(new Box(this.hit.getBlockPos()), this.fillColor.getValue().getColorObject()));
         Render3DEngine.OUTLINE_QUEUE
            .add(
               new Render3DEngine.OutlineAction(
                  new Box(this.hit.getBlockPos()), this.lineColor.getValue().getColorObject(), (float)this.lineWidth.getValue().intValue()
               )
            );
      }
   }
}
