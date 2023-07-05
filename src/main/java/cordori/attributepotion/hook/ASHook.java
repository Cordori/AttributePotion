package cordori.attributepotion.hook;

import com.skillw.attsystem.api.AttrAPI;
import org.bukkit.entity.Player;

import java.util.List;

public class ASHook {

    public static void addAttribute(Player player, String key, List<String> attrList) {
        AttrAPI.addAttribute(player, key, attrList, false);
        AttrAPI.updateAttr(player);
    }

    public static void takeAttribute(Player player, String key) {
        AttrAPI.removeAttribute(player, key);
        AttrAPI.updateAttr(player);
    }

}
