package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Optional;
import net.minecraft.util.Formatting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

public class ItemESP extends Module {
   private final Setting<Boolean> shadow = new Setting<>("Shadow", true);
   private final Setting<ColorSetting> scolor = new Setting<>("ShadowColor", new ColorSetting(new Color(0).getRGB()));
   private final Setting<ColorSetting> tcolor = new Setting<>("TextColor", new ColorSetting(new Color(-1).getRGB()));
   private final Setting<ItemESP.ESPMode> espMode = new Setting<>("Mode", ItemESP.ESPMode.Rect);
   private final Setting<Float> radius = new Setting<>("Radius", 1.0F, 0.1F, 5.0F, v -> this.espMode.getValue() == ItemESP.ESPMode.Circle);
   private final Setting<Boolean> useHudColor = new Setting<>("UseHudColor", true, v -> this.espMode.getValue() == ItemESP.ESPMode.Circle);
   private final Setting<Integer> cOffset = new Setting<>(
      "ColorOffset", 2, 1, 50, v -> this.espMode.getValue() == ItemESP.ESPMode.Circle && this.useHudColor.getValue()
   );
   private final Setting<ColorSetting> circleColor = new Setting<>(
      "CircleColor", new ColorSetting(new Color(-1).getRGB()), v -> this.espMode.getValue() == ItemESP.ESPMode.Circle && !this.useHudColor.getValue()
   );
   private final Setting<Integer> cPoints = new Setting<>("CirclePoints", 12, 3, 32, v -> this.espMode.getValue() == ItemESP.ESPMode.Circle);

   public ItemESP() {
      super("ItemESP", Module.Category.RENDER);
   }

   @Override
   public void onRender2D(DrawContext context) {
      if (mc.world != null) {
         Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
         BufferBuilder bufferBuilder = null;
         if (this.espMode.getValue() == ItemESP.ESPMode.Rect) {
            Render2DEngine.setupRender();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         }

         for (Entity ent : mc.world.getEntities()) {
            if (ent instanceof ItemEntity) {
               ItemEntity itemEntity = (ItemEntity)ent;
               Vec3d[] vectors = getPoints(itemEntity);
               Vector4d position = null;

               for (Vec3d vector : vectors) {
                  vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
                  if (vector.z > 0.0 && vector.z < 1.0) {
                     if (position == null) {
                        position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
                     }

                     position.x = Math.min(vector.x, position.x);
                     position.y = Math.min(vector.y, position.y);
                     position.z = Math.max(vector.x, position.z);
                     position.w = Math.max(vector.y, position.w);
                  }
               }

               if (position != null) {
                  float posX = (float)position.x;
                  float posY = (float)position.y;
                  float endPosX = (float)position.z;
                  float endPosY = (float)position.w;
                  float diff = (endPosX - posX) / 2.0F;
                  ItemStack stack = itemEntity.getStack();
                  Object name = this.getLegacyName(stack.getName());
                  if (stack.getCount() > 1) {
                     name = (String)name + " \u00a77x" + stack.getCount();
                  }

                  name = ((String)name).replace('&', '\u00a7');
                  float textWidth = FontRenderers.sf_bold_mini.getStringWidth((String)name);
                  float tagX = posX + diff - textWidth / 2.0F;
                  if (this.shadow.getValue()) {
                     Render2DEngine.drawBlurredShadow(
                        context.getMatrices(), tagX - 2.0F, posY - 13.0F, textWidth + 4.0F, 11.0F, 14, this.scolor.getValue().getColorObject()
                     );
                  }

                  Render2DEngine.drawRect(context.getMatrices(), tagX - 2.0F, posY - 12.0F, textWidth + 4.0F, 10.0F, new Color(0, 0, 0, 160));
                  FontRenderers.sf_bold_mini
                     .drawString(context.getMatrices(), (String)name, (double)tagX, (double)(posY - 10.0F), this.tcolor.getValue().getColor());
                  if (bufferBuilder != null) {
                     this.drawRect(bufferBuilder, matrix, posX, posY, endPosX, endPosY);
                  }
               }
            }
         }

         if (bufferBuilder != null) {
            Render2DEngine.endBuilding(bufferBuilder);
            Render2DEngine.endRender();
         }
      }
   }

   private String getLegacyName(Text text) {
      StringBuilder sb = new StringBuilder();
      text.visit((style, string) -> {
         Formatting formatting = null;
         if (style.getColor() != null) {
            String colorName = style.getColor().getName();

            for (Formatting f : Formatting.values()) {
               if (f.getName().equalsIgnoreCase(colorName)) {
                  formatting = f;
                  break;
               }
            }

            if (formatting == null) {
               int rgb = style.getColor().getRgb();
               formatting = this.getNearestFormatting(rgb);
            }
         }

         if (formatting != null) {
            sb.append(formatting);
         }

         if (style.isBold()) {
            sb.append(Formatting.BOLD);
         }

         if (style.isItalic()) {
            sb.append(Formatting.ITALIC);
         }

         if (style.isUnderlined()) {
            sb.append(Formatting.UNDERLINE);
         }

         if (style.isStrikethrough()) {
            sb.append(Formatting.STRIKETHROUGH);
         }

         if (style.isObfuscated()) {
            sb.append(Formatting.OBFUSCATED);
         }

         sb.append(string);
         return Optional.empty();
      }, Style.EMPTY);
      return sb.toString();
   }

