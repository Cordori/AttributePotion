package cordori.attributepotion.file;

import com.germ.germplugin.api.GermKeyAPI;
import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.utils.LogInfo;
import cordori.attributepotion.utils.Potion;
import eos.moe.dragoncore.api.CoreAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigManager {
    public static AttributePotion ap;
    public static boolean debug;
    public static String prefix;
    public static String identifier;
    public static boolean dragoncore;
    public static boolean germplugin;
    public static AhoCorasickDoubleArrayTrie<String> trie = new AhoCorasickDoubleArrayTrie<>();
    public static HashMap<String, String> coreKeys = new HashMap<>();
    public static HashMap<String, Integer> group = new HashMap<>();
    public static HashMap<UUID, HashMap<String, Long>> playerUseTime = new HashMap<>();
    public static List<String> potionKeys = new ArrayList<>();
    public static HashMap<String, String> potionNames = new HashMap<>();
    public static HashMap<String, String> potionLores = new HashMap<>();
    public static Map<String, Potion> potions = new HashMap<>();
    public static HashMap<String, String> messagesHashMap = new HashMap<>();
    public static HashMap<String, String> potionDisplayNameMap = new HashMap<>();

    public static void reloadMyConfig() {
        ap.reloadConfig();
        debug = ap.getConfig().getBoolean("debug");
        prefix = ap.getConfig().getString("prefix").replaceAll("&","§");
        identifier = ap.getConfig().getString("identifier");
        dragoncore = ap.getConfig().getBoolean("dragoncore");
        germplugin = ap.getConfig().getBoolean("germplugin");
        loadGroup();
        loadFiles();
    }

    public static void loadGroup() {
        File file = new File(ap.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> groupKeys = config.getConfigurationSection("group").getKeys(false);
        Set<String> dragoncoreKeys = config.getConfigurationSection("dragoncoreKeys").getKeys(false);
        Set<String> germpluginKeys = config.getConfigurationSection("germpluginKeys").getKeys(false);

        group.clear();
        for(String groupKey : groupKeys) {
            int groupTime = config.getInt("group." + groupKey);
            group.put(groupKey, groupTime);
            if(debug) {
                LogInfo.debug("§6----------------------------");
                LogInfo.debug("§a 药水组名称: " + groupKey);
                LogInfo.debug("§a 药水组冷却: " + groupTime);
                LogInfo.debug("§6----------------------------");
            }
        }

        coreKeys.clear();

        if(AttributePotion.DragonCore) {

            for(String dragoncoreKey : dragoncoreKeys) {
                String slotName = config.getString("dragoncoreKeys." + dragoncoreKey);
                coreKeys.put(dragoncoreKey, slotName);
                CoreAPI.registerKey(dragoncoreKey);
                if(debug) {
                    LogInfo.debug("§6----------------------------");
                    LogInfo.debug("§a 按键名称: " + dragoncoreKey);
                    LogInfo.debug("§a 槽位名称: " + slotName);
                    LogInfo.debug("§6----------------------------");
                }
            }

        }

        else if(AttributePotion.GermPlugin) {

            for(String germpluginKey : germpluginKeys) {
                String slotName = config.getString("germpluginKeys." + germpluginKey);
                coreKeys.put(germpluginKey, slotName);
                GermKeyAPI.registerKey(Integer.parseInt(germpluginKey));
            }

        }
    }

    public static List<String> colorStringList(List<String> stringList) {
        List<String> colorList = new ArrayList<>();
        for(String list : stringList) colorList.add(list.replaceAll("&", "§"));
        return colorList;

    }

    public static void trieBuild() {
        File file = new File(ap.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getString("identifier").equalsIgnoreCase("name")) {
            trie.build(potionNames);
        } else {
            trie.build(potionLores);
        }
    }

    private static void loadFiles() {
        potions.clear();
        potionKeys.clear();
        potionNames.clear();
        potionLores.clear();
        potionDisplayNameMap.clear();
        messagesHashMap.clear();
        File potionsFolder = new File(ap.getDataFolder() + "/potions");
        findAllYmlFiles(potionsFolder);
        trieBuild();
        loadMessages();
    }

    private static void loadMessages() {
        File file = new File(ap.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> messages = config.getConfigurationSection("messages").getKeys(false);
        for(String key : messages) {
            String msg = config.getString("messages." + key).replaceAll("&", "§");
            if(msg.equals("")) continue;
            messagesHashMap.put(key, msg);
            if(debug) {
                LogInfo.debug("§a " + key + "信息为: " + msg);
            }
        }
    }

    private static void findAllYmlFiles(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 如果是文件夹则递归查找
                    findAllYmlFiles(file);
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
            String displayName = config.getString(potionKey + ".name").replaceAll("&", "§");
            potionDisplayNameMap.put(potionKey, displayName);
            String name = config.getString(potionKey + ".name").replaceAll("[&§]\\w", "");
            String lore = config.getString(potionKey + ".lore").replaceAll("[&§]\\w", "");
            int time = config.getInt(potionKey + ".time", 0);
            int cooldown = config.getInt(potionKey + ".cooldown", 0);
            String group = config.getString(potionKey + ".group");
            List<String> conditions = config.getStringList(potionKey + ".conditions")
                    .stream()
                    .filter(s -> !s.isEmpty())
                    .map(String::trim)
                    .collect(Collectors.toList());
            boolean shift = config.getBoolean(potionKey + ".shift", false);
            List<String> attributes = colorStringList(config.getStringList(potionKey + ".attributes")).stream()
                    .filter(s -> !s.isEmpty())
                    .map(String::trim)
                    .collect(Collectors.toList());
            boolean consume = config.getBoolean(potionKey + ".consume", true);
            List<String> commands = config.getStringList(potionKey + ".commands").stream()
                    .filter(s -> !s.isEmpty())
                    .map(String::trim)
                    .collect(Collectors.toList());
            commands = commands.stream().map(str -> str.replaceAll("&", "§")).collect(Collectors.toList());

            List<String> endCommands = config.getStringList(potionKey + ".endCommands").stream()
                    .filter(s -> !s.isEmpty())
                    .map(String::trim)
                    .collect(Collectors.toList());
            endCommands = endCommands.stream().map(str -> str.replaceAll("&", "§")).collect(Collectors.toList());
            int rangeValue = config.getInt(potionKey + ".rangeValue", 0);

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

            Potion potion = new Potion(potionKey, name, lore, time, cooldown, group, conditions, shift,
                    effects, potionEffects, attributes, consume, commands, endCommands, options, rangeValue);
            ConfigManager.potionKeys.add(potionKey);
            potionNames.put(name, potionKey);
            potionLores.put(lore, potionKey);
            potions.put(potionKey, potion);

            if(debug) {
                LogInfo.debug("§6----------------------------");
                LogInfo.debug("§a key: " + potionKey);
                LogInfo.debug("§a name: " + name);
                LogInfo.debug("§a Lore: " + lore);
                LogInfo.debug("§a time: " + time);
                LogInfo.debug("§a cooldown: " + cooldown);
                LogInfo.debug("§a group: " + group);
                LogInfo.debug("§a conditions: " + conditions);
                LogInfo.debug("§a shift: " + shift);
                LogInfo.debug("§a effects: " + effects.entrySet());
                LogInfo.debug("§a potions: " + potionEffects.entrySet());
                LogInfo.debug("§a attributes: " + attributes);
                LogInfo.debug("§a consume: " + consume);
                LogInfo.debug("§a commands: " + commands);
                LogInfo.debug("§a endCommands: " + endCommands);
                LogInfo.debug("§a options: " + options);
                LogInfo.debug("§a rangeValue: " + rangeValue);
                LogInfo.debug("§6----------------------------");
            }
        }
    }
}
