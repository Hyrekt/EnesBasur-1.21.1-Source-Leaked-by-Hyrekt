package thunder.hack.gui.misc;

import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.render.Render2DEngine;

public class GuiScanner extends Screen {
   public static boolean neartrack = false;
   public static boolean track = false;
   public static boolean busy = false;
   public ArrayList<String> consoleout = new ArrayList<>();
   int radarx;
   int radary;
   int radarx1;
   int radary1;
   int centerx;
   int centery;
   int consolex;
   int consoley;
   int consolex1;
   int consoley1;
   int hovery;
   int hoverx;
   int searchx;
   int searchy;
   int wheely;

   public GuiScanner() {
      super(Text.of("GuiScanner"));
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (Module.mc.player != null) {
         this.radarx = Module.mc.getWindow().getScaledWidth() / 8;
         this.radarx1 = Module.mc.getWindow().getScaledWidth() * 5 / 8;
         this.radary = Module.mc.getWindow().getScaledHeight() / 2 - (this.radarx1 - this.radarx) / 2;
         this.radary1 = Module.mc.getWindow().getScaledHeight() / 2 + (this.radarx1 - this.radarx) / 2;
         this.centerx = (this.radarx + this.radarx1) / 2;
         this.centery = (this.radary + this.radary1) / 2;
         this.consolex = (int)((float)Module.mc.getWindow().getScaledWidth() * 5.5F / 8.0F);
         this.consolex1 = Module.mc.getWindow().getScaledWidth() - 50;
         this.consoley = this.radary;
         this.consoley1 = this.radary1 - 50;
         Render2DEngine.drawRectDumbWay(
            context.getMatrices(), (float)this.consolex, (float)this.consoley, (float)this.consolex1, (float)this.consoley1, new Color(-150205428, true)
         );
         Render2DEngine.drawRectDumbWay(
            context.getMatrices(),
            (float)this.consolex,
            (float)(this.consoley1 + 3),
            (float)this.consolex1,
            (float)(this.consoley1 + 17),
            new Color(-150205428, true)
         );
         FontRenderers.monsterrat
            .drawString(
               context.getMatrices(),
               "cursor pos: " + this.hoverx * 64 + "x  " + this.hovery * 64 + "z",
               (double)(this.consolex + 4),
               (double)(this.consoley1 + 6),
               -1
            );
         if (!track) {
            Render2DEngine.drawRectDumbWay(
               context.getMatrices(),
               (float)this.consolex,
               (float)(this.consoley1 + 20),
               (float)this.consolex1,
               (float)(this.consoley1 + 35),
               new Color(-150205428, true)
            );
            FontRenderers.monsterrat.drawString(context.getMatrices(), "tracker off", (double)(this.consolex + 4), (double)(this.consoley1 + 26), -1);
         } else {
            Render2DEngine.drawRectDumbWay(
               context.getMatrices(),
               (float)this.consolex,
               (float)(this.consoley1 + 20),
               (float)this.consolex1,
               (float)(this.consoley1 + 35),
               new Color(-144810402, true)
            );
            FontRenderers.monsterrat.drawString(context.getMatrices(), "tracker on", (double)(this.consolex + 4), (double)(this.consoley1 + 26), -1);
         }

         Render2DEngine.drawRectDumbWay(
            context.getMatrices(),
            (float)this.consolex,
            (float)(this.consoley1 + 38),
            (float)this.consolex1,
            (float)(this.consoley1 + 53),
            new Color(-150205428, true)
         );
         FontRenderers.monsterrat.drawString(context.getMatrices(), "clear console", (double)(this.consolex + 4), (double)(this.consoley1 + 42), -1);
         Render2DEngine.drawRectDumbWay(
            context.getMatrices(), (float)this.radarx, (float)this.radary, (float)this.radarx1, (float)this.radary1, new Color(-535489259, true)
         );
         Render2DEngine.drawRectDumbWay(
            context.getMatrices(),
            (float)this.centerx - 1.0F,
            (float)this.centery - 1.0F,
            (float)this.centerx + 1.0F,
            (float)this.centery + 1.0F,
            new Color(16712451)
         );
         Render2DEngine.drawRectDumbWay(
            context.getMatrices(),
            (float)(Module.mc.player.getX() / 16.0 / 4.0 + (double)this.centerx),
            (float)(Module.mc.player.getZ() / 16.0 / 4.0 + (double)this.centery),
            (float)(Module.mc.player.getX() / 16.0 / 4.0 + (double)((float)(this.radarx1 - this.radarx) / 300.0F) + (double)this.centerx),
            (float)(Module.mc.player.getZ() / 16.0 / 4.0 + (double)((float)(this.radary1 - this.radary) / 300.0F) + (double)this.centery),
            new Color(4863)
         );
         if (mouseX > this.radarx && mouseX < this.radarx1 && mouseY > this.radary && mouseY < this.radary1) {
            this.hoverx = mouseX - this.centerx;
            this.hovery = mouseY - this.centery;
         }

         Render2DEngine.addWindow(context.getMatrices(), (float)this.consolex, (float)this.consoley, (float)this.consolex1, (float)(this.consoley1 - 10), 1.0);

         for (String out : new ArrayList<>(this.consoleout)) {
            FontRenderers.monsterrat
               .drawString(
                  context.getMatrices(), out, (double)(this.consolex + 4), (double)(this.consoley + 6 + this.consoleout.indexOf(out) * 11 + this.wheely), -1
               );
         }

         Render2DEngine.popWindow();
         FontRenderers.monsterrat.drawString(context.getMatrices(), "X+", (double)(this.radarx1 + 5), (double)this.centery, -1);
         FontRenderers.monsterrat.drawString(context.getMatrices(), "X-", (double)(this.radarx - 15), (double)this.centery, -1);
         FontRenderers.monsterrat.drawString(context.getMatrices(), "Y+", (double)this.centerx, (double)(this.radary1 + 5), -1);
         FontRenderers.monsterrat.drawString(context.getMatrices(), "Y-", (double)this.centerx, (double)(this.radary - 8), -1);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (mouseX > (double)this.radarx && mouseX < (double)this.radarx1 && mouseY > (double)this.radary && mouseY < (double)this.radary1) {
         busy = true;
         this.searchx = (int)(mouseX - (double)this.centerx);
         this.searchy = (int)(mouseY - (double)this.centery);
         this.consoleout.add("Selected pos " + this.searchx * 65 + "x " + this.searchy * 64 + "z ");
      }

      if (mouseX > (double)this.consolex && mouseX < (double)this.consolex1 && mouseY > (double)(this.consoley1 + 20) && mouseY < (double)(this.consoley1 + 36)
         )
       {
         track = !track;
      }

      if (mouseX > (double)this.consolex && mouseX < (double)this.consolex1 && mouseY > (double)(this.consoley1 + 38) && mouseY < (double)(this.consoley1 + 53)
         )
       {
         this.consoleout.clear();
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.wheely += (int)(verticalAmount * 5.0);
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }
}
