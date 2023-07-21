package fun.milkyway.toomanygen;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class TooManyGenCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("toomanygen.command.reload")) {
            return false;
        }
        if (args.length == 0) {
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                TooManyGen.getInstance().onReload();
                sender.sendMessage(LangManager.getInstance().getComponent("command.reload.success"));
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
