package cordori.attributepotion.command;

import cordori.attributepotion.AttributePotion;
import cordori.attributepotion.file.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;



public class MainCommand implements CommandExecutor, TabCompleter {
    private static final AttributePotion ap = AttributePotion.getInstance();
    private List<String> filter(List<String> list, String latest) {
        if (list.isEmpty() || latest == null)
            return list;
        String ll = latest.toLowerCase();
        list.removeIf(k -> !k.toLowerCase().startsWith(ll));
        return list;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String latest = null;
        List<String> list = new ArrayList<>();
        if (args.length != 0) {
            latest = args[args.length - 1];
        }
        if (args.length == 1) {
            if(sender instanceof Player) {
                Player player = ((Player) sender).getPlayer();
                if(player.isOp()) {
                    list.add("reload");
                }
            } else {
                list.add("reload");
            }
        }
        return filter(list, latest);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0) {
            return false;
        }
        if(args.length == 1) {
            if(args[0].equalsIgnoreCase("reload")) {
                ConfigManager.reloadMyConfig();
                sender.sendMessage(ConfigManager.prefix + ap.getConfig().getString("messages.reload").replaceAll("&","§"));
                return true;
            }
            return false;
        }
        return false;
    }
}
