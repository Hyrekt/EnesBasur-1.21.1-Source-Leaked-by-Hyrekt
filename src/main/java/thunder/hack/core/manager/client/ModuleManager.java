package thunder.hack.core.manager.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.IManager;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.hud.impl.ArmorHud;
import thunder.hack.features.hud.impl.AutoCrystalInfo;
import thunder.hack.features.hud.impl.CandleHud;
import thunder.hack.features.hud.impl.ChestCounter;
import thunder.hack.features.hud.impl.Companion;
import thunder.hack.features.hud.impl.Cooldowns;
import thunder.hack.features.hud.impl.Coords;
import thunder.hack.features.hud.impl.Crosshair;
import thunder.hack.features.hud.impl.CrosshairArrows;
import thunder.hack.features.hud.impl.FpsCounter;
import thunder.hack.features.hud.impl.GapplesHud;
import thunder.hack.features.hud.impl.Hotbar;
import thunder.hack.features.hud.impl.KeyBinds;
import thunder.hack.features.hud.impl.KillFeed;
import thunder.hack.features.hud.impl.KillStats;
import thunder.hack.features.hud.impl.LegacyHud;
import thunder.hack.features.hud.impl.MemoryHud;
import thunder.hack.features.hud.impl.ModuleList;
import thunder.hack.features.hud.impl.PVPResources;
import thunder.hack.features.hud.impl.PingHud;
import thunder.hack.features.hud.impl.PotionHud;
import thunder.hack.features.hud.impl.Radar;
import thunder.hack.features.hud.impl.RadarRewrite;
import thunder.hack.features.hud.impl.Speedometer;
import thunder.hack.features.hud.impl.StaffBoard;
import thunder.hack.features.hud.impl.TPSCounter;
import thunder.hack.features.hud.impl.TargetHud;
import thunder.hack.features.hud.impl.TimerIndicator;
import thunder.hack.features.hud.impl.TotemCounter;
import thunder.hack.features.hud.impl.WaterMark;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.AntiCrash;
import thunder.hack.features.modules.client.AntiPacketException;
import thunder.hack.features.modules.client.ClickGui;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.features.modules.client.ClientSpoof;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.client.Notifications;
import thunder.hack.features.modules.client.RPC;
import thunder.hack.features.modules.client.Rotations;
import thunder.hack.features.modules.client.SoundFX;
import thunder.hack.features.modules.client.UnHook;
import thunder.hack.features.modules.client.Windows;
import thunder.hack.features.modules.combat.AntiBot;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.modules.combat.AutoTotem;
import thunder.hack.features.modules.combat.AutoTrap;
import thunder.hack.features.modules.combat.ElytraTarget;
import thunder.hack.features.modules.combat.HitBox;
import thunder.hack.features.modules.combat.TargetStrafe;
import thunder.hack.features.modules.combat.TriggerBot;
import thunder.hack.features.modules.misc.ExtraTab;
import thunder.hack.features.modules.misc.FakePlayer;
import thunder.hack.features.modules.misc.ItemScroller;
import thunder.hack.features.modules.misc.ItemSwapFix;
import thunder.hack.features.modules.misc.MiddleClick;
import thunder.hack.features.modules.misc.NameProtect;
import thunder.hack.features.modules.misc.Nuker;
import thunder.hack.features.modules.misc.TotemPopCounter;
import thunder.hack.features.modules.misc.VisualRange;
import thunder.hack.features.modules.movement.AutoSprint;
import thunder.hack.features.modules.movement.ElytraBooster;
import thunder.hack.features.modules.movement.Flight;
import thunder.hack.features.modules.movement.GuiMove;
import thunder.hack.features.modules.movement.Speed;
import thunder.hack.features.modules.movement.Strafe;
import thunder.hack.features.modules.movement.Velocity;
import thunder.hack.features.modules.player.AirPlace;
import thunder.hack.features.modules.player.AutoTool;
import thunder.hack.features.modules.player.ElytraHelper;
import thunder.hack.features.modules.player.FastUse;
import thunder.hack.features.modules.player.MultiTask;
import thunder.hack.features.modules.player.NoInteract;
import thunder.hack.features.modules.player.SpeedMine;
import thunder.hack.features.modules.render.Animations;
import thunder.hack.features.modules.render.AspectRatio;
import thunder.hack.features.modules.render.BlockESP;
import thunder.hack.features.modules.render.Chams;
import thunder.hack.features.modules.render.CustomModels;
import thunder.hack.features.modules.render.DamageTint;
import thunder.hack.features.modules.render.ESP;
import thunder.hack.features.modules.render.FOV;
import thunder.hack.features.modules.render.FreeCam;
import thunder.hack.features.modules.render.Fullbright;
import thunder.hack.features.modules.render.HitBubbles;
import thunder.hack.features.modules.render.HitParticles;
import thunder.hack.features.modules.render.ItemESP;
import thunder.hack.features.modules.render.JumpCircle;
import thunder.hack.features.modules.render.NameTags;
import thunder.hack.features.modules.render.NoRender;
import thunder.hack.features.modules.render.Particles;
import thunder.hack.features.modules.render.Tracers;
import thunder.hack.features.modules.render.Trails;
import thunder.hack.features.modules.render.Trajectories;
import thunder.hack.features.modules.render.ViewModel;
import thunder.hack.gui.clickui.ClickGUI;
import thunder.hack.gui.font.FontRenderers;

