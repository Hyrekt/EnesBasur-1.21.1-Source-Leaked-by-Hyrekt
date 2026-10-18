package thunder.hack.gui.clickui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.List;
import java.util.Objects;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.ClickGui;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.clickui.impl.SearchBar;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;
import thunder.hack.utility.render.animation.EaseOutBack;

public class ClickGUI extends Screen {
   public static List<AbstractCategory> windows;
   public static boolean anyHovered;
   private boolean firstOpen;
   private float scrollY;
   private float closeAnimation;
   private float prevYaw;
   private float prevPitch;
   private float closeDirectionX;
   private float closeDirectionY;
   public static boolean close = false;
   public static boolean imageDirection;
   public static String currentDescription = "";
   public EaseOutBack imageAnimation = new EaseOutBack(6);
   private static ClickGUI INSTANCE = new ClickGUI();
   public SearchBar searchBar = new SearchBar();

   public ClickGUI() {
      super(Text.of("NewClickGUI"));
      windows = Lists.newArrayList();
      this.firstOpen = true;
      this.setInstance();
   }

   public static ClickGUI getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClickGUI();
      }

      imageDirection = true;
      return INSTANCE;
   }

   public static ClickGUI getClickGui() {
      windows.forEach(AbstractCategory::init);
      return getInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   protected void init() {
      if (this.firstOpen) {
         float offset = 0.0F;
         int windowHeight = 18;
         int halfWidth = Module.mc.getWindow().getScaledWidth() / 2;
         int halfWidthCats = (int)(
            ((float)Module.Category.values().size() - 1.0F) / 2.0F * ((float)ModuleManager.clickGui.moduleWidth.getValue().intValue() + 4.0F)
         );

         for (Module.Category category : Managers.MODULE.getCategories()) {
            if (category != Module.Category.HUD) {
               Category window = new Category(
                  category, Managers.MODULE.getModulesByCategory(category), (float)(halfWidth - halfWidthCats) + offset, 20.0F, 100.0F, (float)windowHeight
               );
               window.setOpen(true);
               windows.add(window);
               if ((offset += (float)(ModuleManager.clickGui.moduleWidth.getValue() + 2)) > (float)Module.mc.getWindow().getScaledWidth()) {
                  offset = 0.0F;
               }
            }
         }

         this.firstOpen = false;
      } else if (windows.getFirst().getX() < 0.0F || windows.getFirst().getY() < 0.0F) {
         float offset = 0.0F;
         int halfWidth = Module.mc.getWindow().getScaledWidth() / 2;
         int halfWidthCats = (int)(3.0F * ((float)ModuleManager.clickGui.moduleWidth.getValue().intValue() + 4.0F));

         for (AbstractCategory w : windows) {
            w.setX((float)(halfWidth - halfWidthCats) + offset);
            w.setY(20.0F);
            if ((offset += (float)(ModuleManager.clickGui.moduleWidth.getValue() + 2)) > (float)Module.mc.getWindow().getScaledWidth()) {
               offset = 0.0F;
            }
         }
      }

      windows.forEach(AbstractCategory::init);
   }

   public boolean shouldPause() {
      return false;
   }

   public void tick() {
      windows.forEach(AbstractCategory::tick);
      this.imageAnimation.update(imageDirection);
      if (close) {
         if (Module.mc.player != null) {
            if (Module.mc.player.getPitch() > this.prevPitch) {
               this.closeDirectionY = (this.prevPitch - Module.mc.player.getPitch()) * 300.0F;
            }

            if (Module.mc.player.getPitch() < this.prevPitch) {
               this.closeDirectionY = (this.prevPitch - Module.mc.player.getPitch()) * 300.0F;
            }

            if (Module.mc.player.getYaw() > this.prevYaw) {
               this.closeDirectionX = (this.prevYaw - Module.mc.player.getYaw()) * 300.0F;
            }

            if (Module.mc.player.getYaw() < this.prevYaw) {
               this.closeDirectionX = (this.prevYaw - Module.mc.player.getYaw()) * 300.0F;
            }
         }

         if (this.closeDirectionX < 1.0F && this.closeDirectionY < 1.0F && this.closeAnimation > 2.0F) {
            this.closeDirectionY = -3000.0F;
         }

         this.closeAnimation++;
         if (this.closeAnimation > 6.0F) {
            close = false;
            windows.forEach(AbstractCategory::restorePos);
            this.close();
         }
      }

      if (this.searchBar != null) {
         this.searchBar.tick();
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (ModuleManager.clickGui.blur.getValue()) {
         this.applyBlur(delta);
      }

      anyHovered = false;
      ClickGui.Image image = ModuleManager.clickGui.image.getValue();
      if (image != ClickGui.Image.None) {
         RenderSystem.setShaderTexture(0, image.file);
         Render2DEngine.renderTexture(
            context.getMatrices(),
            (double)Module.mc.getWindow().getScaledWidth() - (double)image.fileWidth * this.imageAnimation.getAnimationd(),
            (double)(Module.mc.getWindow().getScaledHeight() - image.fileHeight),
            (double)image.fileWidth,
            (double)image.fileHeight,
            0.0F,
            0.0F,
            (double)image.fileWidth,
            (double)image.fileHeight,
            (double)image.fileWidth,
            (double)image.fileHeight
         );
      }

      if (this.closeAnimation <= 6.0F) {
         windows.forEach(w -> {
            w.setX(w.getX() + this.closeDirectionX * AnimationUtility.deltaTime());
            w.setY(w.getY() + this.closeDirectionY * AnimationUtility.deltaTime());
         });
      }

      if (Module.fullNullCheck()) {
         this.renderBackground(context, mouseX, mouseY, delta);
      }

      if (ModuleManager.clickGui.scrollMode.getValue() == ClickGui.scrollModeEn.Old) {
         for (AbstractCategory window : windows) {
            if (InputUtil.isKeyPressed(Module.mc.getWindow().getHandle(), 264)) {
               window.setY(window.getY() + 2.0F);
            }

            if (InputUtil.isKeyPressed(Module.mc.getWindow().getHandle(), 265)) {
               window.setY(window.getY() - 2.0F);
            }

            if (InputUtil.isKeyPressed(Module.mc.getWindow().getHandle(), 262)) {
               window.setX(window.getX() + 2.0F);
            }

            if (InputUtil.isKeyPressed(Module.mc.getWindow().getHandle(), 263)) {
               window.setX(window.getX() - 2.0F);
            }

            if (this.scrollY != 0.0F) {
               window.setY(window.getY() + this.scrollY);
            }
         }
      } else {
         for (AbstractCategory window : windows) {
            if (this.scrollY != 0.0F) {
               window.setModuleOffset(this.scrollY, (float)mouseX, (float)mouseY);
            }
         }
      }

      this.scrollY = 0.0F;
      windows.forEach(w -> w.render(context, mouseX, mouseY, delta));
      if (!Objects.equals(currentDescription, "") && ModuleManager.clickGui.descriptions.getValue()) {
         Render2DEngine.drawHudBase(
            context.getMatrices(),
            (float)(mouseX + 7),
            (float)(mouseY + 5),
            FontRenderers.sf_medium.getStringWidth(currentDescription) + 6.0F,
            11.0F,
            1.0F,
            false
         );
         FontRenderers.sf_medium
            .drawString(context.getMatrices(), currentDescription, (double)(mouseX + 10), (double)(mouseY + 8), HudEditor.getColor(0).getRGB());
         currentDescription = "";
      }

      if (this.searchBar != null) {
         this.searchBar.setX((float)Module.mc.getWindow().getScaledWidth() / 2.0F - 75.0F);
         this.searchBar.setY((float)(Module.mc.getWindow().getScaledHeight() - 22));
         this.searchBar.setWidth(150.0F);
         this.searchBar.setHeight(14.0F);
         this.searchBar.render(context, mouseX, mouseY, delta);
         FontRenderers.sf_medium_mini
            .drawString(
               context.getMatrices(),
               "Left Click: Toggle | Right Click: Settings | Middle Click: Bind | Ctrl+F: Search",
               (double)((float)Module.mc.getWindow().getScaledWidth() / 2.0F - 350.0F),
               (double)(Module.mc.getWindow().getScaledHeight() - 12),
               new Color(150, 150, 150).getRGB()
            );
      }

      if (!HudElement.anyHovered && !anyHovered && GLFW.glfwGetPlatform() != 393219) {
         GLFW.glfwSetCursor(Module.mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221185));
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.scrollY += (float)((int)(verticalAmount * 5.0));
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      windows.forEach(w -> {
         w.mouseClicked((int)mouseX, (int)mouseY, button);
         windows.forEach(w1 -> {
            if (w.dragging && w != w1) {
               w1.dragging = false;
            }
         });
      });
      if (this.searchBar != null) {
         this.searchBar.mouseClicked((int)mouseX, (int)mouseY, button);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      windows.forEach(w -> w.mouseReleased((int)mouseX, (int)mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean charTyped(char key, int modifier) {
      if (this.searchBar != null) {
         this.searchBar.charTyped(key, modifier);
      }

      windows.forEach(w -> w.charTyped(key, modifier));
      return true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      windows.forEach(w -> w.keyTyped(keyCode));
      if (this.searchBar != null) {
         this.searchBar.keyTyped(keyCode);
      }

      if (keyCode == 256 && SearchBar.listening) {
         SearchBar.listening = false;
         return true;
      } else if (keyCode == 256) {
         if (Module.mc.player == null || !ModuleManager.clickGui.closeAnimation.getValue()) {
            imageDirection = false;
            this.imageAnimation.reset();
            super.keyPressed(keyCode, scanCode, modifiers);
            return true;
         } else if (close) {
            return true;
         } else {
            imageDirection = false;
            windows.forEach(AbstractCategory::savePos);
            this.closeDirectionX = 0.0F;
            this.closeDirectionY = 0.0F;
            close = true;
            Module.mc.mouse.lockCursor();
            this.closeAnimation = 0.0F;
            if (Module.mc.player != null) {
               this.prevYaw = Module.mc.player.getYaw();
               this.prevPitch = Module.mc.player.getPitch();
            }

            return true;
         }
      } else {
         return false;
      }
   }
}
