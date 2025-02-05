package work.novablog.mcplugin.discordconnect.command.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.Message;

public class BungeeCommand extends CompositeCommand {
    private static final String NAME = DiscordConnect.getInstance().getDescription().getName();
    private static final String PERM = "discordconnect.command";
    private static final String ALIAS = "discon";

    private static final String RELOAD_PERM = PERM + ".reload";

    public BungeeCommand() {
        super(NAME, PERM, ALIAS);

        Command helpCommand = new Command("help") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                sender.sendMessage(new TextComponent(Message.bungeeCommandHelpLine1.toString()));
                sender.sendMessage(new TextComponent(Message.bungeeCommandHelpHelpcmd.toString()));
                sender.sendMessage(new TextComponent(Message.bungeeCommandHelpReloadcmd.toString()));
            }
        };

        Command reloadCommand = new Command("reload", RELOAD_PERM) {
            @Override
            public void execute(CommandSender sender, String[] args) {
                DiscordConnect.getInstance().loadConfig();
                sender.sendMessage(new TextComponent(Message.configReloaded.toString()));
            }
        };

        addSubCommands(helpCommand, reloadCommand);
        setDefaultCommand(helpCommand);
    }
}
