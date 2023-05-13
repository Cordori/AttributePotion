package cordori.attributepotion.hook;

import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.file.ConfigManager;
import cordori.attributepotion.utils.Potion;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PAPIHook extends PlaceholderExpansion {
    private final AttributePotion ap;
    private List<String> potionKeys = ConfigManager.potionKeys;
    private Map<String, Potion> potions = ConfigManager.potions;
    private HashMap<UUID, HashMap<String, Long>> cooldown = ConfigManager.playerUseTime;
    public PAPIHook(AttributePotion ap) {
        this.ap = ap;
    }
    //持久化
    @Override
    public boolean persist() {
        return true;
    }
    @Override
    public boolean canRegister() {
        return true;
    }
    @Override
    public String getAuthor() {
        return "Cordori";
    }
    @Override
    public String getVersion() {
        return ap.getDescription().getVersion();
    }
    @Override
    public String getIdentifier() {
        return "AttributePotion";
    }

    public static List<String> papiProcess(Player player, List<String> stringList) {
        return PlaceholderAPI.setPlaceholders(player, stringList);
    }
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null || identifier == null || identifier.isEmpty()) return null;

        long useTime = System.currentTimeMillis();
        long lastPotionTime = 0;
        UUID uuid = player.getUniqueId();
        for (String key : potionKeys) {

            if (identifier.equalsIgnoreCase("count_" + key)) {
                int time = potions.get(key).getTime();
                if (!cooldown.isEmpty() && cooldown.containsKey(uuid) && cooldown.get(uuid).containsKey(key)) {
                    lastPotionTime = cooldown.get(uuid).get(key);
                }
                if ((useTime - lastPotionTime) / 1000 <= time) {
                    return String.valueOf(time - (useTime - lastPotionTime) / 1000);
                }
            }

            if (identifier.equalsIgnoreCase("cooldown_" + key)) {
                int cooldown = potions.get(key).getCooldown();
                return String.valueOf(cooldown);
            }

            if (identifier.equalsIgnoreCase("time_" + key)) {
                int time = potions.get(key).getTime();
                return String.valueOf(time);
            }

            if (identifier.equalsIgnoreCase("group_" + key)) {
                return potions.get(key).getGroup();
            }

        }
        return "0";
    }
}
