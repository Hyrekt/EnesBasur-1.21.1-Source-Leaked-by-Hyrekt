package thunder.hack.features.modules.render;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.RabbitEntityModel;
import net.minecraft.client.render.entity.model.VexEntityModel;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class CustomModels extends Module {
   public final Setting<CustomModels.Mode> mode = new Setting<>("Mode", CustomModels.Mode.Rabbit);
   public final Setting<Float> scale = new Setting<>("Scale", 1.5F, 0.1F, 5.0F);
   public RabbitEntityModel<RabbitEntity> rabbitModel;
   public VexEntityModel devilModel;
   public RabbitEntity dummyRabbit;
   public VexEntity dummyDevil;
   private static final Identifier RABBIT_TEXTURE = Identifier.of("textures/entity/rabbit/white.png");
   private static final Identifier DEVIL_TEXTURE = Identifier.of("textures/entity/illager/vex.png");
   private static final Identifier BLACK_RABBIT_TEXTURE = Identifier.of("textures/entity/rabbit/black.png");

   public CustomModels() {
      super("CustomModels", Module.Category.RENDER);
   }

   @Override
   public void onEnable() {
      if (this.rabbitModel == null) {
         this.rabbitModel = new RabbitEntityModel(mc.getEntityModelLoader().getModelPart(EntityModelLayers.RABBIT));
         this.devilModel = new VexEntityModel(mc.getEntityModelLoader().getModelPart(EntityModelLayers.VEX));
      }
   }

   public boolean onRenderModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, LivingEntity entity, float tickDelta) {
      if (this.isEnabled() && entity == mc.player) {
         if (this.rabbitModel == null || this.devilModel == null) {
            this.rabbitModel = new RabbitEntityModel(mc.getEntityModelLoader().getModelPart(EntityModelLayers.RABBIT));
            this.devilModel = new VexEntityModel(mc.getEntityModelLoader().getModelPart(EntityModelLayers.VEX));
         }

         if ((this.dummyRabbit == null || this.dummyDevil == null) && mc.world != null) {
            this.dummyRabbit = new RabbitEntity(EntityType.RABBIT, mc.world);
            this.dummyDevil = new VexEntity(EntityType.VEX, mc.world);
         }

         float h = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw);
         float j = MathHelper.lerpAngleDegrees(tickDelta, entity.prevHeadYaw, entity.headYaw);
         float headYaw = j - h;
         float headPitch = MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch());
         float s = this.scale.getValue();
         matrices.push();
         matrices.scale(s, s, s);
         if (this.mode.getValue() == CustomModels.Mode.Rabbit || this.mode.getValue() == CustomModels.Mode.BlackRabbit) {
            Identifier tex = this.mode.getValue() == CustomModels.Mode.Rabbit ? RABBIT_TEXTURE : BLACK_RABBIT_TEXTURE;
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
            if (this.dummyRabbit != null) {
               this.rabbitModel
                  .setAngles(
                     this.dummyRabbit,
                     entity.limbAnimator.getPos(tickDelta),
                     entity.limbAnimator.getSpeed(tickDelta),
                     (float)entity.age + tickDelta,
                     headYaw,
                     headPitch
                  );
            }

            this.rabbitModel.render(matrices, buffer, light, overlay, -1);
         } else if (this.mode.getValue() == CustomModels.Mode.Devil) {
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(DEVIL_TEXTURE));
            if (this.dummyDevil != null) {
               this.devilModel
                  .setAngles(
                     this.dummyDevil,
                     entity.limbAnimator.getPos(tickDelta),
                     entity.limbAnimator.getSpeed(tickDelta),
                     (float)entity.age + tickDelta,
                     headYaw,
                     headPitch
                  );
            }

            this.devilModel.render(matrices, buffer, light, overlay, -65536);
         }

         matrices.pop();
         return true;
      } else {
         return false;
      }
   }

   public static enum Mode {
      Rabbit,
      Devil,
      BlackRabbit;
   }
}
