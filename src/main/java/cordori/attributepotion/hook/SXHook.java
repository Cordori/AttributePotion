package cordori.attributepotion.hook;

import cordori.attributepotion.AttributePotion;
import github.saukiya.sxattribute.data.attribute.SXAttributeData;
import github.saukiya.sxattribute.data.condition.SXConditionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
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
        SXAttributeData data = null;

        try {
            data = AttributePotion.SX3 ? getSXAttributeData(SXList) : getSXAttributeData(player, SXList);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        try {
            Object api = getSXAPI();
            Method setEntityAPIData = api.getClass().getMethod("setEntityAPIData", Class.class, UUID.class, SXAttributeData.class);
            setEntityAPIData.invoke(api, AttributePotion.class, uuid, data);
            Method attributeUpdate;
            if(AttributePotion.SX3) {
                attributeUpdate = api.getClass().getMethod("attributeUpdate", LivingEntity.class);
            } else {
                attributeUpdate = api.getClass().getMethod("updateStats", LivingEntity.class);
            }
            attributeUpdate.invoke(api, player);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void takeSXAttribute(Player player, String key) {
        UUID uuid = player.getUniqueId();
        SXData.get(uuid).remove(key);
        List<String> SXList = new ArrayList<>();
        for(List<String> list : SXData.get(uuid).values()) {
            SXList.addAll(list);
        }

        SXAttributeData data = null;

        try {
            data = AttributePotion.SX3 ? getSXAttributeData(SXList) : getSXAttributeData(player, SXList);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        try {
            Object api = getSXAPI();
            Method setEntityAPIData = api.getClass().getMethod("setEntityAPIData", Class.class, UUID.class, SXAttributeData.class);
            setEntityAPIData.invoke(api, AttributePotion.class, uuid, data);
            Method attributeUpdate;
            if(AttributePotion.SX3) {
                attributeUpdate = api.getClass().getMethod("attributeUpdate", LivingEntity.class);
            } else {
                attributeUpdate = api.getClass().getMethod("updateStats", LivingEntity.class);
            }
            attributeUpdate.invoke(api, player);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static SXAttributeData getSXAttributeData(Player player, List<String> SXList) throws Exception {
        /*
         * 使用反射获取方法 - SX 2
         */
        Object api = getSXAPI();
        Method method = api.getClass().getMethod("getLoreData", LivingEntity.class, SXConditionType.class, List.class);
        return (SXAttributeData) method.invoke(api, player, null, SXList);
    }

    private static SXAttributeData getSXAttributeData(List<String> SXList) throws Exception {
        /*
         * 使用反射获取方法 - SX 3
         */
        Object api = getSXAPI();
        Method method = api.getClass().getMethod("loadListData", List.class);
        return (SXAttributeData) method.invoke(api, SXList);
    }

    private static Object getSXAPI() throws Exception {
        Class<?> clazz = Class.forName("github.saukiya.sxattribute.SXAttribute");
        Method method = clazz.getMethod("getApi");
        return method.invoke(clazz);
    }
}
