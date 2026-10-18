package thunder.hack.features.hud.impl;

import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;

public class KeyBinds extends HudElement {
   public final Setting<ColorSetting> oncolor = new Setting<>("OnColor", new ColorSetting(-1));
   public final Setting<ColorSetting> offcolor = new Setting<>("OffColor", new ColorSetting(1));
   public final Setting<Boolean> onlyEnabled = new Setting<>("OnlyEnabled", false);
   private float vAnimation;
   private float hAnimation;

   public KeyBinds() {
      super("KeyBinds", 100, 100);
   }

   @Override
   public void onRender2D(DrawContext context) {
      super.onRender2D(context);
      int y_offset1 = 0;
      float max_width = 50.0F;
      float maxBindWidth = 0.0F;
      float pointerX = 0.0F;

      for (Module feature : Managers.MODULE.modules) {
         if ((!feature.isDisabled() || !this.onlyEnabled.getValue())
            && !Objects.equals(feature.getBind().getBind(), "None")
            && feature != ModuleManager.clickGui) {
            if (y_offset1 == 0) {
               y_offset1 += 4;
            }

            y_offset1 += 9;
            float nameWidth = FontRenderers.sf_bold_mini.getStringWidth(feature.getName());
            float bindWidth = FontRenderers.sf_bold_mini.getStringWidth(getShortKeyName(feature));
            if (bindWidth > maxBindWidth) {
               maxBindWidth = bindWidth;
            }

            if (nameWidth > pointerX) {
               pointerX = nameWidth;
            }
         }
      }

      float px = this.getPosX() + 10.0F + pointerX;
      max_width = Math.max(20.0F + pointerX + maxBindWidth, 50.0F);
      this.vAnimation = AnimationUtility.fast(this.vAnimation, (float)(14 + y_offset1), 15.0F);
      this.hAnimation = AnimationUtility.fast(this.hAnimation, max_width, 15.0F);
      Render2DEngine.drawHudBase(context.getMatrices(), this.getPosX(), this.getPosY(), this.hAnimation, this.vAnimation, HudEditor.hudRound.getValue());
      if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
         FontRenderers.sf_bold
            .drawCenteredString(
               context.getMatrices(),
               "KeyBinds",
               (double)(this.getPosX() + this.hAnimation / 2.0F),
               (double)(this.getPosY() + 4.0F),
               HudEditor.textColor.getValue().getColorObject()
            );
      } else {
         FontRenderers.sf_bold
            .drawGradientCenteredString(context.getMatrices(), "KeyBinds", this.getPosX() + this.hAnimation / 2.0F, this.getPosY() + 4.0F, 10);
      }

      if (y_offset1 > 0) {
         if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawRectDumbWay(
               context.getMatrices(),
               this.getPosX() + 4.0F,
               this.getPosY() + 13.0F,
               this.getPosX() + this.getWidth() - 4.0F,
               this.getPosY() + 13.5F,
               new Color(1426063359, true)
            );
         } else {
            Render2DEngine.horizontalGradient(
               context.getMatrices(),
               this.getPosX() + 2.0F,
               this.getPosY() + 13.7F,
               this.getPosX() + 2.0F + this.hAnimation / 2.0F - 2.0F,
               this.getPosY() + 14.0F,
               Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0),
               HudEditor.textColor.getValue().getColorObject()
            );
            Render2DEngine.horizontalGradient(
               context.getMatrices(),
               this.getPosX() + 2.0F + this.hAnimation / 2.0F - 2.0F,
               this.getPosY() + 13.7F,
               this.getPosX() + 2.0F + this.hAnimation - 4.0F,
               this.getPosY() + 14.0F,
               HudEditor.textColor.getValue().getColorObject(),
               Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0)
            );
         }
      }

      Render2DEngine.addWindow(context.getMatrices(), this.getPosX(), this.getPosY(), this.getPosX() + this.hAnimation, this.getPosY() + this.vAnimation, 1.0);
      int y_offset = 0;

      for (Module featurex : Managers.MODULE.modules) {
         if ((!featurex.isDisabled() || !this.onlyEnabled.getValue())
            && !Objects.equals(featurex.getBind().getBind(), "None")
            && featurex != ModuleManager.clickGui) {
            FontRenderers.sf_bold_mini
               .drawString(
                  context.getMatrices(),
                  featurex.getName(),
                  (double)(this.getPosX() + 5.0F),
                  (double)(this.getPosY() + 19.0F + (float)y_offset),
                  featurex.isOn() ? this.oncolor.getValue().getColor() : this.offcolor.getValue().getColor()
               );
            FontRenderers.sf_bold_mini
               .drawCenteredString(
                  context.getMatrices(),
                  getShortKeyName(featurex),
                  (double)(px + (this.getPosX() + max_width - px) / 2.0F),
                  (double)(this.getPosY() + 19.0F + (float)y_offset),
                  featurex.isOn() ? this.oncolor.getValue().getColor() : this.offcolor.getValue().getColor()
               );
            Render2DEngine.drawRect(context.getMatrices(), px, this.getPosY() + 17.0F + (float)y_offset, 0.5F, 8.0F, new Color(1157627903, true));
            y_offset += 9;
         }
      }

      Render2DEngine.popWindow();
      this.setBounds(this.getPosX(), this.getPosY(), this.hAnimation, this.vAnimation);
   }

   @NotNull
   public static String getShortKeyName(Module feature) {
      String sbind = feature.getBind().getBind();
      String var2 = feature.getBind().getBind();

      return switch (var2) {
         case "LEFT_CONTROL" -> "LCtrl";
         case "RIGHT_CONTROL" -> "RCtrl";
         case "LEFT_SHIFT" -> "LShift";
         case "RIGHT_SHIFT" -> "RShift";
         case "LEFT_ALT" -> "LAlt";
         case "RIGHT_ALT" -> "RAlt";
         default -> sbind.toUpperCase();
      };
   }
}