public class ModuleManager implements IManager {
   public ArrayList<Module> modules = new ArrayList<>();
   public List<Module> sortedModules = new ArrayList<>();
   public List<Integer> activeMouseKeys = new ArrayList<>();
   public static AntiPacketException antiPacketException = new AntiPacketException();
   public static TotemPopCounter totemPopCounter = new TotemPopCounter();
   public static CrosshairArrows crosshairArrows = new CrosshairArrows();
   public static AutoCrystalInfo autoCrystalInfo = new AutoCrystalInfo();
   public static ClientSettings clientSettings = new ClientSettings();
   public static TimerIndicator timerIndicator = new TimerIndicator();
   public static Notifications notifications = new Notifications();
   public static Trajectories trajectories = new Trajectories();
   public static TargetStrafe targetStrafe = new TargetStrafe();
   public static RadarRewrite radarRewrite = new RadarRewrite();
   public static PVPResources pvpResources = new PVPResources();
   public static ItemScroller itemScroller = new ItemScroller();
   public static HitParticles hitParticles = new HitParticles();
   public static TotemCounter totemCounter = new TotemCounter();
   public static ChestCounter chestCounter = new ChestCounter();
   public static VisualRange visualRange = new VisualRange();
   public static Speedometer speedometer = new Speedometer();
   public static NameProtect nameProtect = new NameProtect();
   public static ItemSwapFix itemSwapFix = new ItemSwapFix();
   public static MiddleClick middleClick = new MiddleClick();
   public static AspectRatio aspectRatio = new AspectRatio();
   public static ClientSpoof clientSpoof = new ClientSpoof();
   public static TriggerBot triggerBot = new TriggerBot();
   public static TPSCounter tpsCounter = new TPSCounter();
   public static StaffBoard staffBoard = new StaffBoard();
   public static NoInteract noInteract = new NoInteract();
   public static ModuleList moduleList = new ModuleList();
   public static JumpCircle jumpCircle = new JumpCircle();
   public static Fullbright fullbright = new Fullbright();
   public static FpsCounter fpsCounter = new FpsCounter();
   public static FakePlayer fakePlayer = new FakePlayer();
   public static ElytraHelper elytraHelper = new ElytraHelper();
   public static AutoSprint autoSprint = new AutoSprint();
   public static Animations animations = new Animations();
   public static DamageTint damageTint = new DamageTint();
   public static GapplesHud gapplesHud = new GapplesHud();
   public static HitBubbles hitBubbles = new HitBubbles();
   public static KillStats killStats = new KillStats();
   public static CandleHud candleHud = new CandleHud();
   public static Particles particles = new Particles();
   public static WaterMark waterMark = new WaterMark();
   public static ViewModel viewModel = new ViewModel();
   public static CustomModels customModels = new CustomModels();
   public static TargetHud targetHud = new TargetHud();
   public static SpeedMine speedMine = new SpeedMine();
   public static PotionHud potionHud = new PotionHud();
   public static MultiTask multitask = new MultiTask();
   public static LegacyHud legacyHud = new LegacyHud();
   public static HudEditor hudEditor = new HudEditor();
   public static Crosshair crosshair = new Crosshair();
   public static AutoTotem autoTotem = new AutoTotem();
   public static Cooldowns cooldowns = new Cooldowns();
   public static Rotations rotations = new Rotations();
   public static MemoryHud memoryHud = new MemoryHud();
   public static Companion companion = new Companion();
   public static AntiCrash antiCrash = new AntiCrash();
   public static Velocity velocity = new Velocity();
   public static NoRender noRender = new NoRender();
   public static NameTags nameTags = new NameTags();
   public static KeyBinds keyBinds = new KeyBinds();
   public static ExtraTab extraTab = new ExtraTab();
   public static ClickGui clickGui = new ClickGui();
   public static AutoTrap autoTrap = new AutoTrap();
   public static AutoTool autoTool = new AutoTool();
   public static ArmorHud armorHud = new ArmorHud();
   public static AirPlace airPlace = new AirPlace();
   public static KillFeed killFeed = new KillFeed();
   public static BlockESP blockESP = new BlockESP();
   public static Windows windows = new Windows();
   public static SoundFX soundFX = new SoundFX();
   public static PingHud pingHud = new PingHud();
   public static ItemESP itemESP = new ItemESP();
   public static GuiMove guiMove = new GuiMove();
   public static FreeCam freeCam = new FreeCam();
   public static FastUse fastUse = new FastUse();
   public static AntiBot antiBot = new AntiBot();
   public static Tracers tracers = new Tracers();
   public static Trails trails = new Trails();
   public static Strafe strafe = new Strafe();
   public static Hotbar hotbar = new Hotbar();
   public static HitBox hitBox = new HitBox();
   public static Flight flight = new Flight();
   public static Coords coords = new Coords();
   public static UnHook unHook = new UnHook();
   public static Speed speed = new Speed();
   public static ElytraBooster elytraBooster = new ElytraBooster();
   public static ElytraTarget elytraTarget = new ElytraTarget();
   public static Radar radar = new Radar();
   public static Nuker nuker = new Nuker();
   public static Chams chams = new Chams();
   public static Aura aura = new Aura();
   public static FOV fov = new FOV();
   public static ESP esp = new ESP();
   public static RPC rpc = new RPC();

