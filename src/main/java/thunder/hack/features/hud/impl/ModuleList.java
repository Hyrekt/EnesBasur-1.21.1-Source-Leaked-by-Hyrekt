package thunder.hack.features.hud.impl;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.DrawContext;
import thunder.hack.core.Managers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;

public class ModuleList extends HudElement {
   private final Setting<ModuleList.Mode> mode = new Setting<>("Mode", ModuleList.Mode.ColorText);
   private final Setting<Integer> gste = new Setting<>("GS", 30, 1, 50);
   private final Setting<Boolean> glow = new Setting<>("glow", false);
   private final Setting<Boolean> hrender = new Setting<>("HideHud", true);
   private final Setting<Boolean> hhud = new Setting<>("HideRender", true);
   private final Setting<ColorSetting> color3 = new Setting<>("RectColor", new ColorSetting(-16777216));
   private final Setting<ColorSetting> color4 = new Setting<>("SideRectColor", new ColorSetting(-16777216));

   public ModuleList() {
      super("ArrayList", 50, 30);
   }

   @Override
   public void onRender2D(DrawContext context) {
      super.onRender2D(context);
      boolean reverse = this.getPosX() > (float)mc.getWindow().getScaledWidth() / 2.0F;
      int offset = 0;
      float maxWidth = 0.0F;
      float reversedX = this.getPosX();

      List<Module> list;
      try {
         list = Managers.MODULE
            .getEnabledModules()
            .stream()
            .sorted(Comparator.comparing(module -> FontRenderers.modules.getStringWidth(module.getFullArrayString()) * -1.0F))
            .toList();
      } catch (IllegalArgumentException var11) {
         return;
      }

      for (Module module2 : list) {
         if (this.shouldRender(module2)) {
            Color color1 = HudEditor.getColor(offset);
            int stringWidth = (int)(
               FontRenderers.modules
                     .getStringWidth(
                        module2.getDisplayName()
                           + Formatting.GRAY
                           + (module2.getDisplayInfo() != null ? " [" + Formatting.WHITE + module2.getDisplayInfo() + Formatting.GRAY + "]" : "")
                     )
                  + 3.0F
            );
            if (this.glow.getValue()) {
               Render2DEngine.drawBlurredShadow(
                  context.getMatrices(),
                  reverse ? reversedX - (float)stringWidth - 3.0F : this.getPosX(),
                  this.getPosY() + (float)offset - 1.0F,
                  (float)(stringWidth + 4),
                  9.0F,
                  this.gste.getValue(),
                  color1
               );
            }

            if ((float)stringWidth > maxWidth) {
               maxWidth = (float)stringWidth;
            }

            offset += 9;
         }
      }

      int var14 = 0;

      for (Module module2x : list) {
         if (this.shouldRender(module2x)) {
            int stringWidthx = (int)(
               FontRenderers.modules
                     .getStringWidth(
                        module2x.getDisplayName()
                           + Formatting.GRAY
                           + (module2x.getDisplayInfo() != null ? " [" + Formatting.WHITE + module2x.getDisplayInfo() + Formatting.GRAY + "]" : "")
                     )
                  + 3.0F
            );
            Color color1x = HudEditor.getColor(var14);
            if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
               Render2DEngine.drawRoundedBlur(
                  context.getMatrices(),
                  reverse ? reversedX - (float)stringWidthx : this.getPosX(),
                  this.getPosY() + (float)var14,
                  (float)stringWidthx + 1.0F,
                  9.0F,
                  2.0F,
                  HudEditor.blurColor.getValue().getColorObject()
               );
            } else {
               Render2DEngine.drawRect(
                  context.getMatrices(),
                  reverse ? reversedX - (float)stringWidthx : this.getPosX(),
                  this.getPosY() + (float)var14,
                  (float)stringWidthx + 1.0F,
                  9.0F,
                  this.mode.getValue() == ModuleList.Mode.ColorRect ? color1x : this.color3.getValue().getColorObject()
               );
               Render2DEngine.drawRect(
                  context.getMatrices(),
                  reverse ? reversedX + 1.0F : this.getPosX() - 2.0F,
                  this.getPosY() + (float)var14,
                  2.0F,
                  9.0F,
                  this.mode.getValue() == ModuleList.Mode.ColorRect ? this.color4.getValue().getColorObject() : color1x
               );
            }

            FontRenderers.modules
               .drawString(
                  context.getMatrices(),
                  module2x.getDisplayName()
                     + Formatting.GRAY
                     + (module2x.getDisplayInfo() != null ? " [" + Formatting.WHITE + module2x.getDisplayInfo() + Formatting.GRAY + "]" : ""),
                  reverse ? (double)(reversedX - (float)stringWidthx + 2.0F) : (double)(this.getPosX() + 3.0F),
                  (double)(this.getPosY() + 3.0F + (float)var14),
                  this.mode.getValue() == ModuleList.Mode.ColorRect ? -1 : color1x.getRGB()
               );
            var14 += 9;
         }
      }

      this.setBounds(this.getPosX(), this.getPosY(), (float)((int)maxWidth * (reverse ? -1 : 1)), (float)var14);
   }

   private boolean shouldRender(Module m) {
      return m.isDrawn()
         && (!this.hrender.getValue() || m.getCategory() != Module.Category.RENDER)
         && (!this.hhud.getValue() || m.getCategory() != Module.Category.HUD);
   }

   private static enum Mode {
      ColorText,
      ColorRect;
   }
}
