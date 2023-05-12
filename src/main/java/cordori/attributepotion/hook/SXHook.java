package cordori.attributepotion.hook;

import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.listener.UseEvent;
import github.saukiya.sxattribute.data.attribute.SXAttributeData;
import github.saukiya.sxattribute.data.condition.SXConditionType;
import lombok.SneakyThrows;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SXHook {
    public static Method attributeUpdate;
    public static Method setEntityAPIData;
    public static Method method;
    public static Object api;

    public static void setSXMethod() throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        Class<?> clazz = Class.forName("github.saukiya.sxattribute.SXAttribute");
        Method getApi = clazz.getMethod("getApi");
        api = getApi.invoke(clazz);
        attributeUpdate = AttributePotion.SX3 ? api.getClass().getMethod("attributeUpdate", LivingEntity.class) : api.getClass().getMethod("updateStats", LivingEntity.class);
        setEntityAPIData = api.getClass().getMethod("setEntityAPIData", Class.class, UUID.class, SXAttributeData.class);
        method = AttributePotion.SX3 ? api.getClass().getMethod("loadListData", List.class) : api.getClass().getMethod("getLoreData", LivingEntity.class, SXConditionType.class, List.class);
    }

    public static void addSXAttribute(Player player) {
        UUID uuid = player.getUniqueId();
        List<String> SXList = new ArrayList<>();
        UseEvent.attributeMap.values().forEach(attrMap -> attrMap.values().forEach(SXList::addAll));
        updateAttribute(SXList, player, uuid);
    }

    public static void takeSXAttribute(Player player) {
        UUID uuid = player.getUniqueId();
        List<String> SXList = new ArrayList<>();
        UseEvent.attributeMap.values().forEach(attrMap -> attrMap.values().forEach(SXList::addAll));
        updateAttribute(SXList, player, uuid);
    }

    @SneakyThrows
    private static void updateAttribute(List<String> SXList, Player player, UUID uuid) {
        SXAttributeData data;
        data = getSXAttributeData(SXList);

        setEntityAPIData.invoke(api, AttributePotion.class, uuid, data);
        attributeUpdate.invoke(api, player);
    }

    @SneakyThrows
    private static SXAttributeData getSXAttributeData(List<String> SXList) {
        return AttributePotion.SX3 ? (SXAttributeData) method.invoke(api, SXList) : (SXAttributeData) method.invoke(api, null, null, SXList);
    }

}
