package cordori.attributepotion.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter @RequiredArgsConstructor
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
    private final List<String> endCommands;
    private final Map<String, Boolean> options;
    private final int rangeValue;
}

