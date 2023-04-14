package cordori.attributepotion.listener;

import com.sucy.skill.SkillAPI;
import com.sucy.skill.api.enums.ManaSource;
import com.sucy.skill.api.player.PlayerData;
import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.file.ConfigManager;
import cordori.attributepotion.hook.APHook;
import cordori.attributepotion.hook.SXHook;
import cordori.attributepotion.utils.Potion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;

public class UseEvent implements Listener {
    private static final AttributePotion ap = AttributePotion.getInstance();
    private static final ScriptEngineManager mgr = new ScriptEngineManager();
    private static final ScriptEngine engine = mgr.getEngineByName("nashorn");

    /*
    判断动作是否符合
    判断药水组是否冷却中
    判断药水是否冷却中
    判断条件是否满足
    添加属性并记录冷却时间
    执行effects内容
    判断是否消耗
    设置物品冷却
    执行指令
    */

    public static boolean checkItem(ItemStack item) { return item != null && item.getType() != Material.AIR; }
    public static String matchItem(ItemStack item) {
        String key;
        /*
        if(ConfigManager.potionKeys.size() <= 50) {
            if(ConfigManager.identifier.equalsIgnoreCase("name")) {
                String itemName = item.getItemMeta().getDisplayName();
                if(itemName == null) return null;
                for (String name : ConfigManager.potionNames.keySet()) {
                    if (itemName.contains(name)) {
                        key = ConfigManager.potionNames.get(name);
                        return key;
                    }
                }
            }
            else if(ConfigManager.identifier.equalsIgnoreCase("lore")) {
                List<String> itemLores = item.getItemMeta().getLore();
                if(itemLores == null) return null;
                for(String itemLore : itemLores) {
                    for(String lore : ConfigManager.potionLores.keySet()) {
                        if(itemLore.contains(lore)) {
                            key = ConfigManager.potionLores.get(lore);
                            return key;
                        }
                    }
                }
            }
        } else {

         */
        if(ConfigManager.identifier.equalsIgnoreCase("name")) {
            String text = item.getItemMeta().getDisplayName();
            if (text == null) return null;
            key = ConfigManager.trie.parseText(text)
                    .stream()
                    .map(hit -> hit.value)
                    .findFirst()
                    .orElse(null);
            return key;
        } else if(ConfigManager.identifier.equalsIgnoreCase("lore")) {
            if(item.getItemMeta().getLore() == null) return null;
            String text = String.join(" ", item.getItemMeta().getLore());
            key = ConfigManager.trie.parseText(text)
                    .stream()
                    .map(hit -> hit.value)
                    .findFirst()
                    .orElse(null);
            return key;
        } else return null;
    }
    public static boolean isGroupOnCooldown(Player player, UUID uuid, String group, long useTime) {
        long lastGroupTime = 0;
        if(ConfigManager.cooldown.containsKey(uuid)) {
            if(ConfigManager.cooldown.get(uuid).containsKey(group)) {
                lastGroupTime = ConfigManager.cooldown.get(uuid).get(group);
            }
        }
        if(lastGroupTime != 0) {
            int groupCooldown = ConfigManager.group.get(group);
            if((useTime - lastGroupTime) / 1000 <= groupCooldown) {
                player.sendMessage(ConfigManager.prefix + ap.getConfig()
                        .getString("messages.onGroupCooldown")
                        .replace("%group%", group)
                        .replace("%cooldown%", String.valueOf(groupCooldown - (useTime - lastGroupTime) / 1000))
                        .replaceAll("&", "§"));
                return true;
            }
        }
        return false;
    }
    public static boolean isPotionOnCooldown(Player player, UUID uuid, String key, String name, Potion potion, long useTime) {
        long lastPotionTime = 0;
        if(ConfigManager.cooldown.containsKey(uuid)) {
            if(ConfigManager.cooldown.get(uuid).containsKey(key)) {
                lastPotionTime = ConfigManager.cooldown.get(uuid).get(key);
            }
        }
        if(lastPotionTime != 0) {
            int potionCooldown = potion.getCooldown();
            if((useTime - lastPotionTime) / 1000 <= potionCooldown) {
                player.sendMessage(ConfigManager.prefix + ap.getConfig()
                        .getString("messages.onPotionCooldown")
                        .replace("%potion%", name)
                        .replace("%cooldown%", String.valueOf(potionCooldown - (useTime - lastPotionTime) / 1000))
                        .replaceAll("&", "§"));
                return true;
            }
        }
        return false;
    }
    public static boolean checkConditions(List<String> conditions, Player player, String name) {
        if(conditions.isEmpty()) return true;
        for (String condition : conditions) {
            if (condition.contains("permission:")) {
                condition = condition.substring(10);
                if(!player.hasPermission(condition)) {
                    player.sendMessage(ConfigManager.prefix + ap.getConfig()
                            .getString("messages.useDeny").replaceAll("&", "§"));
                    return true;
                }
            } else {
                try {
                    condition = PlaceholderAPI.setPlaceholders(player, condition);
                    if(!(boolean)engine.eval(condition)) {
                        player.sendMessage(ConfigManager.prefix + ap.getConfig()
                                .getString("messages.useDeny").replaceAll("&", "§"));
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

                    @Override
                    public void run() {
                        double health = player.getHealth();
                        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                        if (time > 0) {
                            player.setHealth(Math.min(health + valueA, maxHealth));
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

                        @Override
                        public void run() {
                            if (!player.isOnline()) { // 判断玩家是否在线
                                cancel();
                            }
                            if (time > 0) {
                                PlayerData localPlayerData = SkillAPI.getPlayerData(player);
                                localPlayerData.giveMana(valueA, ManaSource.COMMAND);
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

                    @Override
                    public void run() {
                        if (!player.isOnline()) { // 判断玩家是否在线
                            cancel();
                        }
                        int currentFoodLevel = player.getFoodLevel();

                        if (time > 0) {
                            player.setFoodLevel(Math.min(currentFoodLevel + valueA, 20));
                            time--;
                        }
                        if (time == 0) {
                            cancel();
                        }
                    }
                }.runTaskTimerAsynchronously(ap, 0L, 20L);
            }
        }
    }
    public static void attributeProcess(Player player, Potion potion, String key, String name, Map<String, Boolean> options) {
        int time = potion.getTime();
        List<String> attributes = potion.getAttributes();
        if(!attributes.isEmpty()) {
            List<String> attrList = new ArrayList<>(PlaceholderAPI.setPlaceholders(player, attributes));
            if(AttributePotion.AttributePlus) {
                APHook.addAPAttribute(player, options, attrList, key, time);
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(ap, (() -> SXHook.addSXAttribute(player, key, attrList)));
            }
            //到时清除属性源
            if(time>0) {
                new BukkitRunnable() {
                    public void run() {
                        if(AttributePotion.AttributePlus) {
                            APHook.takeAPAttribute(player, key);
                        } else {
                            SXHook.takeSXAttribute(player, key);
                        }

                        if (player.isOnline()) {
                            player.sendMessage(ConfigManager.prefix + ap
                                    .getConfig()
                                    .getString("messages.outPotion")
                                    .replaceAll("%player%", player.getName())
                                    .replaceAll("%potion%", name)
                                    .replaceAll("&", "§"));
                        }
                    }
                }.runTaskLaterAsynchronously(ap, time * 20L);
            }
        }
        player.sendMessage(ConfigManager.prefix + ap.getConfig()
                .getString("messages.usePotion")
                .replace("%player%", player.getName())
                .replace("%potion%", name)
                .replace("%time%", String.valueOf(time))
                .replaceAll("&", "§"));
    }
    public static void isConsume(Potion potion, ItemStack item, Player player, Map<String, Boolean> options) {
        if(potion.isConsume()) {
            if (item.getAmount() >= 1) {
                item.setAmount(item.getAmount() - 1);
            }
        }
        //设置物品冷却
        if(!options.isEmpty() && options.containsKey("cool")) {
            if (options.get("cool")) player.setCooldown(item.getType(), potion.getCooldown() * 20);
        }
    }
    public static void commandsProcess(Potion potion, Player player) {
        List<String> commands = potion.getCommands();
        if (!commands.isEmpty()) {
            final String playerName = player.getName();
            CommandSender sender;
            for (String command : commands) {
                if (command.startsWith("[console]")) {
                    command = command.substring(9).replace("%player%", playerName);
                    sender = Bukkit.getConsoleSender();
                } else {
                    sender = player;
                }
                Bukkit.dispatchCommand(sender, command);
            }
        }
    }

    @EventHandler
    public void onPlayerUsePotion(PlayerInteractEvent event) {
        long startTime = System.currentTimeMillis();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if(!checkItem(item)) return;

        //识别物品环节
        if(matchItem(item) == null) return;
        String key = matchItem(item);
        if(ConfigManager.debug) {
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("§e 此处断点0消耗的时间：" + elapsedTime + "ms");
        }

        Potion potion = ConfigManager.potions.get(key);
        String name = potion.getName();
        String group = potion.getGroup();
        //如果不是右键空气或者方块，终止
        if(event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        //当右键了可交互方块时，不使用药水，终止
        if (!event.useInteractedBlock().equals(PlayerInteractEvent.Result.DENY) &&
             event.getClickedBlock() != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            String blockName = event.getClickedBlock().getType().toString().toUpperCase();
            if (BLOCKED_MATERIALS.contains(blockName)) return;
        }
        //如果是需要按下shift使用的
        if(potion.isShift() && !player.isSneaking()) return;
        if(ConfigManager.debug) {
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("§e 此处断点1消耗的时间：" + elapsedTime + "ms");
        }

        UUID uuid = player.getUniqueId();
        long useTime = System.currentTimeMillis();
        //药水组冷却中
        if(isGroupOnCooldown(player, uuid, group, useTime)) return;

        //药水冷却中
        if(isPotionOnCooldown(player, uuid, key, name, potion, useTime)) return;
        if(ConfigManager.debug) {
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("§e 此处断点2消耗的时间：" + elapsedTime + "ms");
        }

        //不满足使用条件
        List<String> conditions = potion.getConditions();
        if(checkConditions(conditions, player, name)) return;

        //处理effects
        effectsProcess(player, potion);
        if(ConfigManager.debug) {
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("§e 此处断点3消耗的时间：" + elapsedTime + "ms");
        }

        //处理属性lore中的变量与运算并添加属性
        Map<String, Boolean> options = potion.getOptions();
        attributeProcess(player, potion, key, name, options);
        if(ConfigManager.debug) {
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("§e 此处断点4消耗的时间：" + elapsedTime + "ms");
        }
        //添加冷却
        if(!ConfigManager.cooldown.containsKey(uuid)) {
            ConfigManager.cooldown.put(uuid, new HashMap<>());
        }
        ConfigManager.cooldown.get(uuid).put(key, useTime);
        ConfigManager.cooldown.get(uuid).put(group, useTime);

        if(ConfigManager.debug) {
            System.out.println(ConfigManager.cooldown);
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("§e 此处断点5消耗的时间：" + elapsedTime + "ms");
        }

        //判断是否消耗
        isConsume(potion, item, player, options);
        if(ConfigManager.debug) {
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("§e 此处断点6消耗的时间：" + elapsedTime + "ms");
        }

        //处理指令
        commandsProcess(potion, player);
        if(ConfigManager.debug) {
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("§e Vanilla的药水使用事件消耗的时间：" + elapsedTime + "ms");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(ap, () -> APHook.delAPAttribute(event.getPlayer()));
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
