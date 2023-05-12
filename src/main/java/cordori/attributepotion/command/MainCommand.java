package cordori.attributepotion.command;

import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.file.ConfigManager;
import cordori.attributepotion.listener.UseEvent;
import cordori.attributepotion.utils.Potion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainCommand implements CommandExecutor, TabCompleter {

    private static final AttributePotion ap = AttributePotion.getInstance();

    private List<String> filter(List<String> list, String latest) {
        if (list.isEmpty() || latest == null)
            return list;
        String ll = latest.toLowerCase();
        list.removeIf(k -> !k.toLowerCase().startsWith(ll));
        return list;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String latest = null;
        List<String> list = new ArrayList<>();
        if (args.length != 0) {
            latest = args[args.length - 1];
        }
        if (args.length == 1) {
            list.add("reload");
            list.add("addPotion");
        } else if (args.length == 2) {
            String playerName = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(playerName)) {
                    list.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            String potionName = args[2];
            list.addAll(filter(ConfigManager.potionKeys, potionName));
        }
        return filter(list, latest);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Bukkit.getScheduler().runTaskAsynchronously(ap, () -> {

            if(args.length == 0) {
                sender.sendMessage(ConfigManager.prefix + "§c参数不足捏~");
                return;
            }

            if(args[0].equalsIgnoreCase("reload")) {

                ConfigManager.reloadMyConfig();
                if(ConfigManager.messagesHashMap.containsKey("reload")) {
                    sender.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("reload"));
                }

            }

            else if (args[0].equalsIgnoreCase("addPotion")) {

                if(args.length <= 2) {
                    sender.sendMessage(ConfigManager.prefix + "§c参数不足捏~");
                    return;
                }

                String playerName = args[1];
                Player player = Bukkit.getPlayer(playerName);
                if (player == null) {
                    sender.sendMessage(ConfigManager.prefix + "§c该玩家不在线！");
                    return;
                }

                String potionKey = args[2];
                if (!ConfigManager.potionKeys.contains(potionKey)) {
                    sender.sendMessage(ConfigManager.prefix + "§c无效的药水节点名！");
                    return;
                }

                long startTime = System.currentTimeMillis();
                commandUsePotion(player, potionKey, startTime);

            } else {
                sender.sendMessage(ConfigManager.prefix + "§c无效的指令！");
            }
        });

        return false;
    }

    private static void commandUsePotion(Player player, String key, long startTime) {

        Potion potion = ConfigManager.potions.get(key);
        String name = potion.getName();
        String group = potion.getGroup();
        UUID uuid = player.getUniqueId();
        long useTime = System.currentTimeMillis();

        //药水组冷却判断
        if(UseEvent.isGroupOnCooldown(player, uuid, group, useTime)) return;

        //药水条件判断
        if(UseEvent.isPotionOnCooldown(player, uuid, key, name, potion, useTime)) return;

        //条件判断
        if(UseEvent.meetConditions(potion.getConditions(), player, name)) return;

        //处理药水效果
        UseEvent.potionEffectsProcess(player, potion);

        // 处理属性
        UseEvent.attributeProcess(player, potion.getTime(), key, name, potion.getAttributes(), useTime, group);

        //effects效果处理
        UseEvent.effectsProcess(player, potion);

        ConfigManager.playerUseTime.get(uuid).put(key, useTime);
        ConfigManager.playerUseTime.get(uuid).put(group, useTime);

        //处理指令
        UseEvent.commandsProcess(potion, player);

        if(ConfigManager.debug) {
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            System.out.println("§e command的药水使用事件消耗的时间：" + elapsedTime + "ms");
        }
    }
}
