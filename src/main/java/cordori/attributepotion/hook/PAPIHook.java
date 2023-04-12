package cordori.attributepotion.hook;

import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.file.ConfigManager;
import cordori.attributepotion.utils.Potion;
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
    private HashMap<UUID, HashMap<String, Long>> cooldown = ConfigManager.cooldown;
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

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null || identifier == null || identifier.isEmpty()) {
            return "";
        }
        long useTime = System.currentTimeMillis();
        long lastPotionTime = 0;
        UUID uuid = player.getUniqueId();
        for (String key : potionKeys) {
            if (identifier.equalsIgnoreCase(key + "_cooldown")) {
                int time = potions.get(key).getTime();
                if (!cooldown.isEmpty() && cooldown.containsKey(uuid) && cooldown.get(uuid).containsKey(key)) {
                    lastPotionTime = cooldown.get(uuid).get(key);
                }
                if ((useTime - lastPotionTime) / 1000 <= time) {
                    return String.valueOf(time - (useTime - lastPotionTime) / 1000);
                }
            }
        }
        return "0";
    }
}