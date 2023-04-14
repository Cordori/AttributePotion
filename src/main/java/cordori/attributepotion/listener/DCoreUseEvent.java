package cordori.attributepotion.listener;

import cordori.attributepotion.file.ConfigManager;
import cordori.attributepotion.utils.Potion;
import eos.moe.dragoncore.api.SlotAPI;
import eos.moe.dragoncore.api.event.KeyPressEvent;
import eos.moe.dragoncore.config.Config;
import eos.moe.dragoncore.database.IDataBase;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DCoreUseEvent implements Listener {
    @EventHandler
    public void onKeyPress(KeyPressEvent event) {
        if(!ConfigManager.dragoncore) return;
        long startTime = System.currentTimeMillis();
        String pressKey = event.getKey();
        if(ConfigManager.coreKeys.containsKey(pressKey)) {
            Player player = event.getPlayer();
            String slotName = ConfigManager.coreKeys.get(pressKey);
            if(!Config.getSlotConfig().contains(slotName)) return;
            SlotAPI.getSlotItem(player, slotName, new IDataBase.Callback<ItemStack>() {
                @Override
                public void onResult(ItemStack item) {
                    if (item == null || item.getType() == Material.AIR) return;
                    if((UseEvent.matchItem(item) == null)) return;
                    String key = UseEvent.matchItem(item);
                    Potion potion = ConfigManager.potions.get(key);
                    if(potion.isShift() && !player.isSneaking()) return;
                    String name = potion.getName();
                    String group = potion.getGroup();
                    UUID uuid = player.getUniqueId();
                    long useTime = System.currentTimeMillis();
                    if(ConfigManager.debug) {
                        long endTime = System.currentTimeMillis();
                        long elapsedTime = endTime - startTime;
                        System.out.println("§e 此处断点0消耗的时间：" + elapsedTime + "ms");
                    }
                    //药水组冷却判断
                    if(UseEvent.isGroupOnCooldown(player, uuid, group, useTime)) return;
                    if(ConfigManager.debug) {
                        long endTime = System.currentTimeMillis();
                        long elapsedTime = endTime - startTime;
                        System.out.println("§e 此处断点1消耗的时间：" + elapsedTime + "ms");
                    }
                    //药水条件判断
                    if(UseEvent.isPotionOnCooldown(player, uuid, key, name, potion, useTime)) return;
                    if(ConfigManager.debug) {
                        long endTime = System.currentTimeMillis();
                        long elapsedTime = endTime - startTime;
                        System.out.println("§e 此处断点2消耗的时间：" + elapsedTime + "ms");
                    }
                    //条件判断
                    List<String> conditions = potion.getConditions();
                    if(UseEvent.checkConditions(conditions, player, name)) return;
                    if(ConfigManager.debug) {
                        long endTime = System.currentTimeMillis();
                        long elapsedTime = endTime - startTime;
                        System.out.println("§e 此处断点3消耗的时间：" + elapsedTime + "ms");
                    }
                    //effects效果处理
                    UseEvent.effectsProcess(player, potion);
                    if(ConfigManager.debug) {
                        long endTime = System.currentTimeMillis();
                        long elapsedTime = endTime - startTime;
                        System.out.println("§e 此处断点4耗的时间：" + elapsedTime + "ms");
                    }
                    //属性处理
                    Map<String, Boolean> options = potion.getOptions();
                    UseEvent.attributeProcess(player, potion, key, name, options);
                    if(ConfigManager.debug) {
                        long endTime = System.currentTimeMillis();
                        long elapsedTime = endTime - startTime;
                        System.out.println("§e 此处断点5消耗的时间：" + elapsedTime + "ms");
                    }
                    //添加冷却
                    //添加冷却
                    if(!ConfigManager.cooldown.containsKey(uuid)) {
                        ConfigManager.cooldown.put(uuid, new HashMap<>());
                    }
                    ConfigManager.cooldown.get(uuid).put(key, useTime);
                    ConfigManager.cooldown.get(uuid).put(group, useTime);
                    //判断是否消耗
                    if(potion.isConsume()) {
                        if (item.getAmount() >= 1) {
                            item.setAmount(item.getAmount()-1);
                            SlotAPI.setSlotItem(player, slotName, item, true);
                        }
                    }
                    if(ConfigManager.debug) {
                        long endTime = System.currentTimeMillis();
                        long elapsedTime = endTime - startTime;
                        System.out.println("§e 此处断点6消耗的时间：" + elapsedTime + "ms");
                    }
                    //设置物品冷却
                    if(item.getType() != Material.AIR && !options.isEmpty() && options.containsKey("cool")) {
                        if (options.get("cool")) player.setCooldown(item.getType(), potion.getCooldown() * 20);
                    }
                    if(ConfigManager.debug) {
                        long endTime = System.currentTimeMillis();
                        long elapsedTime = endTime - startTime;
                        System.out.println("§e 此处断点7消耗的时间：" + elapsedTime + "ms");
                    }
                    //处理指令
                    UseEvent.commandsProcess(potion, player);
                    if(ConfigManager.debug) {
                        long endTime = System.currentTimeMillis();
                        long elapsedTime = endTime - startTime;
                        System.out.println("§e DragonCore的药水使用事件消耗的时间：" + elapsedTime + "ms");
                    }
                }

                @Override
                public void onFail() {
                    player.sendMessage("§c槽位物品获取为空！");
                }
            });
        }
    }
}
