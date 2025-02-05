package work.novablog.mcplugin.discordconnect.command.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
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

    public CompositeCommand(String name, String permission, String... aliases) {
        super(name, permission, aliases);
        subCommands = new HashMap<>();
    }

    public void addSubCommands(Command... subCommands) {
        for (Command subCommand : subCommands) {
            addSubCommand(subCommand);
        }
    }

    public void addSubCommand(Command subCommand) {
        subCommands.put(subCommand.getName(), subCommand);
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias, subCommand);
        }
    }

    public void setDefaultCommand(Command defaultCommand) {
        this.defaultCommand = defaultCommand;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Command command = args.length == 0 ? defaultCommand : subCommands.get(args[0]);
        if (command == null) {
            sender.sendMessage(new TextComponent(Message.bungeeCommandNotFound.toString()));
            return;
        }

        if (!command.hasPermission(sender)) {
            sender.sendMessage(new TextComponent(Message.bungeeCommandDenied.toString()));
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
