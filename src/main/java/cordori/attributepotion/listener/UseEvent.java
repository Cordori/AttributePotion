package cordori.attributepotion.listener;

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.file.ConfigManager;
import cordori.attributepotion.file.SQLManager;
import cordori.attributepotion.hook.APHook;
import cordori.attributepotion.hook.PAPIHook;
import cordori.attributepotion.hook.SXHook;
import cordori.attributepotion.hook.SkillAPIHook;
import cordori.attributepotion.utils.Potion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;

public class UseEvent implements Listener {
    private static final AttributePotion ap = AttributePotion.getInstance();
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
    public static final HashMap<UUID, HashMap<String, List<String>>> attributeMap = new HashMap<>();

    /**
    *判断动作是否符合
    *判断药水组是否冷却中
    *判断药水是否冷却中
    *判断条件是否满足
    *添加属性并记录冷却时间
    *执行effects内容
    *判断是否消耗
    *设置物品冷却
    *执行指令
    */

    public static boolean isAir(ItemStack item) { return item == null || item.getType() == Material.AIR; }
    public static String matchItem(ItemStack item) {
        String key;
        if (ConfigManager.identifier.equalsIgnoreCase("name")) {
            String text = item.getItemMeta().getDisplayName();
            if (text == null) return null;
            List<AhoCorasickDoubleArrayTrie.Hit<String>> hits = ConfigManager.trie.parseText(text);
            if (hits.isEmpty()) return null;
            key = hits.stream()
                    .map(hit -> hit.value)
                    .max(Comparator.comparingInt(String::length))
                    .orElse(null);
            return key;
        } else if (ConfigManager.identifier.equalsIgnoreCase("lore")) {
            if (item.getItemMeta().getLore() == null) return null;
            String text = String.join(" ", item.getItemMeta().getLore());
            List<AhoCorasickDoubleArrayTrie.Hit<String>> hits = ConfigManager.trie.parseText(text);
            if (hits.isEmpty()) return null;
            key = hits.stream()
                    .map(hit -> hit.value)
                    .max(Comparator.comparingInt(String::length))
                    .orElse(null);
            return key;
        } else {
            return null;
        }
    }
    public static boolean isGroupOnCooldown(Player player, UUID uuid, String group, long useTime) {
        // 如果这个药水的药水组设置为空或者不是已设置的药水组，终止
        if (group == null || !ConfigManager.group.containsKey(group)) return false;

        // 获取上次使用药水的时间
        long lastGroupTime = 0;
        if (ConfigManager.playerUseTime.get(uuid).containsKey(group)) {
            lastGroupTime = ConfigManager.playerUseTime.get(uuid).get(group);
        }

        // 计算当前使用时间与上次使用时间之差是否小于药水组冷却时间
        if (lastGroupTime != 0) {
            int groupCooldown = ConfigManager.group.get(group);

            if ((useTime - lastGroupTime) / 1000 <= groupCooldown) {
                if(ConfigManager.messagesHashMap.containsKey("onGroupCooldown")) {
                    player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("onGroupCooldown")
                            .replace("%group%", group)
                            .replace("%cooldown%", String.valueOf(groupCooldown - (useTime - lastGroupTime) / 1000))
                    );
                }

                return true;
            }
        }

