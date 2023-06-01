package plugin.treasurehunt.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * コマンドを実行して動かすプラグイン処理の基底クラス
 */

public abstract class BaseCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            return onExecutePlayerCommand(player, command, label, args);
        }else{
            return onExecutePlayerNPCCommand(sender, command, label, args);
        }
    }

    /**
     * コマンド実行者がプレーヤーだった場合に実行
     * @param player　プレーヤー
     */
    public abstract boolean onExecutePlayerCommand(Player player, @NotNull Command command, @NotNull String label, String[] args) ;

    /**
     * コマンド実行者がプレーヤーだった場合に実行
     * @param sender　プレーヤー以外
     */
    protected abstract boolean onExecutePlayerNPCCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args);



}
