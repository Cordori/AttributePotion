package cordori.attributepotion.hook;

import cordori.attributepotion.AttributePotion;
import org.bukkit.entity.Player;
import org.serverct.ersha.api.AttributeAPI;
import org.serverct.ersha.attribute.data.AttributeData;
import org.serverct.ersha.jd.api.EntityAttributeAPI;

import java.util.List;

public class APHook {

    public static void addAttribute(Player player, List<String> attrList, String key) {
        if(AttributePotion.AP3) {
            AttributeData APdata = AttributeAPI.getAttrData(player);
            AttributeAPI.addSourceAttribute(APdata, "AttributePotion_" + key, attrList);
        } else {
            EntityAttributeAPI.addEntityAttribute(player, "AttributePotion_" + key, attrList);
        }

    }

    public static void takeAttribute(Player player, String key) {
        if(AttributePotion.AP3) {
            AttributeData APdata = AttributeAPI.getAttrData(player);
            AttributeAPI.takeSourceAttribute(APdata, "AttributePotion_" + key);
            AttributeAPI.updateAttribute(player);
        } else {
            EntityAttributeAPI.removeEntityAttribute(player, "AttributePotion_" + key);
        }
    }

}
