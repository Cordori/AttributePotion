package cordori.attributepotion.hook;

import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.file.ConfigManager;
import org.bukkit.entity.Player;
import org.serverct.ersha.api.AttributeAPI;
import org.serverct.ersha.attribute.data.AttributeData;
import org.serverct.ersha.jd.api.EntityAttributeAPI;

import java.util.List;
import java.util.Map;

public class APHook {

    public static void addAPAttribute(Player player, List<String> attrList, String key) {
        if(AttributePotion.AP3) {
            AttributeData APdata = AttributeAPI.getAttrData(player);
            AttributeAPI.addSourceAttribute(APdata, "AttributePotion_" + key, attrList);
        } else {
            EntityAttributeAPI.addEntityAttribute(player, "AttributePotion_" + key, attrList);
        }

    }

    public static void takeAPAttribute(Player player, String key) {
        if(AttributePotion.AP3) {
            AttributeData APdata = AttributeAPI.getAttrData(player);
            AttributeAPI.takeSourceAttribute(APdata, "AttributePotion_" + key);
        } else {
            EntityAttributeAPI.removeEntityAttribute(player, "AttributePotion_" + key);
        }
    }

    public static void delAPAttribute(Player player) {
        AttributeData data = AttributeAPI.getAttrData(player);
        for(String potionKey : ConfigManager.potionKeys) {
            Map<String, Boolean> options = ConfigManager.potions.get(potionKey).getOptions();
            if(!options.isEmpty() && options.containsKey("death")) {
                if(options.get("death")) {
                    AttributeAPI.takeSourceAttribute(data, "AttributePotion_" + potionKey);
                }
            }
        }
    }
}
