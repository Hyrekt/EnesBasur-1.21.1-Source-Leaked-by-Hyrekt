package thunder.hack.injection;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.entity.EntityPose;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

@Mixin({LivingEntityRenderer.class})
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> {
   private LivingEntity lastEntity;
   private float originalHeadYaw;
   private float originalPrevHeadYaw;
   private float originalPrevHeadPitch;
   private float originalHeadPitch;
   @Shadow
   protected M model;
   @Shadow
   @Final
   protected List<FeatureRenderer<T, M>> features;

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onRenderPre(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         if (Module.mc.player != null
            && livingEntity == Module.mc.player
            && Module.mc.player.getControllingVehicle() == null
            && ClientSettings.renderRotations.getValue()
            && !ThunderHack.isFuturePresent()) {
            this.originalHeadYaw = livingEntity.headYaw;
            this.originalPrevHeadYaw = livingEntity.prevHeadYaw;
            this.originalPrevHeadPitch = livingEntity.prevPitch;
            this.originalHeadPitch = livingEntity.getPitch();
            livingEntity.setPitch(((IClientPlayerEntity)MinecraftClient.getInstance().player).getLastPitch());
            livingEntity.prevPitch = Managers.PLAYER.lastPitch;
            livingEntity.headYaw = ((IClientPlayerEntity)MinecraftClient.getInstance().player).getLastYaw();
            livingEntity.bodyYaw = Render2DEngine.interpolateFloat(
               Managers.PLAYER.prevBodyYaw, Managers.PLAYER.bodyYaw, (double)Render3DEngine.getTickDelta()
            );
            livingEntity.prevHeadYaw = Managers.PLAYER.lastYaw;
            livingEntity.prevBodyYaw = Render2DEngine.interpolateFloat(
               Managers.PLAYER.prevBodyYaw, Managers.PLAYER.bodyYaw, (double)Render3DEngine.getTickDelta()
            );
         }

         if (livingEntity != Module.mc.player
            && ModuleManager.freeCam.isEnabled()
            && ModuleManager.freeCam.track.getValue()
            && ModuleManager.freeCam.trackEntity != null
            && ModuleManager.freeCam.trackEntity == livingEntity) {
            ci.cancel();
         } else {
            this.lastEntity = livingEntity;
            if (livingEntity instanceof PlayerEntity pe && ModuleManager.chams.isEnabled() && ModuleManager.chams.players.getValue()) {
               ModuleManager.chams.renderPlayer(pe, f, g, matrixStack, i, this.model, ci, () -> this.postRender(livingEntity));
               if (!pe.isSpectator()) {
                  matrixStack.push();
                  float h = MathHelper.lerpAngleDegrees(g, pe.prevBodyYaw, pe.bodyYaw);
                  float j = MathHelper.lerpAngleDegrees(g, pe.prevHeadYaw, pe.headYaw);
                  float k = j - h;
                  Entity entity;
                  if (pe.hasVehicle() && (entity = pe.getVehicle()) instanceof LivingEntity) {
                     LivingEntity livingEntity2 = (LivingEntity)entity;
                     h = MathHelper.lerpAngleDegrees(g, livingEntity2.prevBodyYaw, livingEntity2.bodyYaw);
                     k = j - h;
                     float l = MathHelper.wrapDegrees(k);
                     if (l < -85.0F) {
                        l = -85.0F;
                     }

                     if (l >= 85.0F) {
                        l = 85.0F;
                     }

                     h = j - l;
                     if (l * l > 2500.0F) {
                        h += l * 0.2F;
                     }

                     k = j - h;
                  }

                  float m = MathHelper.lerp(g, pe.prevPitch, pe.getPitch());
                  if (LivingEntityRenderer.shouldFlipUpsideDown(pe)) {
                     m *= -1.0F;
                     k *= -1.0F;
                  }

                  Direction direction;
                  if (pe.isInPose(EntityPose.SLEEPING) && (direction = pe.getSleepingDirection()) != null) {
                     float n = pe.getEyeHeight(EntityPose.STANDING) - 0.1F;
                     matrixStack.translate((float)(-direction.getOffsetX()) * n, 0.0F, (float)(-direction.getOffsetZ()) * n);
                  }

                  float lx = (float)pe.age + g;
                  ModuleManager.chams.setupTransforms1(pe, matrixStack, lx, h, g);
                  matrixStack.scale(-1.0F, -1.0F, 1.0F);
                  matrixStack.scale(0.9375F, 0.9375F, 0.9375F);
                  matrixStack.translate(0.0F, -1.501F, 0.0F);
                  float n = 0.0F;
                  float o = 0.0F;
                  if (!pe.hasVehicle() && pe.isAlive()) {
                     n = pe.limbAnimator.getSpeed(g);
                     o = pe.limbAnimator.getPos(g);
                     if (pe.isBaby()) {
                        o *= 3.0F;
                     }

                     if (n > 1.0F) {
                        n = 1.0F;
                     }
                  }

                  for (FeatureRenderer<T, M> featureRenderer : this.features) {
                     featureRenderer.render(matrixStack, vertexConsumerProvider, i, livingEntity, o, n, g, lx, k, m);
                  }

                  matrixStack.pop();
               }
            }
         }
      }
   }

   @Unique
   public void postRender(T livingEntity) {
      if (!Module.fullNullCheck()) {
         if (Module.mc.player != null
            && livingEntity == Module.mc.player
            && Module.mc.player.getControllingVehicle() == null
            && ClientSettings.renderRotations.getValue()
            && !ThunderHack.isFuturePresent()) {
            livingEntity.prevPitch = this.originalPrevHeadPitch;
            livingEntity.setPitch(this.originalHeadPitch);
            livingEntity.headYaw = this.originalHeadYaw;
            livingEntity.prevHeadYaw = this.originalPrevHeadYaw;
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   public void onRenderPost(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      if (!Module.fullNullCheck()) {
         this.postRender(livingEntity);
      }
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private void renderModelHook(EntityModel<T> instance, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
      if (!Module.fullNullCheck()) {
         if (ModuleManager.customModels.isEnabled() && this.lastEntity == Module.mc.player) {
            float tickDelta = Module.mc.getRenderTickCounter().getTickDelta(true);
            if (ModuleManager.customModels
               .onRenderModel(matrices, MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(), light, overlay, this.lastEntity, tickDelta)) {
               return;
            }
         }

         float alpha = -1.0F;
         if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.antiPlayerCollision.getValue() && this.lastEntity != Module.mc.player) {
            LivingEntity class_13092 = this.lastEntity;
            PlayerEntity pl;
            if (this.lastEntity instanceof PlayerEntity && !(pl = (PlayerEntity)class_13092).isInvisible()) {
               alpha = MathUtility.clamp((float)(Module.mc.player.squaredDistanceTo(this.lastEntity.getPos()) / 3.0) + 0.2F, 0.0F, 1.0F);
            }
         }

         if (this.lastEntity != Module.mc.player) {
            LivingEntity class_13092 = this.lastEntity;
            PlayerEntity pl;
            if (this.lastEntity instanceof PlayerEntity && (pl = (PlayerEntity)class_13092).isInvisible()) {
               alpha = 0.3F;
            }
         }

         if (alpha != -1.0F) {
            color = Render2DEngine.applyOpacity(654311423, alpha);
         }

         instance.render(matrices, vertices, light, overlay, color);
      }
   }
}
