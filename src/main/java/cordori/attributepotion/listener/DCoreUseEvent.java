package cordori.attributepotion.listener;

import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.file.ConfigManager;
import cordori.attributepotion.hook.PAPIHook;
import cordori.attributepotion.utils.LogInfo;
import cordori.attributepotion.utils.Potion;
import eos.moe.dragoncore.api.SlotAPI;
import eos.moe.dragoncore.api.event.KeyPressEvent;
import eos.moe.dragoncore.config.Config;
import eos.moe.dragoncore.database.IDataBase;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DCoreUseEvent implements Listener {

    @EventHandler
    public void onKeyPress(KeyPressEvent event) {

        Bukkit.getScheduler().runTaskAsynchronously(AttributePotion.getInstance(), () -> {

            if(!ConfigManager.dragoncore) return;
            long startTime = System.currentTimeMillis();
            String pressKey = event.getKey();

            if(ConfigManager.coreKeys.containsKey(pressKey)) {

                Player player = event.getPlayer();
                String slotName = ConfigManager.coreKeys.get(pressKey);

                if (!Config.getSlotConfig().contains(slotName)) return;

                SlotAPI.getSlotItem(player, slotName, new IDataBase.Callback<ItemStack>() {

                    @Override
                    public void onResult(ItemStack item) {

                        if (UseEvent.isAir(item)) return;

                        String key = UseEvent.matchItem(item);
                        if ((key == null)) return;

                        Potion potion = ConfigManager.potions.get(key);

                        String name = potion.getName();
                        String group = potion.getGroup();
                        UUID uuid = player.getUniqueId();
                        long useTime = System.currentTimeMillis();

                        //药水组冷却判断
                        if (UseEvent.isGroupOnCooldown(player, uuid, group, useTime)) return;

                        //药水条件判断
                        if (UseEvent.isPotionOnCooldown(player, uuid, key, potion, useTime)) return;

                        //条件判断
                        if (UseEvent.meetConditions(potion.getConditions(), player, name)) return;

                        //判断是否消耗
                        if (potion.isConsume() && item.getAmount() >= 1) {
                            item.setAmount(item.getAmount() - 1);
                            SlotAPI.setSlotItem(player, slotName, item, true);
                        }

                        //处理药水效果
                        UseEvent.potionEffectsProcess(player, potion);

                        // 处理属性
                        List<String> attrList = PAPIHook.papiProcess(player, potion.getAttributes());
                        UseEvent.attributeProcess(player, potion.getTime(), key, attrList, useTime, group, potion);

                        //effects效果处理
                        UseEvent.effectsProcess(player, potion);

                        //添加冷却
                        ConfigManager.playerUseTime.get(uuid).put(key, useTime);
                        ConfigManager.playerUseTime.get(uuid).put(group, useTime);

                        //设置物品冷却
                        Map<String, Boolean> options = potion.getOptions();
                        if (!UseEvent.isAir(item) && !options.isEmpty() && options.containsKey("cool") && options.get("cool")) {
                            player.setCooldown(item.getType(), potion.getCooldown() * 20);
                        }

                        UseEvent.optionsProcess(player, potion, item, potion.getTime(), key, attrList, useTime, group);

                        //处理指令
                        UseEvent.commandsProcess(potion, player);

                        if (ConfigManager.debug) {
                            long endTime = System.currentTimeMillis();
                            long elapsedTime = endTime - startTime;
                            LogInfo.debug("§e DragonCore的药水使用事件消耗的时间：" + elapsedTime + "ms");

                        }

                    }

                    @Override
                    public void onFail() {
                        player.sendMessage("§c槽位物品获取为空！");
                    }

                });
            }
        });
    }
}
