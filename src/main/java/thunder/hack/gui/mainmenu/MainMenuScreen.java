package thunder.hack.gui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.jetbrains.annotations.NotNull;
import thunder.hack.api.IAddon;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.ThunderUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

public class MainMenuScreen extends Screen {
   private final List<MainMenuButton> buttons = new ArrayList<>();
   public boolean confirm = false;
   public static int ticksActive;
   private static MainMenuScreen INSTANCE = new MainMenuScreen();

   protected MainMenuScreen() {
      super(Text.of("THMainMenuScreen"));
      INSTANCE = this;
      this.buttons
         .add(
            new MainMenuButton(
               -110.0F,
               -70.0F,
               I18n.translate("menu.singleplayer", new Object[0]).toUpperCase(Locale.ROOT),
               () -> Module.mc.setScreen(new SelectWorldScreen(this))
            )
         );
      this.buttons
         .add(
            new MainMenuButton(
               4.0F,
               -70.0F,
               I18n.translate("menu.multiplayer", new Object[0]).toUpperCase(Locale.ROOT),
               () -> Module.mc.setScreen(new MultiplayerScreen(this))
            )
         );
      this.buttons
         .add(
            new MainMenuButton(
               -110.0F,
               -29.0F,
               I18n.translate("menu.options", new Object[0]).toUpperCase(Locale.ROOT).replace(".", ""),
               () -> Module.mc.setScreen(new OptionsScreen(this, Module.mc.options))
            )
         );
      this.buttons.add(new MainMenuButton(4.0F, -29.0F, "CLICKGUI", () -> ModuleManager.clickGui.setGui()));
      this.buttons
         .add(
            new MainMenuButton(-110.0F, 12.0F, I18n.translate("menu.quit", new Object[0]).toUpperCase(Locale.ROOT), () -> Module.mc.scheduleStop(), true)
         );
   }

   public static MainMenuScreen getInstance() {
      ticksActive = 0;
      if (INSTANCE == null) {
         INSTANCE = new MainMenuScreen();
      }

      return INSTANCE;
   }

   public void tick() {
      if (++ticksActive > 400) {
         ticksActive = 0;
      }
   }

