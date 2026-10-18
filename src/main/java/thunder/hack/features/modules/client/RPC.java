package thunder.hack.features.modules.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Objects;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.discord.DiscordEventHandlers;
import thunder.hack.utility.discord.DiscordRPC;
import thunder.hack.utility.discord.DiscordRichPresence;

public final class RPC extends Module {
   private static final DiscordRPC rpc = DiscordRPC.Holder.INSTANCE;
   public static Setting<RPC.Mode> mode = new Setting<>("Picture", RPC.Mode.Recode);
   public static Setting<Boolean> showIP = new Setting<>("ShowIP", true);
   public static Setting<RPC.sMode> smode = new Setting<>("StateMode", RPC.sMode.Stats);
   public static Setting<String> state = new Setting<>("State", "Beta? Recode? NextGen?");
   public static Setting<Boolean> nickname = new Setting<>("Nickname", true);
   public static DiscordRichPresence presence = new DiscordRichPresence();
   public static boolean started;
   static String String1 = "none";
   private final Timer timer_delay = new Timer();
   private static Thread thread;
   String slov;
   String[] rpc_perebor_en = new String[]{"Parkour", "Reporting cheaters", "Touching grass", "Asks how to bind", "Reporting bugs", "Watching Kilab"};
   String[] rpc_perebor_ru = new String[]{
      "\u041f\u0430\u0440\u043a\u0443\u0440\u0438\u0442",
      "\u0420\u0435\u043f\u043e\u0440\u0442\u0438\u0442 \u0447\u0438\u0442\u0435\u0440\u043e\u0432",
      "\u0422\u0440\u043e\u0433\u0430\u0435\u0442 \u0442\u0440\u0430\u0432\u0443",
      "\u0421\u043f\u0440\u0430\u0448\u0438\u0432\u0430\u0435\u0442 \u043a\u0430\u043a \u0437\u0430\u0431\u0438\u043d\u0434\u0438\u0442\u044c",
      "\u0420\u0435\u043f\u043e\u0440\u0442\u0438\u0442 \u0431\u0430\u0433\u0438",
      "\u0421\u043c\u043e\u0442\u0440\u0438\u0442 \u0424\u043b\u044e\u0433\u0435\u0440\u0430"
   };
   int randomInt;

   public RPC() {
      super("DiscordRPC", Module.Category.CLIENT);
   }

   public static void readFile() {
      try {
         File file = new File("ThunderHackRecode/misc/RPC.txt");
         if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
               while (reader.ready()) {
                  String1 = reader.readLine();
               }
            }
         }
      } catch (Exception var6) {
      }
   }

   public static void WriteFile(String url1, String url2) {
      File file = new File("ThunderHackRecode/misc/RPC.txt");

      try {
         file.createNewFile();

         try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(url1 + "SEPARATOR" + url2 + "\n");
         } catch (Exception var8) {
         }
      } catch (Exception var9) {
      }
   }

   @Override
   public void onDisable() {
      started = false;
      if (thread != null && !thread.isInterrupted()) {
         thread.interrupt();
      }

      if (rpc != null) {
         rpc.Discord_Shutdown();
      }
   }

   @Override
   public void onUpdate() {
      this.startRpc();
   }

   public void startRpc() {
      if (!this.isDisabled()) {
         if (!started) {
            started = true;
            if (rpc == null) {
               return;
            }

            DiscordEventHandlers handlers = new DiscordEventHandlers();
            rpc.Discord_Initialize("1093053626198523935", handlers, true, "");
            presence.startTimestamp = System.currentTimeMillis() / 1000L;
            presence.largeImageText = "v1.7b2407 [" + ThunderHack.GITHUB_HASH + "]";
            rpc.Discord_UpdatePresence(presence);
            thread = new Thread(() -> {
               while (!Thread.currentThread().isInterrupted()) {
                  rpc.Discord_RunCallbacks();
                  presence.details = this.getDetails();
                  switch ((RPC.sMode)smode.getValue()) {
                     case Custom:
                        presence.state = state.getValue();
                        break;
                     case Stats:
                        presence.state = "Hacks: " + Managers.MODULE.getEnabledModules().size() + " / " + Managers.MODULE.modules.size();
                        break;
                     case Version:
                        presence.state = "v1.7b2407 for mc 1.21";
                  }

                  if (nickname.getValue()) {
                     presence.smallImageText = "logged as - " + mc.getSession().getUsername();
                     presence.smallImageKey = "https://minotar.net/helm/" + mc.getSession().getUsername() + "/100.png";
                  } else {
                     presence.smallImageText = "";
                     presence.smallImageKey = "";
                  }

                  presence.button_label_1 = "Download";
                  presence.button_url_1 = "https://github.com/Pan4ur/ThunderHack-Recode/";
                  switch ((RPC.Mode)mode.getValue()) {
                     case Custom:
                        readFile();
                        presence.largeImageKey = String1.split("SEPARATOR")[0];
                        if (!Objects.equals(String1.split("SEPARATOR")[1], "none")) {
                           presence.smallImageKey = String1.split("SEPARATOR")[1];
                        }
                        break;
                     case MegaCute:
                        presence.largeImageKey = "https://media1.tenor.com/images/6bcbfcc0be97d029613b54f97845bc59/tenor.gif?itemid=26823781";
                        break;
                     case Recode:
                        presence.largeImageKey = "https://i.imgur.com/yY0z2Uq.gif";
                  }

                  rpc.Discord_UpdatePresence(presence);

                  try {
                     Thread.sleep(2000L);
                  } catch (InterruptedException var2) {
                  }
               }
            }, "TH-RPC-Handler");
            thread.start();
         }
      }
   }

   private String getDetails() {
      String result = "";
      if (!(mc.currentScreen instanceof MultiplayerScreen) && !(mc.currentScreen instanceof AddServerScreen) && !(mc.currentScreen instanceof TitleScreen)) {
         if (mc.getCurrentServerEntry() != null) {
            result = ClientSettings.isRu()
               ? (
                  showIP.getValue()
                     ? "\u0418\u0433\u0440\u0430\u0435\u0442 \u043d\u0430 " + mc.getCurrentServerEntry().address
                     : "\u0418\u0433\u0440\u0430\u0435\u0442 \u043d\u0430 \u0441\u0435\u0440\u0432\u0435\u0440\u0435"
               )
               : (showIP.getValue() ? "Playing on " + mc.getCurrentServerEntry().address : "Playing on server");
         } else if (mc.isInSingleplayer()) {
            result = ClientSettings.isRu()
               ? "\u0427\u0438\u0442\u0435\u0440\u0438\u0442 \u0432 \u043e\u0434\u0438\u043d\u043e\u0447\u043a\u0435"
               : "SinglePlayer hacker";
         }
      } else {
         if (this.timer_delay.passedMs(60000L)) {
            this.randomInt = (int)(Math.random() * 6.0 + 0.0);
            this.slov = ClientSettings.isRu() ? this.rpc_perebor_ru[this.randomInt] : this.rpc_perebor_en[this.randomInt];
            this.timer_delay.reset();
         }

         result = this.slov;
      }

      return result;
   }

   public static enum Mode {
      Custom,
      MegaCute,
      Recode;
   }

   public static enum sMode {
      Custom,
      Stats,
      Version;
   }
}
