package work.novablog.mcplugin.discordconnect.listener;

import com.github.ucchyocean.lc.velocity.event.LunaChatVelocityChannelMessageEvent;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.velocitypowered.api.event.Subscribe;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.event.PreForwardChatToDiscordEvent;
import work.novablog.mcplugin.discordconnect.util.BotManager;

public class LunaChatListener {
    private final BotManager botManager;
    private final String toDiscordFormat;

    public LunaChatListener(@NotNull BotManager botManager, @NotNull String toDiscordFormat) {
        this.botManager = botManager;
        this.toDiscordFormat = toDiscordFormat;
    }

    @Subscribe
    public void onPreForwardChatToDiscord(PreForwardChatToDiscordEvent event) {
        // BungeeListener#onPreForwardChatToDiscordによって二重に送信されるのを抑制
        event.setCancelled(true);
    }

    @Subscribe  // HIGHEST?
    public void onLunaChatMessage(LunaChatVelocityChannelMessageEvent event) {
        if (!event.getChannel().isGlobalChannel()) return;

        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(event.getMessage(), '§');
        String convertedMessage = MarkdownConverter.toDiscordMessage(components);

        botManager.sendMessageToChatChannel(
                toDiscordFormat.replace("{server}", event.getMember().getServerName())
                        .replace("{sender}", event.getMember().getDisplayName())
                        .replace("{message}", convertedMessage)
        );
    }
}
