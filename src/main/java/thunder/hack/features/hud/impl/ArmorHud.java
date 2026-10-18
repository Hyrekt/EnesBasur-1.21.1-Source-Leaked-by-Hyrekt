package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.DrawContext;
import thunder.hack.features.hud.HudElement;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;

public class ArmorHud extends HudElement {
   private final Setting<ArmorHud.Mode> mode = new Setting<>("Mode", ArmorHud.Mode.V2);

   public ArmorHud() {
      super("ArmorHud", 60, 25);
   }

   @Override
   public void onRender2D(DrawContext context) {
      super.onRender2D(context);
      float xItemOffset = this.getPosX();

      for (ItemStack itemStack : mc.player.getInventory().armor.reversed()) {
         if (!itemStack.isEmpty()) {
            if (this.mode.is(ArmorHud.Mode.V1)) {
               context.drawItem(itemStack, (int)xItemOffset, (int)this.getPosY());
               context.drawItemInSlot(mc.textRenderer, itemStack, (int)xItemOffset, (int)this.getPosY());
            } else {
               RenderSystem.setShaderColor(0.4F, 0.4F, 0.4F, 0.35F);
               context.drawItem(itemStack, (int)xItemOffset, (int)this.getPosY());
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               Item class_17922 = itemStack.getItem();
               ArmorItem ai;
               float offset = class_17922 instanceof ArmorItem && (ai = (ArmorItem)class_17922).getSlotType() == EquipmentSlot.HEAD ? -4.0F : 0.0F;
               Render2DEngine.addWindow(
                  context.getMatrices(),
                  (float)((int)xItemOffset),
                  this.getPosY() + offset + (15.0F - offset) * ((float)itemStack.getDamage() / (float)itemStack.getMaxDamage()),
                  xItemOffset + 15.0F,
                  this.getPosY() + 15.0F,
                  1.0
               );
               context.drawItem(itemStack, (int)xItemOffset, (int)this.getPosY());
               Render2DEngine.popWindow();
            }

            xItemOffset += 20.0F;
         }
      }

      this.setBounds(this.getPosX(), this.getPosY(), 60.0F, 25.0F);
   }

   private static enum Mode {
      V1,
      V2;
   }
}
