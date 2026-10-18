package thunder.hack.features.modules.misc;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity.RemovalReason;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventAttack;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.TotemPopEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.injection.accesors.ILivingEntity;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.world.ExplosionUtility;

public class FakePlayer extends Module {
   private final Setting<Boolean> copyInventory = new Setting<>("CopyInventory", false);
   public static OtherClientPlayerEntity fakePlayer;
   private Setting<Boolean> record = new Setting<>("Record", false);
   private Setting<Boolean> play = new Setting<>("Play", false);
   private Setting<Boolean> autoTotem = new Setting<>("AutoTotem", false);
   private Setting<String> name = new Setting<>("Name", "Hell_Raider");
   private final List<FakePlayer.PlayerState> positions = new ArrayList<>();
   int movementTick;
   int deathTime;

   public FakePlayer() {
      super("FakePlayer", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      fakePlayer = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.fromString("66123666-6666-6666-6666-666666666600"), this.name.getValue()));
      fakePlayer.copyPositionAndRotation(mc.player);
      if (this.copyInventory.getValue()) {
         fakePlayer.setStackInHand(Hand.MAIN_HAND, mc.player.getMainHandStack().copy());
         fakePlayer.setStackInHand(Hand.OFF_HAND, mc.player.getOffHandStack().copy());
         fakePlayer.getInventory().setStack(36, mc.player.getInventory().getStack(36).copy());
         fakePlayer.getInventory().setStack(37, mc.player.getInventory().getStack(37).copy());
         fakePlayer.getInventory().setStack(38, mc.player.getInventory().getStack(38).copy());
         fakePlayer.getInventory().setStack(39, mc.player.getInventory().getStack(39).copy());
      }

      mc.world.addEntity(fakePlayer);
      fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 9999, 2));
      fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 9999, 4));
      fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 9999, 1));
   }

   @EventHandler
   public void onPacketReceive(PacketEvent.Receive e) {
      if (e.getPacket() instanceof ExplosionS2CPacket explosion && fakePlayer != null && fakePlayer.hurtTime == 0) {
         fakePlayer.onDamaged(mc.world.getDamageSources().generic());
         fakePlayer.setHealth(
            fakePlayer.getHealth()
               + fakePlayer.getAbsorptionAmount()
               - ExplosionUtility.getAutoCrystalDamage(
                  new Vec3d(explosion.getX(), explosion.getY(), explosion.getZ()), fakePlayer, 0, false
               )
         );
         if (fakePlayer.getHealth() <= 0.0F && ((ILivingEntity)fakePlayer).invokeTryUseTotem(mc.world.getDamageSources().generic())) {
            fakePlayer.setHealth(10.0F);
            ThunderHack.EVENT_BUS.post(new TotemPopEvent(fakePlayer, 1));
         }
      }
   }

   @EventHandler
   public void onSync(EventSync e) {
      if (this.record.getValue()) {
         this.positions
            .add(
               new FakePlayer.PlayerState(
                  mc.player.getX(),
                  mc.player.getY(),
                  mc.player.getZ(),
                  mc.player.getYaw(),
                  mc.player.getPitch()
               )
            );
      } else {
         if (fakePlayer != null) {
            if (this.play.getValue() && !this.positions.isEmpty()) {
               this.movementTick++;
               if (this.movementTick >= this.positions.size()) {
                  this.movementTick = 0;
                  return;
               }

               FakePlayer.PlayerState p = this.positions.get(this.movementTick);
               fakePlayer.setYaw(p.yaw);
               fakePlayer.setPitch(p.pitch);
               fakePlayer.setHeadYaw(p.yaw);
               fakePlayer.updateTrackedPosition(p.x, p.y, p.z);
               fakePlayer.updateTrackedPositionAndAngles(p.x, p.y, p.z, p.yaw, p.pitch, 3);
            } else {
               this.movementTick = 0;
            }

            if (this.autoTotem.getValue() && fakePlayer.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
               fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }

            if (fakePlayer.isDead()) {
               this.deathTime++;
               if (this.deathTime > 10) {
                  this.disable();
               }
            }
         }
      }
   }

   @EventHandler
   public void onAttack(EventAttack e) {
      if (fakePlayer != null && e.getEntity() == fakePlayer && fakePlayer.hurtTime == 0 && !e.isPre()) {
         mc.world
            .playSound(
               mc.player,
               fakePlayer.getX(),
               fakePlayer.getY(),
               fakePlayer.getZ(),
               SoundEvents.ENTITY_PLAYER_HURT,
               SoundCategory.PLAYERS,
               1.0F,
               1.0F
            );
         if (mc.player.fallDistance > 0.0F) {
            mc.world
               .playSound(
                  mc.player,
                  fakePlayer.getX(),
                  fakePlayer.getY(),
                  fakePlayer.getZ(),
                  SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                  SoundCategory.PLAYERS,
                  1.0F,
                  1.0F
               );
         }

         fakePlayer.onDamaged(mc.world.getDamageSources().generic());
         if ((double)ModuleManager.aura.getAttackCooldown() >= 0.85) {
            fakePlayer.setHealth(fakePlayer.getHealth() + fakePlayer.getAbsorptionAmount() - InventoryUtility.getHitDamage(mc.player.getMainHandStack(), fakePlayer));
         } else {
            fakePlayer.setHealth(fakePlayer.getHealth() + fakePlayer.getAbsorptionAmount() - 1.0F);
         }

         if (fakePlayer.getHealth() <= 0.0F && ((ILivingEntity)fakePlayer).invokeTryUseTotem(mc.world.getDamageSources().generic())) {
            fakePlayer.setHealth(10.0F);
            new EntityStatusS2CPacket(fakePlayer, (byte)35).apply(mc.player.networkHandler);
         }
      }
   }

   @Override
   public void onDisable() {
      if (fakePlayer != null) {
         fakePlayer.kill();
         fakePlayer.setRemoved(RemovalReason.KILLED);
         fakePlayer.onRemoved();
         fakePlayer = null;
         this.positions.clear();
         this.deathTime = 0;
      }
   }

   private static record PlayerState(double x, double y, double z, float yaw, float pitch) {
   }
}
