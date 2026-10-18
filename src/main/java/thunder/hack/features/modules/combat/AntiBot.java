package thunder.hack.features.modules.combat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity.RemovalReason;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.misc.FakePlayer;
import thunder.hack.features.modules.render.NameTags;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

public final class AntiBot extends Module {
   public static ArrayList<PlayerEntity> bots = new ArrayList<>();
   public Setting<Boolean> remove = new Setting<>("Remove", false);
   public Setting<Boolean> onlyAura = new Setting<>("OnlyAura", true);
   private final Setting<AntiBot.Mode> mode = new Setting<>("Mode", AntiBot.Mode.UUIDCheck);
   public Setting<Integer> checkticks = new Setting<>("checkTicks", 3, 0, 10, v -> this.mode.getValue() == AntiBot.Mode.MotionCheck);
   private final Timer clearTimer = new Timer();
   private int ticks = 0;

   public AntiBot() {
      super("AntiBot", Module.Category.COMBAT);
   }

   @EventHandler
   public void onSync(EventSync e) {
      if (!this.onlyAura.getValue()) {
         mc.world.getPlayers().forEach(this::markAsBot);
      } else if (Aura.target instanceof PlayerEntity ent) {
         this.markAsBot(ent);
      }

      if (this.remove.getValue()) {
         bots.forEach(b -> {
            try {
               mc.world.removeEntity(b.getId(), RemovalReason.KILLED);
            } catch (Exception var2) {
            }
         });
      }

      if (this.clearTimer.passedMs(10000L)) {
         bots.clear();
         this.ticks = 0;
         this.clearTimer.reset();
      }
   }

   private void markAsBot(PlayerEntity ent) {
      if (!bots.contains(ent)) {
         switch ((AntiBot.Mode)this.mode.getValue()) {
            case UUIDCheck:
               if (!ent.getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + ent.getName().getString()).getBytes(StandardCharsets.UTF_8)))
                  && ent instanceof OtherClientPlayerEntity
                  && (FakePlayer.fakePlayer == null || ent.getId() != FakePlayer.fakePlayer.getId())
                  && !ent.getName().getString().contains("-")) {
                  this.addBot(ent);
               }
               break;
            case MotionCheck:
               double diffX = ent.getX() - ent.prevX;
               double diffZ = ent.getZ() - ent.prevZ;
               if (diffX * diffX + diffZ * diffZ > 0.5) {
                  if (this.ticks >= this.checkticks.getValue()) {
                     this.addBot(ent);
                  }

                  this.ticks++;
               }
               break;
            case ZeroPing:
               if (NameTags.getEntityPing(ent) <= 0) {
                  this.addBot(ent);
               }
         }
      }
   }

   private void addBot(PlayerEntity entity) {
      this.sendMessage(entity.getName().getString() + " is a bot!");
      bots.add(entity);
   }

   @Override
   public String getDisplayInfo() {
      return String.valueOf(bots.size());
   }

   public static enum Mode {
      UUIDCheck,
      MotionCheck,
      ZeroPing;
   }
}
