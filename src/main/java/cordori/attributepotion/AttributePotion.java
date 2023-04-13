package cordori.attributepotion;

import cordori.attributepotion.command.MainCommand;
import cordori.attributepotion.file.ConfigManager;
import cordori.attributepotion.hook.PAPIHook;
import cordori.attributepotion.listener.DCoreUseEvent;
import cordori.attributepotion.listener.UseEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public final class AttributePotion extends JavaPlugin {
    private static AttributePotion Instance;
    public static boolean Skillapi = false;
    public static boolean DragonCore = false;
    public static boolean AttributePlus = false;
    public static boolean AP3 = false;
    public static boolean SX3 = false;
    public static boolean SXAttribute = false;
    public static AttributePotion getInstance() {
        return Instance;
    }
    @Override
    public void onEnable() {
        checkPlugins();
        Instance = this;
        createFile();
        ConfigManager.reloadMyConfig();
        Bukkit.getPluginCommand(("attributepotion")).setExecutor(new MainCommand());
        Bukkit.getPluginManager().registerEvents(new UseEvent(), this);
        getLogger().info("§a[属性药水] 插件加载成功！");
    }

    @Override
    public void onDisable() {
        getLogger().info("§c[属性药水] 已卸载，感谢您的使用~");
    }
    private void checkPlugins() {
        Plugin AP = Bukkit.getPluginManager().getPlugin("AttributePlus");
        Plugin SX = Bukkit.getPluginManager().getPlugin("SX-Attribute");

        if (AP != null && SX == null) {
            getLogger().info("§6[属性药水]§a已找到AttributePlus插件，插件将以AP作为默认属性来源！");
            String version = AP.getDescription().getVersion();
            if(version.startsWith("3")) AP3 = true;
            AttributePlus = true;
        }
        if(SX != null && AP == null) {
            getLogger().info("§6[属性药水]§a已找到 SX-Attribute 插件！插件将以SX作为默认属性来源！");
            String version = SX.getDescription().getVersion();
            if(version.startsWith("3")) SX3 = true;
            SXAttribute = true;
        }
        if(AP != null && SX != null){
            getLogger().info("§6[属性药水]§a已找到AttributePlus/SX-Attribute插件，插件将以AP作为默认属性来源！");
            String version = AP.getDescription().getVersion();
            if(version.startsWith("3")) AP3 = true;
            AttributePlus = true;
        } else if(AP == null && SX == null) {
            getLogger().severe("§6[属性药水]§c未找到AttributePlus/SX-Attribute插件，插件加载失败！");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("§6[属性药水]§a已找到PlaceholderAPI插件，可使用变量计算功能！");
            new PAPIHook(this).register();
        } else {
            getLogger().warning("§6[属性药水]§e未找到PAPI插件，无法使用本插件变量与变量计算功能！");
        }
        if(Bukkit.getPluginManager().getPlugin("SkillAPI") != null) {
            getLogger().info("§6[属性药水]§6已找到SkillAPI插件，可使用回复Mana功能！");
            Skillapi = true;
        } else {
            getLogger().warning("§6[属性药水]§e未找到SkillAPI插件，无法使用回复Mana功能！");
        }
        if(Bukkit.getPluginManager().getPlugin("DragonCore") != null) {
            getLogger().info("§6[属性药水]§a已找到DragonCore插件，可启用龙核按键使用药水！");
            Bukkit.getPluginManager().registerEvents(new DCoreUseEvent(), this);
            DragonCore = true;
        } else {
            getLogger().warning("§6[属性药水]§e未找到DragonCore插件，无法启用龙核按键使用药水！");
        }
    }
    private void createFile() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        saveDefaultConfig();
        try {
            createPotionsFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void createPotionsFile() throws IOException {
        File potionsFile = new File(getDataFolder(),"potions.yml");
        if(!potionsFile.exists()) {
            potionsFile.createNewFile();
            try (InputStream inputStream = getResource("potions.yml");
                 OutputStream outputStream = Files.newOutputStream(potionsFile.toPath())) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
        }
    }
}
