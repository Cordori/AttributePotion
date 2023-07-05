package cordori.attributepotion.utils;

import cordori.attributepotion.AttributePotion;

public class LogInfo {

    public static void debug(String str) {
        AttributePotion.getInstance().getLogger().info(str);
    }
}
