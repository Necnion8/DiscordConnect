package work.novablog.mcplugin.discordconnect.command.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.util.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * 複数のサブコマンドを持つコマンド
 * 引数が指定されていない場合はdefaultCommandを実行する
 */
public class CompositeCommand extends Command implements TabExecutor {
    private final HashMap<String, Command> subCommands;
    private Command defaultCommand;

    /**
     * サブコマンド無しの状態でコマンドを作成する
     * コマンドは{@code name}または{@code aliases}で参照される
     *
     * @param name       コマンド名
     * @param permission 実行に必要な権限
     * @param aliases    エイリアス
     */
    public CompositeCommand(@NotNull String name, @Nullable String permission, @NotNull String... aliases) {
        super(name, permission, aliases);
        subCommands = new HashMap<>();
    }

    /**
     * サブコマンドを追加する
     * 実行権限は{@link Command#hasPermission(CommandSender)}で判定される
     *
     * @param subCommands 追加するサブコマンド
     */
    public void addSubCommands(@NotNull Command... subCommands) {
        for (Command subCommand : subCommands) {
            this.subCommands.put(subCommand.getName(), subCommand);
            for (String alias : subCommand.getAliases()) {
                this.subCommands.put(alias, subCommand);
            }
        }
    }

    /**
     * デフォルトのコマンドを設定する
     * 引数を指定しなかった場合に実行される
     * ただしデフォルトコマンドがnullならばnot foundメッセージが送信される
     *
     * @param defaultCommand デフォルトのコマンド
     */
    public void setDefaultCommand(@Nullable Command defaultCommand) {
        this.defaultCommand = defaultCommand;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Command command = args.length == 0 ? defaultCommand : subCommands.get(args[0]);
        if (command == null) {
            sender.sendMessage(TextComponent.fromLegacyText(Message.bungeeCommandNotFound.toString()));
            return;
        }

        if (!command.hasPermission(sender)) {
            sender.sendMessage(TextComponent.fromLegacyText(Message.bungeeCommandDenied.toString()));
            return;
        }

        command.execute(sender, args.length == 0 ? args : Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        Command subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null) {
            if (subCommand instanceof TabExecutor) {
                return ((TabExecutor) subCommand).onTabComplete(
                        sender, Arrays.copyOfRange(args, 1, args.length)
                );
            } else {
                return Collections.emptyList();
            }
        }

        ArrayList<String> suggestions = new ArrayList<>();
        for (String alias : subCommands.keySet()) {
            if (alias.startsWith(args[0].toLowerCase())) {
                suggestions.add(alias);
            }
        }
        return suggestions;
    }
}