        return false;
    }
    public static boolean isPotionOnCooldown(Player player, UUID uuid, String key, String name, Potion potion, long useTime) {
        long lastPotionTime = 0;

        // 获取玩家上次使用该药水的时间
        if (ConfigManager.playerUseTime.get(uuid).containsKey(key)) {
            lastPotionTime = ConfigManager.playerUseTime.get(uuid).get(key);
        }

        // 计算当前使用时间与上次使用时间之差是否小于药水冷却时间
        if (lastPotionTime != 0) {
            int potionCooldown = potion.getCooldown();

            if ((useTime - lastPotionTime) / 1000 <= potionCooldown) {
                if(ConfigManager.messagesHashMap.containsKey("onPotionCooldown")) {
                    player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("onPotionCooldown")
                            .replace("%potion%", name)
                            .replace("%cooldown%", String.valueOf(potionCooldown - (useTime - lastPotionTime) / 1000))
                    );
                }

                return true;
            }
        }

        return false;
    }
    public static boolean meetConditions(List<String> conditions, Player player, String name) {

        if(conditions.isEmpty()) return false;
        conditions = PAPIHook.papiProcess(player, conditions);

        for (String condition : conditions) {

            if (condition.startsWith("permission:")) {
                condition = condition.substring(11);
                if(!player.hasPermission(condition)) {
                    if(ConfigManager.messagesHashMap.containsKey("useDeny")) {
                        player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("useDeny"));
                    }
                    return true;
                }
            } else {
                try {
                    if(!(boolean)engine.eval(condition)) {
                        if(ConfigManager.messagesHashMap.containsKey("useDeny")) {
                            player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("useDeny"));
                        }
                        return true;
                    }
                } catch (ScriptException e) {
                    ap.getLogger().warning("尝试解析 " + name +" §e条件变量或表达式失败，请检查配置！");
                    if(ConfigManager.debug) throw new RuntimeException(e);
                }
            }
        }

        return false;
    }
    public static void effectsProcess(Player player, Potion potion) {

        Map<String, String> effects = potion.getEffects();

        if(!effects.isEmpty()) {

            if(effects.containsKey("health")) {
                new BukkitRunnable() {
                    final String value = effects.get("health");
                    final String[] valueArray = value.split(":");
                    final int valueA = Integer.parseInt(valueArray[0]);
                    final int valueB = Integer.parseInt(valueArray[1]);
                    int time = valueB;
                    final String healthMode = effects.get("healthMode") == null ? "0" : effects.get("healthMode");

                    @Override
                    public void run() {
                        double health = player.getHealth();
                        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

                        if (time > 0) {
                            if(valueA>0) {
                                switch (healthMode) {
                                    case "0":
                                        player.setHealth(Math.min(health + valueA, maxHealth));
                                        break;
                                    case "1":
                                        player.setHealth(Math.min(health + health * valueA / 100, maxHealth));
                                        break;
                                    case "2":
                                        player.setHealth(Math.min(health + maxHealth * valueA / 100, maxHealth));

                                        break;
                                }
                                System.out.println(player.getHealth());

                            } else {
                                // 比如-10:5,每秒扣除当前生命的10%
                                switch (healthMode) {
                                    case "0":
                                        if (health + valueA <= 0) {
                                            player.setHealth(0);
                                            cancel();
                                        } else {
                                            player.setHealth(health + valueA);
                                        }
                                        break;
                                    case "1":
                                        player.setHealth(health + health * valueA / 100);
                                        break;
                                    case "2":
                                        if(health + maxHealth * valueA / 100 <= 0) {
                                            player.setHealth(0);
                                            cancel();
                                        } else {
                                            player.setHealth(health + maxHealth * valueA / 100);
                                        }
                                        break;
                                }
                            }
                            time--;
                        }

                        if (time == 0) {
                            cancel();
                        }
                    }
                }.runTaskTimer(ap, 0L, 20L);
            }

            if(effects.containsKey("mana")) {
                if(AttributePotion.Skillapi) {
                    new BukkitRunnable() {
                        final String value = effects.get("mana");
                        final String[] valueArray = value.split(":");
                        final int valueA = Integer.parseInt(valueArray[0]);
                        final int valueB = Integer.parseInt(valueArray[1]);
                        int time = valueB;
                        final String manaMode = effects.get("manaMode") == null ? "0" : effects.get("manaMode");

                        @Override
                        public void run() {

                            if (!player.isOnline()) { // 判断玩家是否在线
                                cancel();
                            }

                            if (time > 0) {
                                SkillAPIHook.giveMana(player, valueA, manaMode);
                                time--;
                            }
                            if (time == 0) {
                                cancel();
                            }
                        }
                    }.runTaskTimerAsynchronously(ap, 0L, 20L);
                }
            }

            if(effects.containsKey("hunger")) {
                new BukkitRunnable() {
                    final String value = effects.get("hunger");
                    final String[] valueArray = value.split(":");
                    final int valueA = Integer.parseInt(valueArray[0]);
                    final int valueB = Integer.parseInt(valueArray[1]);
                    int time = valueB;
                    final String hungerMode = effects.get("hungerMode") == null ? "0" : effects.get("hungerMode");
                    @Override
                    public void run() {
                        if (!player.isOnline()) { // 判断玩家是否在线
                            cancel();
                        }
                        int currentFoodLevel = player.getFoodLevel();

                        if (time > 0) {
                            switch (hungerMode) {
                                case "0":
                                    player.setFoodLevel(Math.min(currentFoodLevel + valueA, 20));
                                    break;
                                case "1":
                                    player.setFoodLevel(currentFoodLevel + currentFoodLevel * valueA / 100);
                                    break;
                            }

                            time--;
                        }
                        if (time == 0) {
                            cancel();
                        }
                    }
                }.runTaskTimer(ap, 0L, 20L);
            }

        }
    }
    public static void potionEffectsProcess(Player player, Potion potion) {

        Map<String, String> potionEffects = potion.getPotionEffects();

        if(!potionEffects.isEmpty()) {

            for (String potionEffect : potionEffects.keySet()) {
                final String value = potionEffects.get(potionEffect);
                final String[] valueArray = value.split(":");
                final int valueA = Integer.parseInt(valueArray[0]);
                final int valueB = Integer.parseInt(valueArray[1]);

                PotionEffect pe = new PotionEffect(PotionEffectType.getByName(potionEffect), valueB * 20, valueA);
                Bukkit.getScheduler().runTask(ap, () -> pe.apply(player));
            }
        }
    }
    public static void attributeProcess(Player player, int time, String key, String name, List<String> attrList, long useTime, String group) {

        if(attrList.isEmpty()) return;

        UUID uuid = player.getUniqueId();
        attrList = PAPIHook.papiProcess(player, attrList);
        attributeMap.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, attrList);

        if(AttributePotion.AttributePlus) {
            APHook.addAPAttribute(player, attrList, key);
        } else if(AttributePotion.SXAttribute){
            SXHook.addSXAttribute(player);
        }

        // 插入数据到数据库
        SQLManager.sql.insert(String.valueOf(uuid), key, attrList, useTime, group);

        // 到时清除属性源
        if(time>0) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(ap, () -> {

                attributeMap.computeIfPresent(uuid, (k, attrMap) -> {
                    attrMap.remove(key);
                    return attrMap;
                });

                if(AttributePotion.AttributePlus) {
                    APHook.takeAPAttribute(player, key);
                } else if(AttributePotion.SXAttribute){
                    SXHook.takeSXAttribute(player);
                }

                // 从数据库删除数据
                SQLManager.sql.delete(String.valueOf(uuid), key);

                if (player.isOnline()) {
                    if(ConfigManager.messagesHashMap.containsKey("outPotion")) {
                        player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("outPotion")
                                .replaceAll("%player%", player.getName())
                                .replaceAll("%potion%", name)
                        );
                    }
                }
            }, time * 20L);
        }

        if(ConfigManager.messagesHashMap.containsKey("usePotion")) {
            player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("usePotion")
                    .replace("%player%", player.getName())
                    .replace("%potion%", name)
                    .replace("%time%", String.valueOf(time))
            );
        }
    }

    public static void isConsume(Potion potion, ItemStack item) {
        if(potion.isConsume()) {
            if (item.getAmount() >= 1) {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }
    public static void optionsProcess(Player player, Potion potion, ItemStack item) {

        Map<String, Boolean> options = potion.getOptions();

        if(!options.isEmpty() && !isAir(item)) {
            //设置物品冷却
            if(options.containsKey("cool") && options.get("cool")) {
                player.setCooldown(item.getType(), potion.getCooldown() * 20);
            }
        }

    }
    public static void commandsProcess(Potion potion, Player player) {
        List<String> commands = potion.getCommands();

        if (commands.isEmpty())  return;

        final String playerName = player.getName();
        CommandSender sender;
        for (String command : commands) {
            if (command.startsWith("[console]")) {
                command = command.substring(9).replace("%player%", playerName);
                sender = Bukkit.getConsoleSender();
            } else {
                sender = player;
            }

            CommandSender finalSender = sender;
            String finalCommand = command;
            Bukkit.getScheduler().runTask(ap, () -> Bukkit.dispatchCommand(finalSender, finalCommand));

        }

    }

    @EventHandler
    public void onPlayerUsePotion(PlayerInteractEvent event) {

        Bukkit.getScheduler().runTaskAsynchronously(ap, () -> {

            long startTime = System.currentTimeMillis();
            Player player = event.getPlayer();
            ItemStack item = event.getItem();

            //物品为空，终止
            if(isAir(item)) return;

            //如果不是右键空气或者方块，终止
            if(event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

            //当右键了可交互方块时，不使用药水，终止
            if (!event.useInteractedBlock().equals(PlayerInteractEvent.Result.DENY) &&
                    event.getClickedBlock() != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                String blockName = event.getClickedBlock().getType().toString().toUpperCase();
                if (BLOCKED_MATERIALS.contains(blockName)) return;
            }

            //识别物品环节
            String key = matchItem(item);
            if(key == null) return;

            Potion potion = ConfigManager.potions.get(key);

            //如果是需要按下shift使用的
            if(potion.isShift() && !player.isSneaking()) return;

            UUID uuid = player.getUniqueId();
            long useTime = System.currentTimeMillis();

            String group = potion.getGroup();

            // 判断药水组是否处于冷却
            if(isGroupOnCooldown(player, uuid, group, useTime)) return;

            String name = potion.getName();

            // 判断药水是否处于冷却
            if(isPotionOnCooldown(player, uuid, key, name, potion, useTime)) return;

            // 判断是否满足条件
            if(meetConditions(potion.getConditions(), player, name)) return;

            // 判断是否消耗
            isConsume(potion, item);

            // 处理药水效果
            potionEffectsProcess(player, potion);

            // 处理属性
            attributeProcess(player, potion.getTime(), key, name, potion.getAttributes(), useTime, group);

            // 处理effects
            effectsProcess(player, potion);

            // 添加冷却
            ConfigManager.playerUseTime.get(uuid).put(key, useTime);
            ConfigManager.playerUseTime.get(uuid).put(group, useTime);

            // 选项处理
            optionsProcess(player, potion, item);

            // 处理指令
            commandsProcess(potion, player);

            if(ConfigManager.debug) {
                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;
                System.out.println("§e Vanilla的药水使用事件消耗的时间：" + elapsedTime + "ms");
            }

        });
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        Bukkit.getScheduler().runTaskAsynchronously(ap, () -> {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            if(attributeMap.get(uuid).isEmpty()) return;
            for(String key : attributeMap.get(uuid).keySet()) {

                if(ConfigManager.potions.get(key).getOptions().containsKey("death")) {
                    if(ConfigManager.potions.get(key).getOptions().get("death")) {
                        attributeMap.get(uuid).remove(key);
                        if(AttributePotion.AttributePlus) {
                            APHook.takeAPAttribute(player, key);
                        } else if(AttributePotion.SXAttribute){
                            SXHook.takeSXAttribute(player);
                        }

                        // 从数据库删除数据
                        SQLManager.sql.delete(uuid.toString(), key);
                    }
                }

            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Bukkit.getScheduler().runTaskAsynchronously(ap, () -> {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            attributeMap.remove(uuid);
        });

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Bukkit.getScheduler().runTaskAsynchronously(ap, () -> {

            long startTime = System.currentTimeMillis();
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            String uid = uuid.toString();
            if(!ConfigManager.playerUseTime.containsKey(uuid)) {
                ConfigManager.playerUseTime.put(uuid, new HashMap<>());
            }

            HashMap<String, List<Object>> potionData = SQLManager.sql.getPotionData(uid);
            if(potionData == null) return;

            HashMap<String, Long> groupTimeMap = new HashMap<>();
            long lastGroupTime = 0;

            for(String key : potionData.keySet()) {
                String str = (String) potionData.get(key).get(0);
                List<String> attrList = Arrays.asList(str.split(","));
                String group = (String) potionData.get(key).get(1);

                long lastTime = (long) potionData.get(key).get(2);

                if(lastTime > lastGroupTime) {
                    lastGroupTime = lastTime;
                    groupTimeMap.put(group, lastGroupTime);
                }

                long currentTime = System.currentTimeMillis();
                int time = ConfigManager.potions.get(key).getTime();

                if(ConfigManager.debug) {
                    System.out.println(str);
                    System.out.println(attrList);
                    System.out.println(group);
                }

                if(currentTime - lastTime < time * 1000L) {
                    attributeMap.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, attrList);
                    int remainTime = (int) (time - (currentTime - lastTime) / 1000);
                    if(AttributePotion.AttributePlus) {
                        APHook.addAPAttribute(player, attrList, key);
                    } else if(AttributePotion.SXAttribute){
                        SXHook.addSXAttribute(player);
                    }

                    if(time>0) {
                        Bukkit.getScheduler().runTaskLaterAsynchronously(ap, () -> {

                            attributeMap.computeIfPresent(uuid, (k, attrMap) -> {
                                attrMap.remove(key);
                                return attrMap;
                            });

                            if(AttributePotion.AttributePlus) {
                                APHook.takeAPAttribute(player, key);
                            } else if(AttributePotion.SXAttribute){
                                SXHook.takeSXAttribute(player);
                            }

                            // 从数据库删除数据
                            SQLManager.sql.delete(uid, key);

                            String name = ConfigManager.potions.get(key).getName();
                            if (player.isOnline()) {
                                if(ConfigManager.messagesHashMap.containsKey("outPotion")) {
                                    player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("outPotion")
                                            .replaceAll("%player%", player.getName())
                                            .replaceAll("%potion%", name)
                                    );
                                }
                            }
                        }, remainTime * 20L);
                    }

                } else {
                    SQLManager.sql.delete(uid, key);
                }
            }

            for(String group : groupTimeMap.keySet()) {
                ConfigManager.playerUseTime.get(uuid).put(group, lastGroupTime);
            }

            if(ConfigManager.debug) {
                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;
                System.out.println("§e 进服药水效果重新获取事件消耗的时间：" + elapsedTime + "ms");
            }

        });

    }

    private static final Set<String> BLOCKED_MATERIALS = newHashSet(
            "FURNACE", "CHEST", "TRAPPED_CHEST", "BEACON", "DISPENSER", "DROPPER", "HOPPER",
            "WORKBENCH", "ENCHANTMENT_TABLE", "ENDER_CHEST", "ANVIL", "BED_BLOCK", "FENCE_GATE",
            "SPRUCE_FENCE_GATE", "BIRCH_FENCE_GATE", "ACACIA_FENCE_GATE", "JUNGLE_FENCE_GATE",
            "DARK_OAK_FENCE_GATE", "IRON_DOOR_BLOCK", "WOODEN_DOOR", "SPRUCE_DOOR", "BIRCH_DOOR",
            "JUNGLE_DOOR", "ACACIA_DOOR", "DARK_OAK_DOOR", "WOOD_BUTTON", "STONE_BUTTON", "TRAP_DOOR",
            "IRON_TRAPDOOR", "DIODE_BLOCK_OFF", "DIODE_BLOCK_ON", "REDSTONE_COMPARATOR_OFF",
            "REDSTONE_COMPARATOR_ON", "FENCE", "SPRUCE_FENCE", "BIRCH_FENCE", "JUNGLE_FENCE",
            "DARK_OAK_FENCE", "ACACIA_FENCE", "NETHER_FENCE", "BREWING_STAND", "CAULDRON",
            "LEGACY_SIGN_POST", "LEGACY_WALL_SIGN", "LEGACY_SIGN", "ACACIA_SIGN", "ACACIA_WALL_SIGN",
            "BIRCH_SIGN", "BIRCH_WALL_SIGN", "DARK_OAK_SIGN", "DARK_OAK_WALL_SIGN", "JUNGLE_SIGN",
            "JUNGLE_WALL_SIGN", "OAK_SIGN", "OAK_WALL_SIGN", "SPRUCE_SIGN", "SPRUCE_WALL_SIGN", "LEVER",
            "BLACK_SHULKER_BOX", "BLUE_SHULKER_BOX", "BROWN_SHULKER_BOX", "CYAN_SHULKER_BOX",
            "GRAY_SHULKER_BOX", "GREEN_SHULKER_BOX", "LIGHT_BLUE_SHULKER_BOX", "LIME_SHULKER_BOX",
            "MAGENTA_SHULKER_BOX", "ORANGE_SHULKER_BOX", "PINK_SHULKER_BOX", "PURPLE_SHULKER_BOX",
            "RED_SHULKER_BOX", "SILVER_SHULKER_BOX", "WHITE_SHULKER_BOX", "YELLOW_SHULKER_BOX",
            "DAYLIGHT_DETECTOR_INVERTED", "DAYLIGHT_DETECTOR", "BARREL", "BLAST_FURNACE", "SMOKER",
            "CARTOGRAPHY_TABLE", "COMPOSTER", "GRINDSTONE", "LECTERN", "LOOM", "STONECUTTER", "BELL",
            "BEEHIVE"
    );
}