   public ModuleManager() {
      for (Field field : this.getClass().getDeclaredFields()) {
         if (Module.class.isAssignableFrom(field.getType())) {
            field.setAccessible(true);

            try {
               this.modules.add((Module)field.get(this));
            } catch (IllegalAccessException var6) {
               var6.printStackTrace();
            }
         }
      }
   }

   public Module get(String name) {
      for (Module module : this.modules) {
         if (module.getName().equalsIgnoreCase(name)) {
            return module;
         }
      }

      return null;
   }

   public ArrayList<Module> getEnabledModules() {
      ArrayList<Module> enabledModules = new ArrayList<>();

      for (Module module : this.modules) {
         if (module.isEnabled()) {
            enabledModules.add(module);
         }
      }

      return enabledModules;
   }

   public ArrayList<Module> getModulesByCategory(Module.Category category) {
      ArrayList<Module> modulesCategory = new ArrayList<>();
      this.modules.forEach(module -> {
         if (module.getCategory() == category) {
            modulesCategory.add(module);
         }
      });
      return modulesCategory;
   }

   public List<Module.Category> getCategories() {
      return new ArrayList<>(Module.Category.values());
   }

   public void onLoad(String category) {
      try {
         ThunderHack.EVENT_BUS.unsubscribe(unHook);
      } catch (Exception var3) {
      }

      unHook.setEnabled(false);
      this.modules.sort(Comparator.comparing(Module::getName));
      this.modules.forEach(m -> {
         if (m.isEnabled() && (m.getCategory().getName().equalsIgnoreCase(category) || category.equals("none"))) {
            ThunderHack.EVENT_BUS.subscribe(m);
         }
      });
      if (ConfigManager.firstLaunch) {
         notifications.enable();
         rpc.enable();
         soundFX.enable();
      }
   }