   private Formatting getNearestFormatting(int rgb) {
      Formatting nearest = Formatting.WHITE;
      double minDistance = Double.MAX_VALUE;
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;

      for (Formatting f : Formatting.values()) {
         Integer fRgb;
         if (f.isColor() && (fRgb = f.getColorValue()) != null) {
            int fr = fRgb >> 16 & 0xFF;
            int fg = fRgb >> 8 & 0xFF;
            int fb = fRgb & 0xFF;
            double dist = Math.sqrt(Math.pow((double)(r - fr), 2.0) + Math.pow((double)(g - fg), 2.0) + Math.pow((double)(b - fb), 2.0));
            if (dist < minDistance) {
               minDistance = dist;
               nearest = f;
            }
         }
      }

      return nearest;
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (this.espMode.getValue() == ItemESP.ESPMode.Circle) {
         for (Entity ent : mc.world.getEntities()) {
            if (ent instanceof ItemEntity) {
               Render3DEngine.drawCircle3D(
                  stack,
                  ent,
                  this.radius.getValue(),
                  this.circleColor.getValue().getColor(),
                  this.cPoints.getValue(),
                  this.useHudColor.getValue(),
                  this.cOffset.getValue()
               );
            }
         }
      }
   }

   private void drawRect(BufferBuilder bufferBuilder, Matrix4f stack, float posX, float posY, float endPosX, float endPosY) {
      Color black = Color.BLACK;
      Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 1.0F, posY, posX + 0.5F, endPosY + 0.5F, black, black, black, black);
      Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 1.0F, posY - 0.5F, endPosX + 0.5F, posY + 1.0F, black, black, black, black);
      Render2DEngine.setRectPoints(bufferBuilder, stack, endPosX - 1.0F, posY, endPosX + 0.5F, endPosY + 0.5F, black, black, black, black);
      Render2DEngine.setRectPoints(bufferBuilder, stack, posX - 1.0F, endPosY - 1.0F, endPosX + 0.5F, endPosY + 0.5F, black, black, black, black);
      Render2DEngine.setRectPoints(
         bufferBuilder, stack, posX - 0.5F, posY, posX, endPosY, HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(0), HudEditor.getColor(270)
      );
      Render2DEngine.setRectPoints(
         bufferBuilder,
         stack,
         posX,
         endPosY - 0.5F,
         endPosX,
         endPosY,
         HudEditor.getColor(0),
         HudEditor.getColor(180),
         HudEditor.getColor(180),
         HudEditor.getColor(0)
      );
      Render2DEngine.setRectPoints(
         bufferBuilder,
         stack,
         posX - 0.5F,
         posY,
         endPosX,
         posY + 0.5F,
         HudEditor.getColor(180),
         HudEditor.getColor(90),
         HudEditor.getColor(90),
         HudEditor.getColor(180)
      );
      Render2DEngine.setRectPoints(
         bufferBuilder,
         stack,
         endPosX - 0.5F,
         posY,
         endPosX,
         endPosY,
         HudEditor.getColor(90),
         HudEditor.getColor(270),
         HudEditor.getColor(270),
         HudEditor.getColor(90)
      );
   }

   @NotNull
   private static Vec3d[] getPoints(Entity ent) {
      Box axisAlignedBB = getBox(ent);
      return new Vec3d[]{
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ),
         new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)
      };
   }

   @NotNull
   private static Box getBox(Entity ent) {
      double x = ent.prevX + (ent.getX() - ent.prevX) * (double)Render3DEngine.getTickDelta();
      double y = ent.prevY + (ent.getY() - ent.prevY) * (double)Render3DEngine.getTickDelta();
      double z = ent.prevZ + (ent.getZ() - ent.prevZ) * (double)Render3DEngine.getTickDelta();
      Box axisAlignedBB2 = ent.getBoundingBox();
      return new Box(
         axisAlignedBB2.minX - ent.getX() + x - 0.05,
         axisAlignedBB2.minY - ent.getY() + y,
         axisAlignedBB2.minZ - ent.getZ() + z - 0.05,
         axisAlignedBB2.maxX - ent.getX() + x + 0.05,
         axisAlignedBB2.maxY - ent.getY() + y + 0.15,
         axisAlignedBB2.maxZ - ent.getZ() + z + 0.05
      );
   }

   private static enum ESPMode {
      Rect,
      Circle,
      None;
   }
}
