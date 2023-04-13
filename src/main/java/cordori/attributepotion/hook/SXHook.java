package cordori.attributepotion.hook;

import cordori.attributepotion.AttributePotion;
import github.saukiya.sxattribute.SXAttribute;
import github.saukiya.sxattribute.data.attribute.SXAttributeData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SXHook {
    public static HashMap<UUID, HashMap<String, List<String>>> SXData = new HashMap<>();

    public static void addSXAttribute(Player player, String key, List<String> attrList) {
        UUID uuid = player.getUniqueId();
        if(!SXData.containsKey(uuid)) {
            HashMap<String, List<String>> map = new HashMap<>();
            SXData.put(uuid, map);
        }
        SXData.get(uuid).put(key, attrList);
        List<String> SXList = new ArrayList<>();
        for(List<String> list : SXData.get(uuid).values()) {
            SXList.addAll(list);
        }
        SXAttributeData SXdata = SXAttribute.getApi().loadListData(SXList);
        SXAttribute.getApi().setEntityAPIData(AttributePotion.class, uuid, SXdata);
        SXAttribute.getApi().attributeUpdate(player);
    }

    public static void takeSXAttribute(Player player, String key) {
        UUID uuid = player.getUniqueId();
        SXData.get(uuid).remove(key);
        List<String> SXList = new ArrayList<>();
        for(List<String> list : SXData.get(uuid).values()) {
            SXList.addAll(list);
        }
        SXAttributeData SXdata = SXAttribute.getApi().loadListData(SXList);
        SXAttribute.getApi().setEntityAPIData(AttributePotion.class, uuid, SXdata);
        SXAttribute.getApi().attributeUpdate(player);
    }
}