   public void onUpdate() {
      if (!Module.fullNullCheck()) {
         this.modules.stream().filter(Module::isEnabled).forEach(Module::onUpdate);
      }
   }

   public void onRender2D(DrawContext context) {
      if (!mc.getDebugHud().shouldShowDebugHud() && !mc.options.hudHidden) {
         HudElement.anyHovered = false;
         this.modules.stream().filter(Module::isEnabled).forEach(module -> module.onRender2D(context));
         if (!HudElement.anyHovered && !ClickGUI.anyHovered && GLFW.glfwGetPlatform() != 393219) {
            GLFW.glfwSetCursor(mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(221185));
         }

         ThunderHack.core.onRender2D(context);
      }
   }

   public void onRender3D(MatrixStack stack) {
      this.modules.stream().filter(Module::isEnabled).forEach(module -> module.onRender3D(stack));
   }

   public void sortModules() {
      this.sortedModules = this.getEnabledModules()
         .stream()
         .filter(Module::isDrawn)
         .sorted(Comparator.comparing(module -> FontRenderers.getModulesRenderer().getStringWidth(module.getFullArrayString()) * -1.0F))
         .collect(Collectors.toList());
   }

   public void onLogout() {
      this.modules.forEach(Module::onLogout);
   }

   public void onLogin() {
      this.modules.forEach(Module::onLogin);
   }

   public void onUnload(String category) {
      this.modules.forEach(module -> {
         if (module.isEnabled() && (module.getCategory().getName().equalsIgnoreCase(category) || category.equals("none"))) {
            ThunderHack.EVENT_BUS.unsubscribe(module);
            module.setEnabled(false);
         }
      });
      this.modules.forEach(Module::onUnload);
   }

   public void onKeyPressed(int eventKey) {
      if (eventKey != -1 && eventKey != 0 && !(mc.currentScreen instanceof ClickGUI)) {
         this.modules.forEach(module -> {
            if (module.getBind().getKey() == eventKey) {
               module.toggle();
            }
         });
      }
   }

   public void onKeyReleased(int eventKey) {
      if (eventKey != -1 && eventKey != 0 && !(mc.currentScreen instanceof ClickGUI)) {
         this.modules.forEach(module -> {
            if (module.getBind().getKey() == eventKey && module.getBind().isHold()) {
               module.disable();
            }
         });
      }
   }

   public void onMoseKeyPressed(int eventKey) {
      if (eventKey != -1 && !(mc.currentScreen instanceof ClickGUI)) {
         this.modules.forEach(module -> {
            if (Objects.equals(module.getBind().getBind(), "M" + eventKey)) {
               module.toggle();
            }
         });
      }
   }

   public void onMoseKeyReleased(int eventKey) {
      if (eventKey != -1 && !(mc.currentScreen instanceof ClickGUI)) {
         this.activeMouseKeys.add(eventKey);
         this.modules.forEach(module -> {
            if (Objects.equals(module.getBind().getBind(), "M" + eventKey) && module.getBind().isHold()) {
               module.disable();
            }
         });
      }
   }

   public ArrayList<Module> getModulesSearch(String string) {
      ArrayList<Module> modulesCategory = new ArrayList<>();
      this.modules.forEach(module -> {
         if (module.getName().toLowerCase().contains(string.toLowerCase())) {
            modulesCategory.add(module);
         }
      });
      return modulesCategory;
   }

   public void registerModule(Module module) {
      if (module != null) {
         this.modules.add(module);
         if (module.isEnabled()) {
            ThunderHack.EVENT_BUS.subscribe(module);
         }
      }
   }

   public void registerHudElement(HudElement hudElement) {
      if (hudElement != null) {
         this.modules.add(hudElement);
      }
   }
}
