package cordori.attributepotion.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Potion {
    private final String key;
    private final String name;
    private final String lore;
    private final int time;
    private final int cooldown;
    private final String group;
    private final List<String> conditions;
    private final boolean shift;
    private final Map<String, String> effects;
    private final Map<String, String> potionEffects;
    private final List<String> attributes;
    private final boolean consume;
    private final List<String> commands;
    private final Map<String, Boolean> options;

    public Potion(String key, String name, String lore, int time, int cooldown, String group,
                  List<String> conditions, boolean shift, Map<String, String> effects, Map<String, String> potionEffects, List<String> attributes, boolean consume,
                  List<String> commands, Map<String, Boolean> options) {
        this.key = key;
        this.name = name;
        this.lore = lore;
        this.time = time;
        this.cooldown = cooldown;
        this.group = group;
        this.conditions = conditions != null ? conditions : Collections.emptyList();
        this.shift = shift;
        this.effects = effects != null ? effects : Collections.emptyMap();
        this.potionEffects = potionEffects != null ? potionEffects : Collections.emptyMap();
        this.attributes = attributes != null ? attributes : Collections.emptyList();
        this.consume = consume;
        this.commands = commands != null ? commands : Collections.emptyList();
        this.options = options != null ? options : Collections.emptyMap();
    }


    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getLore() {
        return lore;
    }

    public int getTime() {
        return time;
    }

    public int getCooldown() {
        return cooldown;
    }

    public String getGroup() {
        return group;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public Map<String, String> getEffects() {
        return effects;
    }
    public Map<String, String> getPotionEffects() {
        return potionEffects;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public boolean isConsume() {
        return consume;
    }

    public List<String> getCommands() {
        return commands;
    }

    public boolean isShift() {
        return shift;
    }

    public Map<String, Boolean> getOptions() {
        return options;
    }
}