   public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
      float halfOfWidth = (float)Module.mc.getWindow().getScaledWidth() / 2.0F;
      float halfOfHeight = (float)Module.mc.getWindow().getScaledHeight() / 2.0F;
      float mainX = halfOfWidth - 120.0F;
      float mainY = halfOfHeight - 80.0F;
      float mainWidth = 240.0F;
      float mainHeight = 140.0F;
      this.renderBackground(context, mouseX, mouseY, delta);
      Render2DEngine.drawHudBase(context.getMatrices(), mainX, mainY, mainWidth, mainHeight, 20.0F);
      this.buttons.forEach(b -> b.onRender(context, (float)mouseX, (float)mouseY));
      boolean hoveredLogo = Render2DEngine.isHovered(
         (double)mouseX, (double)mouseY, (double)((int)(halfOfWidth - 120.0F)), (double)((int)(halfOfHeight - 130.0F)), 210.0, 50.0
      );
      FontRenderers.thglitchBig
         .drawCenteredString(
            context.getMatrices(),
            "THUNDERHACK",
            (double)((int)halfOfWidth),
            (double)((int)(halfOfHeight - 120.0F)),
            new Color(255, 255, 255, hoveredLogo ? 230 : 180).getRGB()
         );
      boolean hovered = Render2DEngine.isHovered((double)mouseX, (double)mouseY, (double)(halfOfWidth - 50.0F), (double)(halfOfHeight + 70.0F), 100.0, 10.0);
      FontRenderers.sf_medium
         .drawCenteredString(
            context.getMatrices(),
            "<-- Back to default menu",
            (double)halfOfWidth,
            (double)(halfOfHeight + 70.0F),
            hovered ? -1 : Render2DEngine.applyOpacity(-1, 0.6F)
         );
      String onlineUsers = String.format("online: %s%s", Formatting.DARK_GREEN, Managers.TELEMETRY.getOnlinePlayers().size());
      FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), onlineUsers, (double)halfOfWidth, (double)(halfOfHeight * 2.0F - 15.0F), Color.GREEN);
      context.getMatrices().push();
      context.getMatrices().translate(halfOfWidth - 10.0F - FontRenderers.sf_medium.getStringWidth(onlineUsers) / 2.0F, halfOfHeight * 2.0F - 17.0F, 0.0F);
      Render2DEngine.drawBloom(context.getMatrices(), Render2DEngine.applyOpacity(Color.GREEN, 0.6F), 9.0F);
      context.getMatrices().pop();
      context.getMatrices().push();
      context.getMatrices().translate(halfOfWidth - 10.0F - FontRenderers.sf_medium.getStringWidth(onlineUsers) / 2.0F, halfOfHeight * 2.0F - 17.0F, 0.0F);
      Render2DEngine.drawBloom(
         context.getMatrices(), Render2DEngine.applyOpacity(Color.GREEN, (float)(0.5 + Math.sin((double)System.currentTimeMillis() / 500.0) / 2.0)), 9.0F
      );
      context.getMatrices().pop();
      Render2DEngine.drawHudBase(
         context.getMatrices(),
         (float)(Module.mc.getWindow().getScaledWidth() - 40),
         (float)(Module.mc.getWindow().getScaledHeight() - 40),
         30.0F,
         30.0F,
         5.0F,
         Render2DEngine.isHovered(
               (double)mouseX,
               (double)mouseY,
               (double)(Module.mc.getWindow().getScaledWidth() - 40),
               (double)(Module.mc.getWindow().getScaledHeight() - 40),
               30.0,
               30.0
            )
            ? 0.7F
            : 1.0F
      );
      RenderSystem.setShaderColor(
         1.0F,
         1.0F,
         1.0F,
         Render2DEngine.isHovered(
               (double)mouseX,
               (double)mouseY,
               (double)(Module.mc.getWindow().getScaledWidth() - 40),
               (double)(Module.mc.getWindow().getScaledHeight() - 40),
               30.0,
               30.0
            )
            ? 0.7F
            : 1.0F
      );
      context.drawTexture(
         TextureStorage.thTeam, Module.mc.getWindow().getScaledWidth() - 40, Module.mc.getWindow().getScaledHeight() - 40, 30, 30, 0.0F, 0.0F, 30, 30, 30, 30
      );
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      Render2DEngine.drawHudBase(
         context.getMatrices(),
         (float)(Module.mc.getWindow().getScaledWidth() - 80),
         (float)(Module.mc.getWindow().getScaledHeight() - 40),
         30.0F,
         30.0F,
         5.0F,
         Render2DEngine.isHovered(
               (double)mouseX,
               (double)mouseY,
               (double)(Module.mc.getWindow().getScaledWidth() - 80),
               (double)(Module.mc.getWindow().getScaledHeight() - 40),
               30.0,
               30.0
            )
            ? 0.7F
            : 1.0F
      );
      RenderSystem.setShaderColor(
         1.0F,
         1.0F,
         1.0F,
         Render2DEngine.isHovered(
               (double)mouseX,
               (double)mouseY,
               (double)(Module.mc.getWindow().getScaledWidth() - 80),
               (double)(Module.mc.getWindow().getScaledHeight() - 40),
               30.0,
               30.0
            )
            ? 0.7F
            : 1.0F
      );
      context.drawTexture(
         TextureStorage.donation, Module.mc.getWindow().getScaledWidth() - 79, Module.mc.getWindow().getScaledHeight() - 39, 28, 28, 0.0F, 0.0F, 30, 30, 30, 30
      );
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int offsetY = 10;

      for (String change : ThunderUtility.changeLog) {
         String prefix = getPrefix(change);
         FontRenderers.sf_medium.drawString(context.getMatrices(), prefix, 10.0, (double)offsetY, Render2DEngine.applyOpacity(-1, 0.4F));
         offsetY += 10;
      }

      int totalAddonsLoaded = Managers.ADDON.getTotalAddons();
      String addonsText = "Addons Loaded: " + totalAddonsLoaded;
      int screenWidth = Module.mc.getWindow().getScaledWidth();
      int textWidth = (int)FontRenderers.sf_bold.getStringWidth(addonsText);
      int textX = screenWidth - textWidth - 5;
      FontRenderers.sf_bold.drawString(context.getMatrices(), addonsText, (double)textX, 5.0, Color.WHITE.getRGB());
      int offset = 0;

      for (IAddon addon : Managers.ADDON.getAddons()) {
         textWidth = (int)FontRenderers.sf_bold.getStringWidth(addon.getName() + " |");
         textX = screenWidth - textWidth - 5;
         FontRenderers.sf_bold
            .drawString(context.getMatrices(), addon.getName() + Formatting.WHITE + " |", (double)textX, (double)(13 + offset), Color.GRAY.getRGB());
         offset += 9;
      }
   }

   @NotNull
   private static String getPrefix(@NotNull String change) {
      Object prefix = "";
      if (change.contains("[+]")) {
         change = change.replace("[+] ", "");
         prefix = Formatting.GREEN + "[+] " + Formatting.RESET;
      } else if (change.contains("[-]")) {
         change = change.replace("[-] ", "");
         prefix = Formatting.RED + "[-] " + Formatting.RESET;
      } else if (change.contains("[/]")) {
         change = change.replace("[/] ", "");
         prefix = Formatting.LIGHT_PURPLE + "[/] " + Formatting.RESET;
      } else if (change.contains("[*]")) {
         change = change.replace("[*] ", "");
         prefix = Formatting.GOLD + "[*] " + Formatting.RESET;
      }

      return (String)prefix + change;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float halfOfWidth = (float)Module.mc.getWindow().getScaledWidth() / 2.0F;
      float halfOfHeight = (float)Module.mc.getWindow().getScaledHeight() / 2.0F;
      this.buttons.forEach(b -> b.onClick((int)mouseX, (int)mouseY));
      if (Render2DEngine.isHovered(mouseX, mouseY, (double)(halfOfWidth - 50.0F), (double)(halfOfHeight + 70.0F), 100.0, 10.0)) {
         this.confirm = true;
         Module.mc.setScreen(new TitleScreen());
         this.confirm = false;
      }

      if (Render2DEngine.isHovered(
         mouseX, mouseY, (double)(Module.mc.getWindow().getScaledWidth() - 40), (double)(Module.mc.getWindow().getScaledHeight() - 40), 40.0, 40.0
      )) {
         Module.mc.setScreen(CreditsScreen.getInstance());
      }

      if (Render2DEngine.isHovered(
         mouseX, mouseY, (double)(Module.mc.getWindow().getScaledWidth() - 90), (double)(Module.mc.getWindow().getScaledHeight() - 40), 40.0, 40.0
      )) {
         Util.getOperatingSystem().open(URI.create("https://www.donationalerts.com/r/06ed/"));
      }

      if (Render2DEngine.isHovered(mouseX, mouseY, (double)((int)(halfOfWidth - 157.0F)), (double)((int)(halfOfHeight - 140.0F)), 300.0, 70.0)) {
         Util.getOperatingSystem().open(URI.create("https://thunderhack-site.vercel.app/"));
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }
}
