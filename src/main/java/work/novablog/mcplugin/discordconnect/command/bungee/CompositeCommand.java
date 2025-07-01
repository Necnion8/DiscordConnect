package work.novablog.mcplugin.discordconnect.command.bungee;

import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.util.Message;
import work.novablog.mcplugin.discordconnect.util.TextUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 複数のサブコマンドを持つコマンド
 * 引数が指定されていない場合はdefaultCommandを実行する
 */
public class CompositeCommand extends Command implements SimpleCommand {
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
     * 実行権限は{@link SimpleCommand#hasPermission(CommandInvocation)}で判定される
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
    public void execute(Invocation invocation) {
        execute(invocation.source(), invocation.arguments());
    }

    public void execute(CommandSource sender, String[] args) {
        Command command = args.length == 0 ? defaultCommand : subCommands.get(args[0]);
        if (command == null) {
            sender.sendMessage(TextUtil.LEGACY_SERIALIZER.deserialize(Message.bungeeCommandNotFound.toString()));
            return;
        }

        if (!command.hasPermission(sender)) {
            sender.sendMessage(TextUtil.LEGACY_SERIALIZER.deserialize(Message.bungeeCommandDenied.toString()));
            return;
        }

        command.execute(sender, args.length == 0 ? args : Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return onTabComplete(invocation.source(), invocation.arguments());
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args) {
        String arg = args.length == 0 ? "" : args[0].toLowerCase();
        Command subCommand = subCommands.get(arg);
        if (subCommand != null) {
            return subCommand.onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        ArrayList<String> suggestions = new ArrayList<>();
        for (String alias : subCommands.keySet()) {
            if (alias.startsWith(arg)) {
                suggestions.add(alias);
            }
        }
        return suggestions;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return hasPermission(invocation.source());
    }

    @Override
    public String[] getAliases() {
        return super.getAliases();
    }
}
