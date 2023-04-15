package cordori.attributepotion.file;

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.utils.Potion;
import eos.moe.dragoncore.api.CoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ConfigManager {
    public static boolean debug;
    public static String prefix;
    public static String identifier;
    public static boolean dragoncore;
    public static AhoCorasickDoubleArrayTrie<String> trie = new AhoCorasickDoubleArrayTrie<>();
    public static HashMap<String, String> coreKeys = new HashMap<>();
    public static HashMap<String, Integer> group = new HashMap<>();
    public static HashMap<UUID, HashMap<String, Long>> cooldown = new HashMap<>();
    public static List<String> potionKeys = new ArrayList<>();
    public static HashMap<String, String> potionNames = new HashMap<>();
    public static HashMap<String, String> potionLores = new HashMap<>();
    public static Map<String, Potion> potions = new HashMap<>();
    public static void reloadMyConfig() {
        AttributePotion ap = AttributePotion.getInstance();
        ap.reloadConfig();
        debug = ap.getConfig().getBoolean("debug");
        prefix = ap.getConfig().getString("prefix").replaceAll("&","§");
        identifier = ap.getConfig().getString("identifier");
        dragoncore = ap.getConfig().getBoolean("dragoncore");
        loadGroup(ap);
        loadFiles(ap);
        Bukkit.getScheduler().runTaskAsynchronously(ap, () -> loadFiles(ap));
    }

    public static void loadGroup(AttributePotion ap) {
        File file = new File(ap.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> groupKeys = config.getConfigurationSection("group").getKeys(false);
        Set<String> dragoncoreKeys = config.getConfigurationSection("dragoncoreKeys").getKeys(false);

        group.clear();
        for(String groupKey : groupKeys) {
            int groupTime = config.getInt("group." + groupKey);
            group.put(groupKey, groupTime);
            if(debug) {
                System.out.println("§6----------------------------");
                System.out.println("§a 药水组名称: " + groupKey);
                System.out.println("§a 药水组冷却: " + groupTime);
                System.out.println("§6----------------------------");
            }
        }

        if(!AttributePotion.DragonCore) return;
        coreKeys.clear();
        for(String dragoncoreKey : dragoncoreKeys) {
            String slotName = config.getString("dragoncoreKeys." + dragoncoreKey);
            coreKeys.put(dragoncoreKey, slotName);
            CoreAPI.registerKey(dragoncoreKey);
            if(debug) {
                System.out.println("§6----------------------------");
                System.out.println("§a 按键名称: " + dragoncoreKey);
                System.out.println("§a 槽位名称: " + slotName);
                System.out.println("§6----------------------------");
            }
        }
    }
    public static List<String> colorStringList(List<String> stringList) {
        List<String> colorList = new ArrayList<>();
        for(String list : stringList) colorList.add(list.replaceAll("&", "§"));
        return colorList;

    }
    public static void trieBuild(AttributePotion ap) {
        File file = new File(ap.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getString("identifier").equalsIgnoreCase("name")) {
            trie.build(potionNames);
        } else {
            trie.build(potionLores);
        }
    }

    private static void loadFiles(AttributePotion ap) {
        potions.clear();
        potionKeys.clear();
        potionNames.clear();
        potionLores.clear();
        File potionsFolder = new File(ap.getDataFolder() + "/potions");
        findAllYmlFiles(ap, potionsFolder);
        trieBuild(ap);
        if(debug) {
            System.out.println(potionNames);
            System.out.println(potionLores);
        }
    }

    private static void findAllYmlFiles(AttributePotion ap, File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 如果是文件夹则递归查找
                    findAllYmlFiles(ap, file);
                } else if (file.getName().endsWith(".yml")) {
                    // 如果是YML文件则加入结果列表
                    File potionfile = new File(folder + "/" + file.getName());
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(potionfile);
                    loadPotions(config);
                }
            }
        }
    }
    private static void loadPotions(YamlConfiguration config) {
        Set<String> potionKeys = config.getKeys(false);
        for(String potionKey : potionKeys) {
            String name = config.getString(potionKey + ".name").replaceAll("[^\\[\\u4e00-\\u9fa5\\]+]", "");
            String lore = config.getString(potionKey + ".lore").replaceAll("[^\\[\\u4e00-\\u9fa5\\]+]", "");
            int time = config.getInt(potionKey + ".time");
            int cooldown = config.getInt(potionKey + ".cooldown");
            String group = config.getString(potionKey + ".group");
            List<String> conditions = config.getStringList(potionKey + ".conditions");
            boolean shift = config.getBoolean(potionKey + ".shift", false);
            List<String> attributes = colorStringList(config.getStringList(potionKey + ".attributes"));
            boolean consume = config.getBoolean(potionKey + ".consume", true);
            List<String> commands = config.getStringList(potionKey + ".commands");

            Map<String, String> effects = new HashMap<>();
            if(config.contains(potionKey + ".effects")) {
                Set<String> effectKeys = config.getConfigurationSection(potionKey + ".effects").getKeys(false);
                for (String effectKey : effectKeys) {
                    String value = config.getString(potionKey + ".effects." + effectKey);
                    effects.put(effectKey, value);
                }
            }

            Map<String, String> potionEffects = new HashMap<>();
            if(config.contains(potionKey + ".potions")) {
                Set<String> potionEffectsKeys = config.getConfigurationSection(potionKey + ".potions").getKeys(false);
                for (String potionEffectKey : potionEffectsKeys) {
                    String value = config.getString(potionKey + ".potions." + potionEffectKey);
                    potionEffects.put(potionEffectKey, value);
                }
            }

            Map<String, Boolean> options = new HashMap<>();
            if(config.contains(potionKey + ".options")) {
                Set<String> optionKeys = config.getConfigurationSection(potionKey + ".options").getKeys(false);
                for (String optionKey : optionKeys) {
                    boolean value = config.getBoolean(potionKey + ".options." + optionKey);
                    options.put(optionKey, value);
                }
            }
            Potion potion = new Potion(potionKey, name, lore, time, cooldown, group, conditions, shift, effects, potionEffects, attributes, consume, commands, options);
            ConfigManager.potionKeys.add(potionKey);
            potionNames.put(name, potionKey);
            potionLores.put(lore, potionKey);
            potions.put(potionKey, potion);

            if(debug) {
                System.out.println("§6----------------------------");
                System.out.println("§a key: " + potionKey);
                System.out.println("§a name: " + name);
                System.out.println("§a Lore: " + lore);
                System.out.println("§a time: " + time);
                System.out.println("§a cooldown: " + cooldown);
                System.out.println("§a group: " + group);
                System.out.println("§a conditions: " + conditions);
                System.out.println("§a shift: " + shift);
                System.out.println("§a effects: " + effects.entrySet());
                System.out.println("§a potions: " + potionEffects.entrySet());
                System.out.println("§a attributes: " + attributes);
                System.out.println("§a consume: " + consume);
                System.out.println("§a commands: " + commands);
                System.out.println("§a options: " + options);
                System.out.println("§6----------------------------");
            }
        }
    }
}
