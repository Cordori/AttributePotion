package cordori.attributepotion.listener;

import com.germ.germplugin.api.GermSlotAPI;
import com.germ.germplugin.api.event.GermKeyDownEvent;
import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.file.ConfigManager;
import cordori.attributepotion.hook.PAPIHook;
import cordori.attributepotion.utils.LogInfo;
import cordori.attributepotion.utils.Potion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GermUseEvent implements Listener {

    @EventHandler
    public void onKeyPress(GermKeyDownEvent event) {

        Bukkit.getScheduler().runTaskAsynchronously(AttributePotion.getInstance(), () -> {

            long startTime = System.currentTimeMillis();

            if(!ConfigManager.germplugin) return;
            String pressKey = String.valueOf(event.getKeyID());

            if(!ConfigManager.coreKeys.containsKey(pressKey)) return;

            Player player = event.getPlayer();
            String slotName = ConfigManager.coreKeys.get(pressKey);

            ItemStack item = GermSlotAPI.getItemStackFromIdentity(player, slotName);
            if(UseEvent.isAir(item)) return;

            String key = UseEvent.matchItem(item);
            if (key == null) return;

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
                GermSlotAPI.saveItemStackToIdentity(player, slotName, item);
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

            // 选项处理
            UseEvent.optionsProcess(player, potion, item, potion.getTime(), key, attrList, useTime, group);

            //处理指令
            UseEvent.commandsProcess(potion, player);

            if (ConfigManager.debug) {
                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;
                LogInfo.debug("§e GermPlugin的药水使用事件消耗的时间：" + elapsedTime + "ms");

            }

        });
    }
}
