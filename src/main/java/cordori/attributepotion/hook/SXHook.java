package cordori.attributepotion.hook;

import cordori.attributepotion.AttributePotion;
import github.saukiya.sxattribute.data.attribute.SXAttributeData;
import github.saukiya.sxattribute.data.condition.SXConditionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SXHook {
    public static Method attributeUpdate;
    public static Method setEntityAPIData;
    public static Method method;
    public static Object api;
    private static final HashMap<UUID, HashMap<String, List<String>>> SXData = new HashMap<>();

    public static void setSXMethod() throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        Class<?> clazz = Class.forName("github.saukiya.sxattribute.SXAttribute");
        Method getApi = clazz.getMethod("getApi");
        api = getApi.invoke(clazz);
        attributeUpdate = AttributePotion.SX3 ? api.getClass().getMethod("attributeUpdate", LivingEntity.class) : api.getClass().getMethod("updateStats", LivingEntity.class);
        setEntityAPIData = api.getClass().getMethod("setEntityAPIData", Class.class, UUID.class, SXAttributeData.class);
        method = AttributePotion.SX3 ? api.getClass().getMethod("loadListData", List.class) : api.getClass().getMethod("getLoreData", LivingEntity.class, SXConditionType.class, List.class);
    }

    public static void addSXAttribute(Player player, String key, List<String> attrList) {
        UUID uuid = player.getUniqueId();
        SXData.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, attrList);
        List<String> SXList = new ArrayList<>();
        SXData.values().forEach(attrMap -> attrMap.values().forEach(SXList::addAll));
        updateAttribute(SXList, player, uuid);
    }

    public static void takeSXAttribute(Player player, String key) {
        UUID uuid = player.getUniqueId();
        SXData.computeIfPresent(uuid, (k, attrMap) -> {
            attrMap.remove(key);
            return attrMap;
        });
        List<String> SXList = new ArrayList<>();
        SXData.values().forEach(attrMap -> attrMap.values().forEach(SXList::addAll));
        updateAttribute(SXList, player, uuid);
    }

    private static void updateAttribute(List<String> SXList, Player player, UUID uuid) {
        SXAttributeData data;
        try {
            data = getSXAttributeData(SXList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            setEntityAPIData.invoke(api, AttributePotion.class, uuid, data);
            attributeUpdate.invoke(api, player);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static SXAttributeData getSXAttributeData(List<String> SXList) throws Exception {
        return AttributePotion.SX3 ? (SXAttributeData) method.invoke(api, SXList) : (SXAttributeData) method.invoke(api, null, null, SXList);
    }

}
