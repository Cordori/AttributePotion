package cordori.attributepotion.hook;

import com.sucy.skill.SkillAPI;
import com.sucy.skill.api.player.PlayerData;
import org.bukkit.entity.Player;

public class SkillAPIHook {

    public static void giveMana(Player player, int value, String manaMode) {
        PlayerData localPlayerData = SkillAPI.getPlayerData(player);
        double mana = localPlayerData.getMana();
        double maxMana = localPlayerData.getMaxMana();

        switch (manaMode) {
            case "0":
                localPlayerData.giveMana(value);
                break;
            case "1":
                localPlayerData.giveMana(mana * value / 100);
                break;
            case "2":
                localPlayerData.giveMana(maxMana * value / 100);
                break;
        }
    }
}
