package thunder.hack.gui.mainmenu;

import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.render.Render2DEngine;

public class MainMenuButton {
   private final float posX;
   private final float posY;
   private final float width;
   private final float height;
   private final String name;
   private final Runnable action;

   public MainMenuButton(float posX, float posY, @NotNull String name, Runnable action, boolean isExit) {
      this.name = name;
      this.posX = posX;
      this.posY = posY;
      this.action = action;
      this.width = isExit ? 222.0F : 107.0F;
      this.height = 38.0F;
   }

   public MainMenuButton(float posX, float posY, @NotNull String name, Runnable action) {
      this(posX, posY, name, action, false);
   }

   public void onRender(DrawContext context, float mouseX, float mouseY) {
      float halfOfWidth = (float)Module.mc.getWindow().getScaledWidth() / 2.0F;
      float halfOfHeight = (float)Module.mc.getWindow().getScaledHeight() / 2.0F;
      Render2DEngine.drawHudBase(context.getMatrices(), halfOfWidth + this.posX, halfOfHeight + this.posY, this.width, this.height, 10.0F);
      boolean hovered = Render2DEngine.isHovered(
         (double)mouseX, (double)mouseY, (double)(halfOfWidth + this.posX), (double)(halfOfHeight + this.posY), (double)this.width, (double)this.height
      );
      FontRenderers.monsterrat
         .drawCenteredString(
            context.getMatrices(),
            this.name,
            (double)(halfOfWidth + this.posX + this.width / 2.0F),
            (double)(halfOfHeight + this.posY + this.height / 2.0F - 3.0F),
            hovered ? -1 : Render2DEngine.applyOpacity(-1, 0.7F)
         );
   }

   public void onClick(int mouseX, int mouseY) {
      float halfOfWidth = (float)Module.mc.getWindow().getScaledWidth() / 2.0F;
      boolean hovered = Render2DEngine.isHovered(
         (double)mouseX,
         (double)mouseY,
         (double)(halfOfWidth + this.posX),
         (double)((float)Module.mc.getWindow().getScaledHeight() / 2.0F + this.posY),
         (double)this.width,
         (double)this.height
      );
      if (hovered) {
         this.action.run();
      }
   }
}
