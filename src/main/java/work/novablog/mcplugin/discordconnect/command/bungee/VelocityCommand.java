package work.novablog.mcplugin.discordconnect.command.bungee;

import com.velocitypowered.api.command.CommandSource;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.util.Message;
import work.novablog.mcplugin.discordconnect.util.TextUtil;

public class VelocityCommand extends CompositeCommand {
    private static final String NAME = DiscordConnect.class.getSimpleName();
    private static final String PERM = "discordconnect.command";
    private static final String ALIAS = "discon";

    private static final String RELOAD_PERM = PERM + ".reload";

    public VelocityCommand() {
        super(NAME, PERM, ALIAS);

        Command helpCommand = new Command("help") {
            @Override
            public void execute(CommandSource sender, String[] args) {
                sender.sendMessage(TextUtil.LEGACY_SERIALIZER.deserialize(Message.bungeeCommandHelpLine1.toString()));
                sender.sendMessage(TextUtil.LEGACY_SERIALIZER.deserialize(Message.bungeeCommandHelpHelpcmd.toString()));
                sender.sendMessage(TextUtil.LEGACY_SERIALIZER.deserialize(Message.bungeeCommandHelpReloadcmd.toString()));
            }
        };

        Command reloadCommand = new Command("reload", RELOAD_PERM) {
            @Override
            public void execute(CommandSource sender, String[] args) {
                DiscordConnect.getInstance().reload();
                sender.sendMessage(TextUtil.LEGACY_SERIALIZER.deserialize(Message.configReloaded.toString()));
            }
        };

        addSubCommands(helpCommand, reloadCommand);
        setDefaultCommand(helpCommand);
    }
}
