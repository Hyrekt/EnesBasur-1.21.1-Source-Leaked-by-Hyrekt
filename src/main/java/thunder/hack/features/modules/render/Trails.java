package thunder.hack.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.world.RaycastContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.interfaces.IEntity;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.TextureStorage;

public class Trails extends Module {
   private final Setting<Boolean> xp = new Setting<>("Xp", false);
   private final Setting<Trails.Particles> pearls = new Setting<>("Pearls", Trails.Particles.Particles);
   private final Setting<Trails.Particles> arrows = new Setting<>("Arrows", Trails.Particles.Particles);
   private final Setting<Trails.Players> players = new Setting<>("Players", Trails.Players.Particles);
   private final Setting<Boolean> onlySelf = new Setting<>("OnlySelf", false, v -> this.players.getValue() != Trails.Players.None);
   private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(-2013200640));
   private final Setting<Float> down = new Setting<>("Down", 0.5F, 0.0F, 2.0F);
   private final Setting<Float> width = new Setting<>("Height", 1.3F, 0.1F, 2.0F);
   private final Setting<Integer> speed = new Setting<>(
      "Speed",
      2,
      1,
      20,
      v -> this.players.is(Trails.Players.Particles) || this.arrows.is(Trails.Particles.Particles) || this.pearls.is(Trails.Particles.Particles)
   );
   private final Setting<HitParticles.Mode> mode = new Setting<>("Mode", HitParticles.Mode.Stars);
   private final Setting<HitParticles.Physics> physics = new Setting<>(
      "Physics",
      HitParticles.Physics.Fall,
      v -> this.players.is(Trails.Players.Particles) || this.arrows.is(Trails.Particles.Particles) || this.pearls.is(Trails.Particles.Particles)
   );
   private final Setting<Integer> starsScale = new Setting<>(
      "Scale",
      3,
      1,
      10,
      v -> this.players.is(Trails.Players.Particles) || this.arrows.is(Trails.Particles.Particles) || this.pearls.is(Trails.Particles.Particles)
   );
   private final Setting<Integer> amount = new Setting<>(
      "Amount",
      2,
      1,
      5,
      v -> this.players.is(Trails.Players.Particles) || this.arrows.is(Trails.Particles.Particles) || this.pearls.is(Trails.Particles.Particles)
   );
   private final Setting<Integer> lifeTime = new Setting<>(
      "LifeTime",
      2,
      1,
      10,
      v -> this.players.is(Trails.Players.Particles) || this.arrows.is(Trails.Particles.Particles) || this.pearls.is(Trails.Particles.Particles)
   );
   private final Setting<Trails.Mode> lmode = new Setting<>("ColorMode", Trails.Mode.Sync);
   private final Setting<ColorSetting> lcolor = new Setting<>("Color2", new ColorSetting(575714484), v -> this.lmode.getValue() == Trails.Mode.Custom);
   private List<Trails.Particle> particles = new ArrayList<>();

   public Trails() {
      super("Trails", Module.Category.RENDER);
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      for (Entity en : Managers.ASYNC.getAsyncEntities()) {
         if (en instanceof EnderPearlEntity && this.pearls.is(Trails.Particles.Trail)) {
            this.calcTrajectory(en);
         }

         if (en instanceof ArrowEntity && this.arrows.is(Trails.Particles.Trail)) {
            this.calcTrajectory(en);
         }

         if (en instanceof ExperienceBottleEntity && this.xp.getValue()) {
            this.calcTrajectory(en);
         }
      }

      if (this.players.getValue() == Trails.Players.Trail) {
         for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player || !this.onlySelf.getValue()) {
               float alpha = (float)this.color.getValue().getAlpha() / 255.0F;
               if (!((IEntity)entity).getTrails().isEmpty()) {
                  stack.push();
                  RenderSystem.disableCull();
                  RenderSystem.enableBlend();
                  RenderSystem.defaultBlendFunc();
                  RenderSystem.enableDepthTest();
                  RenderSystem.depthFunc(515);
                  RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                  BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

                  for (int i = 0; i < ((IEntity)entity).getTrails().size(); i++) {
                     Trails.Trail ctx = ((IEntity)entity).getTrails().get(i);
                     Vec3d pos = ctx.interpolate(Render3DEngine.getTickDelta());
                     bufferBuilder.vertex(
                           stack.peek().getPositionMatrix(), (float)pos.x, (float)pos.y + this.down.getValue(), (float)pos.z
                        )
                        .color(
                           Render2DEngine.injectAlpha(
                                 ((IEntity)entity).getTrails().get(i).color(), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0)
                              )
                              .getRGB()
                        );
                     bufferBuilder.vertex(
                           stack.peek().getPositionMatrix(),
                           (float)pos.x,
                           (float)pos.y + this.width.getValue() + this.down.getValue(),
                           (float)pos.z
                        )
                        .color(
                           Render2DEngine.injectAlpha(
                                 ((IEntity)entity).getTrails().get(i).color(), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0)
                              )
                              .getRGB()
                        );
                  }

                  Render2DEngine.endBuilding(bufferBuilder);
                  Render3DEngine.endRender();
                  RenderSystem.enableCull();
                  RenderSystem.disableDepthTest();
                  stack.pop();
               }
            }
         }
      } else if (this.players.getValue() == Trails.Players.Tail) {
         for (PlayerEntity entityx : mc.world.getPlayers()) {
            if (entityx == mc.player || !this.onlySelf.getValue()) {
               float alpha = (float)this.color.getValue().getAlpha();
               Camera camera = mc.gameRenderer.getCamera();
               stack.push();
               RenderSystem.setShaderTexture(0, TextureStorage.firefly);
               RenderSystem.enableBlend();
               RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(false);
               RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
               BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
               int size = ((IEntity)entityx).getTrails().size();
               if (!((IEntity)entityx).getTrails().isEmpty()) {
                  for (int i = 0; i < size; i++) {
                     Trails.Trail ctx = ((IEntity)entityx).getTrails().get(i);
                     MatrixStack matrices = new MatrixStack();
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                     Vec3d pos = ctx.interpolate(Render3DEngine.getTickDelta());
                     matrices.translate(pos.x, pos.y + 0.9F, pos.z);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                     Matrix4f matrix = matrices.peek().getPositionMatrix();
                     Color col = ctx.color();
                     float sc = 0.6F;
                     float colorFactor = (float)i / (float)size;
                     col = Render2DEngine.interpolateColorC(col, Color.WHITE, (float)Math.pow((double)(1.0F - colorFactor), 2.0));
                     float animPow = (float)ctx.animation(Render3DEngine.getTickDelta());
                     int animatedAlpha = (int)(alpha * animPow);
                     bufferBuilder.vertex(matrix, -sc, sc, 0.0F)
                        .texture(0.0F, 1.0F)
                        .color(Render2DEngine.injectAlpha(col, animatedAlpha).getRGB());
                     bufferBuilder.vertex(matrix, sc, sc, 0.0F)
                        .texture(1.0F, 1.0F)
                        .color(Render2DEngine.injectAlpha(col, animatedAlpha).getRGB());
                     bufferBuilder.vertex(matrix, sc, -sc, 0.0F)
                        .texture(1.0F, 0.0F)
                        .color(Render2DEngine.injectAlpha(col, animatedAlpha).getRGB());
                     bufferBuilder.vertex(matrix, -sc, -sc, 0.0F)
                        .texture(0.0F, 0.0F)
                        .color(Render2DEngine.injectAlpha(col, animatedAlpha).getRGB());
                  }
               }

               Render2DEngine.endBuilding(bufferBuilder);
               RenderSystem.depthMask(true);
               RenderSystem.disableDepthTest();
               RenderSystem.disableBlend();
               stack.pop();
            }
         }
      } else if (this.players.getValue() == Trails.Players.Cute) {
         for (PlayerEntity entityxx : mc.world.getPlayers()) {
            if (entityxx == mc.player || !this.onlySelf.getValue()) {
               float alpha = (float)this.color.getValue().getAlpha() / 255.0F;
               if (!((IEntity)entityxx).getTrails().isEmpty()) {
                  stack.push();
                  RenderSystem.disableCull();
                  RenderSystem.enableBlend();
                  RenderSystem.defaultBlendFunc();
                  RenderSystem.enableDepthTest();
                  RenderSystem.depthFunc(515);
                  float step = (float)(mc.player.getBoundingBox().getLengthY() / 5.0);
                  RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                  BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

                  for (int i = 0; i < ((IEntity)entityxx).getTrails().size(); i++) {
                     Trails.Trail ctx = ((IEntity)entityxx).getTrails().get(i);
                     Vec3d pos = ctx.interpolate(Render3DEngine.getTickDelta());
                     bufferBuilder.vertex(stack.peek().getPositionMatrix(), (float)pos.x, (float)pos.y, (float)pos.z)
                        .color(
                           Render2DEngine.injectAlpha(new Color(69, 221, 255), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0))
                              .getRGB()
                        );
                     bufferBuilder.vertex(stack.peek().getPositionMatrix(), (float)pos.x, (float)pos.y + step, (float)pos.z)
                        .color(
                           Render2DEngine.injectAlpha(new Color(69, 221, 255), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0))
                              .getRGB()
                        );
                  }

                  Render2DEngine.endBuilding(bufferBuilder);
                  RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                  bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

                  for (int var39 = 0; var39 < ((IEntity)entityxx).getTrails().size(); var39++) {
                     Trails.Trail ctx = ((IEntity)entityxx).getTrails().get(var39);
                     Vec3d pos = ctx.interpolate(Render3DEngine.getTickDelta());
                     bufferBuilder.vertex(stack.peek().getPositionMatrix(), (float)pos.x, (float)pos.y + step, (float)pos.z)
                        .color(
                           Render2DEngine.injectAlpha(new Color(248, 139, 160), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0))
                              .getRGB()
                        );
                     bufferBuilder.vertex(
                           stack.peek().getPositionMatrix(), (float)pos.x, (float)pos.y + step * 2.0F, (float)pos.z
                        )
                        .color(
                           Render2DEngine.injectAlpha(new Color(248, 139, 160), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0))
                              .getRGB()
                        );
                  }

                  Render2DEngine.endBuilding(bufferBuilder);
                  RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                  bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

                  for (int var40 = 0; var40 < ((IEntity)entityxx).getTrails().size(); var40++) {
                     Trails.Trail ctx = ((IEntity)entityxx).getTrails().get(var40);
                     Vec3d pos = ctx.interpolate(Render3DEngine.getTickDelta());
                     bufferBuilder.vertex(
                           stack.peek().getPositionMatrix(), (float)pos.x, (float)pos.y + step * 2.0F, (float)pos.z
                        )
                        .color(
                           Render2DEngine.injectAlpha(new Color(255, 255, 255), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0))
                              .getRGB()
                        );
                     bufferBuilder.vertex(
                           stack.peek().getPositionMatrix(), (float)pos.x, (float)pos.y + step * 3.0F, (float)pos.z
                        )
                        .color(
                           Render2DEngine.injectAlpha(new Color(255, 255, 255), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0))
                              .getRGB()
                        );
                  }

                  Render2DEngine.endBuilding(bufferBuilder);
                  RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                  bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

                  for (int var41 = 0; var41 < ((IEntity)entityxx).getTrails().size(); var41++) {
                     Trails.Trail ctx = ((IEntity)entityxx).getTrails().get(var41);
                     Vec3d pos = ctx.interpolate(Render3DEngine.getTickDelta());
                     bufferBuilder.vertex(
                           stack.peek().getPositionMatrix(), (float)pos.x, (float)pos.y + step * 3.0F, (float)pos.z
                        )
                        .color(
                           Render2DEngine.injectAlpha(new Color(248, 139, 160), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0))
                              .getRGB()
                        );
                     bufferBuilder.vertex(
                           stack.peek().getPositionMatrix(), (float)pos.x, (float)pos.y + step * 4.0F, (float)pos.z
                        )
                        .color(
                           Render2DEngine.injectAlpha(new Color(248, 139, 160), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0))
                              .getRGB()
                        );
                  }

                  Render2DEngine.endBuilding(bufferBuilder);
                  RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                  bufferBuilder = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

                  for (int var42 = 0; var42 < ((IEntity)entityxx).getTrails().size(); var42++) {
                     Trails.Trail ctx = ((IEntity)entityxx).getTrails().get(var42);
                     Vec3d pos = ctx.interpolate(Render3DEngine.getTickDelta());
                     bufferBuilder.vertex(
                           stack.peek().getPositionMatrix(), (float)pos.x, (float)pos.y + step * 4.0F, (float)pos.z
                        )
                        .color(
                           Render2DEngine.injectAlpha(new Color(69, 221, 255), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0))
                              .getRGB()
                        );
                     bufferBuilder.vertex(
                           stack.peek().getPositionMatrix(), (float)pos.x, (float)pos.y + step * 5.0F, (float)pos.z
                        )
                        .color(
                           Render2DEngine.injectAlpha(new Color(69, 221, 255), (int)((double)alpha * ctx.animation(Render3DEngine.getTickDelta()) * 255.0))
                              .getRGB()
                        );
                  }

                  Render2DEngine.endBuilding(bufferBuilder);
                  Render3DEngine.endRender();
                  RenderSystem.enableCull();
                  RenderSystem.disableDepthTest();
                  stack.pop();
               }
            }
         }
      }

      if (!this.particles.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         switch ((HitParticles.Mode)this.mode.getValue()) {
            case Stars:
               RenderSystem.setShaderTexture(0, TextureStorage.star);
               break;
            case Bloom:
               RenderSystem.setShaderTexture(0, TextureStorage.firefly);
               break;
            case Hearts:
               RenderSystem.setShaderTexture(0, TextureStorage.heart);
               break;
            default:
               return;
         }

         RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
         BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         if (mc.player != null && mc.world != null) {
            this.particles.forEach(p -> p.render(stack, bufferBuilder));
         }

         Render2DEngine.endBuilding(bufferBuilder);
         RenderSystem.disableBlend();
         RenderSystem.depthMask(true);
         RenderSystem.disableDepthTest();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   @Override
   public void onUpdate() {
      Color c = this.lmode.getValue() == Trails.Mode.Sync ? HudEditor.getColor(mc.player.age % 360) : this.lcolor.getValue().getColorObject();

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player.getPos().getZ() != player.prevZ
            || player.getPos().getX() != player.prevX && !this.onlySelf.getValue()) {
            ((IEntity)player)
               .getTrails()
               .add(new Trails.Trail(new Vec3d(player.prevX, player.prevY, player.prevZ), player.getPos(), c));
            if (this.players.is(Trails.Players.Particles)) {
               for (int i = 0; i < this.amount.getValue(); i++) {
                  this.particles
                     .add(
                        new Trails.Particle(
                           player.getX(),
                           (double)MathUtility.random((float)(player.getY() + (double)player.getHeight()), (float)player.getY()),
                           player.getZ(),
                           c
                        )
                     );
               }
            }
         }

         ((IEntity)player).getTrails().removeIf(Trails.Trail::update);
      }

      for (Entity en : Managers.ASYNC.getAsyncEntities()) {
         if (en instanceof ArrowEntity ae && ae.prevY != ae.getY() && this.arrows.is(Trails.Particles.Particles)) {
            for (int i = 0; i < 5; i++) {
               this.particles.add(new Trails.Particle(en.getX(), en.getY(), en.getZ(), HudEditor.getColor(mc.player.age)));
            }
         }

         if (en instanceof EnderPearlEntity && this.pearls.is(Trails.Particles.Particles)) {
            for (int i = 0; i < 5; i++) {
               this.particles.add(new Trails.Particle(en.getX(), en.getY(), en.getZ(), HudEditor.getColor(mc.player.age)));
            }
         }
      }

      if (Managers.PLAYER.currentPlayerSpeed != 0.0F) {
         ((IEntity)mc.player)
            .getTrails()
            .add(new Trails.Trail(new Vec3d(mc.player.prevX, mc.player.prevY, mc.player.prevZ), mc.player.getPos(), c));
         if (this.players.is(Trails.Players.Particles)) {
            for (int i = 0; i < this.amount.getValue(); i++) {
               this.particles
                  .add(
                     new Trails.Particle(
                        mc.player.getX(),
                        (double)MathUtility.random(
                           (float)(mc.player.getY() + (double)mc.player.getHeight()), (float)mc.player.getY()
                        ),
                        mc.player.getZ(),
                        c
                     )
                  );
            }
         }
      }

      this.particles.removeIf(particle -> (float)(System.currentTimeMillis() - particle.time) > 1000.0F * (float)this.lifeTime.getValue().intValue());
   }

   private void calcTrajectory(Entity e) {
      double motionX = e.getVelocity().x;
      double motionY = e.getVelocity().y;
      double motionZ = e.getVelocity().z;
      double x = e.getX();
      double y = e.getY();
      double z = e.getZ();
      new Vec3d(x, y, z);

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

         double var19;
         double var20;
         motionY = e instanceof ArrowEntity ? (var19 = motionY - 0.05F) : (var20 = motionY - 0.03F);
         Vec3d pos = new Vec3d(x, y, z);
         if (mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player)) != null
               && (
                  mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player)).getType()
                        == Type.ENTITY
                     || mc.world.raycast(new RaycastContext(lastPos, pos, ShapeType.OUTLINE, FluidHandling.NONE, mc.player)).getType()
                        == Type.BLOCK
               )
            || y <= -65.0) {
            break;
         }

         if (e.getVelocity().x != 0.0 || e.getVelocity().y != 0.0 || e.getVelocity().z != 0.0) {
            int alpha = (int)MathUtility.clamp(255.0F * ((float)i / 8.0F), 0.0F, 255.0F);
            Render3DEngine.drawLine(
               lastPos,
               pos,
               this.lmode.getValue() == Trails.Mode.Sync
                  ? Render2DEngine.injectAlpha(HudEditor.getColor(i * 5), alpha)
                  : Render2DEngine.injectAlpha(this.lcolor.getValue().getColorObject(), alpha)
            );
         }
      }
   }

   private static enum Mode {
      Custom,
      Sync;
   }

   public class Particle {
      double x;
      double y;
      double z;
      double motionX;
      double motionY;
      double motionZ;
      long time;
      Color color;

      public Particle(double x, double y, double z, Color color) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.motionX = (double)MathUtility.random(
            -((float)Trails.this.speed.getValue().intValue()) / 200.0F, (float)Trails.this.speed.getValue().intValue() / 200.0F
         );
         this.motionY = (double)MathUtility.random(
            -((float)Trails.this.speed.getValue().intValue()) / 200.0F, (float)Trails.this.speed.getValue().intValue() / 200.0F
         );
         this.motionZ = (double)MathUtility.random(
            -((float)Trails.this.speed.getValue().intValue()) / 200.0F, (float)Trails.this.speed.getValue().intValue() / 200.0F
         );
         this.time = System.currentTimeMillis();
         this.color = color;
      }

      public void update() {
         double sp = (double)((float)Trails.this.starsScale.getValue().intValue() / 10.0F);
         this.x = this.x + this.motionX;
         this.y = this.y + this.motionY;
         this.z = this.z + this.motionZ;
         if (this.posBlock(this.x, this.y - (double)((float)Trails.this.starsScale.getValue().intValue() / 10.0F), this.z)) {
            this.motionY = -this.motionY / 1.1;
         } else if (this.posBlock(this.x, this.y, this.z)
            || this.posBlock(this.x - sp, this.y, this.z - sp)
            || this.posBlock(this.x + sp, this.y, this.z + sp)
            || this.posBlock(this.x + sp, this.y, this.z - sp)
            || this.posBlock(this.x - sp, this.y, this.z + sp)
            || this.posBlock(this.x + sp, this.y, this.z)
            || this.posBlock(this.x - sp, this.y, this.z)
            || this.posBlock(this.x, this.y, this.z - sp)
            || this.posBlock(this.x, this.y, this.z + sp)) {
            this.motionX = -this.motionX;
            this.motionZ = -this.motionZ;
         }

         if (Trails.this.physics.getValue() == HitParticles.Physics.Fall) {
            this.motionY -= 5.0E-4F;
         }

         this.motionX /= 1.005;
         this.motionZ /= 1.005;
         this.motionY /= 1.005;
      }

      public void render(MatrixStack matrixStack, BufferBuilder bufferBuilder) {
         this.update();
         float scale = (float)Trails.this.starsScale.getValue().intValue() / 10.0F;
         double posX = this.x - Module.mc.getEntityRenderDispatcher().camera.getPos().getX();
         double posY = this.y - Module.mc.getEntityRenderDispatcher().camera.getPos().getY();
         double posZ = this.z - Module.mc.getEntityRenderDispatcher().camera.getPos().getZ();
         Camera camera = Module.mc.gameRenderer.getCamera();
         MatrixStack matrices = new MatrixStack();
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
         matrices.translate(posX, posY, posZ);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         float colorAnim = (float)(System.currentTimeMillis() - this.time) / (1000.0F * (float)Trails.this.lifeTime.getValue().intValue());
         Color c = Trails.this.lmode.getValue() == Trails.Mode.Sync
            ? HudEditor.getColor((int)(360.0F * colorAnim))
            : Trails.this.lcolor.getValue().getColorObject();
         bufferBuilder.vertex(matrix, -scale / 2.0F, scale, 0.0F)
            .texture(0.0F, 1.0F)
            .color(Render2DEngine.applyOpacity(c, 1.0F - colorAnim).getRGB());
         bufferBuilder.vertex(matrix, scale, scale, 0.0F)
            .texture(1.0F, 1.0F)
            .color(Render2DEngine.applyOpacity(c, 1.0F - colorAnim).getRGB());
         bufferBuilder.vertex(matrix, scale, -scale / 2.0F, 0.0F)
            .texture(1.0F, 0.0F)
            .color(Render2DEngine.applyOpacity(c, 1.0F - colorAnim).getRGB());
         bufferBuilder.vertex(matrix, -scale / 2.0F, -scale / 2.0F, 0.0F)
            .texture(0.0F, 0.0F)
            .color(Render2DEngine.applyOpacity(c, 1.0F - colorAnim).getRGB());
      }

      private boolean posBlock(double x, double y, double z) {
         Block b = Module.mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock();
         return b != Blocks.AIR && b != Blocks.WATER && b != Blocks.LAVA;
      }
   }

   private static enum Particles {
      Trail,
      Particles,
      None;
   }

   private static enum Players {
      Trail,
      Particles,
      Cute,
      Tail,
      None;
   }

   public static class Trail {
      private final Vec3d from;
      private final Vec3d to;
      private final Color color;
      private int ticks;
      private int prevTicks;

      public Trail(Vec3d from, Vec3d to, Color color) {
         this.from = from;
         this.to = to;
         this.ticks = 10;
         this.color = color;
      }

      public Vec3d interpolate(float pt) {
         double x = this.from.x
            + (this.to.x - this.from.x) * (double)pt
            - Module.mc.getEntityRenderDispatcher().camera.getPos().getX();
         double y = this.from.y
            + (this.to.y - this.from.y) * (double)pt
            - Module.mc.getEntityRenderDispatcher().camera.getPos().getY();
         double z = this.from.z
            + (this.to.z - this.from.z) * (double)pt
            - Module.mc.getEntityRenderDispatcher().camera.getPos().getZ();
         return new Vec3d(x, y, z);
      }

      public double animation(float pt) {
         return (double)((float)this.prevTicks + (float)(this.ticks - this.prevTicks) * pt) / 10.0;
      }

      public boolean update() {
         this.prevTicks = this.ticks;
         return this.ticks-- <= 0;
      }

      public Color color() {
         return this.color;
      }
   }
}
