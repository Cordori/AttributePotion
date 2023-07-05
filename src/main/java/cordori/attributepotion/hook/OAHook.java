package cordori.attributepotion.hook;

import ac.github.oa.api.OriginAttributeAPI;
import ac.github.oa.internal.core.attribute.AttributeData;
import org.bukkit.entity.Player;

import java.util.List;

public class OAHook {

    public static OriginAttributeAPI OAAPI;

    public static void addAttribute(Player player, String key, List<String> attrList) {
        AttributeData data = OAAPI.loadList(attrList);
        OAAPI.setExtra(player.getUniqueId(), key, data);
        OAAPI.callUpdate(player);
    }

    public static void takeAttribute(Player player, String key) {
        OAAPI.removeExtra(player.getUniqueId(), key);
        OAAPI.callUpdate(player);
    }

}
