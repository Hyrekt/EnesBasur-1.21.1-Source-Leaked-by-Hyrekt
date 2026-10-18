package thunder.hack.features.modules.render;

import java.awt.Color;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render3DEngine;

public class Tracers extends Module {
   private final Setting<Float> height = new Setting<>("Height", 0.0F, 0.0F, 2.0F);
   private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(-1812004864, true)));
   private final Setting<ColorSetting> friendColor = new Setting<>("Friends", new ColorSetting(new Color(-1827152291, true)));

   public Tracers() {
      super("Tracers", Module.Category.RENDER);
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      for (PlayerEntity class_16572 : Managers.ASYNC.getAsyncPlayers()) {
         if (class_16572 != mc.player) {
            Color color1 = this.color.getValue().getColorObject();
            if (Managers.FRIEND.isFriend(class_16572)) {
               color1 = this.friendColor.getValue().getColorObject();
            }

            double x1 = mc.player.prevX + (mc.player.getX() - mc.player.prevX) * (double)Render3DEngine.getTickDelta();
            double y1 = (double)mc.player.getEyeHeight(mc.player.getPose())
               + mc.player.prevY
               + (mc.player.getY() - mc.player.prevY) * (double)Render3DEngine.getTickDelta();
            double z1 = mc.player.prevZ + (mc.player.getZ() - mc.player.prevZ) * (double)Render3DEngine.getTickDelta();
            Vec3d vec2 = new Vec3d(0.0, 0.0, 75.0)
               .rotateX(-((float)Math.toRadians((double)mc.gameRenderer.getCamera().getPitch())))
               .rotateY(-((float)Math.toRadians((double)mc.gameRenderer.getCamera().getYaw())))
               .add(x1, y1, z1);
            double x = class_16572.prevX + (class_16572.getX() - class_16572.prevX) * (double)Render3DEngine.getTickDelta();
            double y = class_16572.prevY + (class_16572.getY() - class_16572.prevY) * (double)Render3DEngine.getTickDelta();
            double z = class_16572.prevZ + (class_16572.getZ() - class_16572.prevZ) * (double)Render3DEngine.getTickDelta();
            Render3DEngine.drawLineDebug(vec2, new Vec3d(x, y + (double)this.height.getValue().floatValue(), z), color1);
         }
      }
   }
}
