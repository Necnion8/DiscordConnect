package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.event.PreForwardChatToDiscordEvent;
import work.novablog.mcplugin.discordconnect.util.BotManager;
import work.novablog.mcplugin.discordconnect.util.Message;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VelocityListener {
    private static final String AVATAR_IMG_URL = "https://mc-heads.net/avatar/{uuid}";
    private final @NotNull ProxyServer server;
    private final BotManager botManager;
    private final String toDiscordFormat;
    private final List<String> hiddenServers;

    public VelocityListener(
            @NotNull ProxyServer server,
            @NotNull BotManager botManager,
            @NotNull String toDiscordFormat,
            @NotNull List<String> hiddenServers
    ) {
        this.server = server;
        this.botManager = botManager;
        this.toDiscordFormat = toDiscordFormat;
        this.hiddenServers = hiddenServers;
    }

    @Subscribe  // HIGHEST?
    public void onChat(PlayerChatEvent event) {
        if (!event.getResult().isAllowed()) return;

        String serverName = event.getPlayer().getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse("unknown");
        if (hiddenServers.contains(serverName)) return;

        PreForwardChatToDiscordEvent preEvent = new PreForwardChatToDiscordEvent(event);
        server.getEventManager().fireAndForget(preEvent);
    }

    @Subscribe  // HIGHEST?
    public void onPreForwardChatToDiscord(PreForwardChatToDiscordEvent event) {
        if (event.isCancelled()) return;

        Player sender = event.getSender();
        ServerConnection server = sender.getCurrentServer().orElse(null);
        if (server == null)
            return;
        String message = event.getMessage();

        MarkComponent[] components = MarkdownConverter.fromMinecraftMessage(message, '&');
        String convertedMessage = MarkdownConverter.toDiscordMessage(components);
        botManager.sendMessageToChatChannel(
                toDiscordFormat.replace("{server}", server.getServerInfo().getName())
                        .replace("{sender}", sender.getUsername())
                        .replace("{message}", convertedMessage)
        );
    }

    @Subscribe
    public void onLogin(PostLoginEvent e) {
        botManager.sendMessageToChatChannel(
                Message.userActivity.toString(),
                null,
                Message.joined.toString().replace("{name}", e.getPlayer().getUsername()),
                Color.GREEN,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                AVATAR_IMG_URL.replace(
                        "{uuid}",
                        e.getPlayer().getUniqueId().toString().replace("-", "")
                )
        );

        updatePlayerCount();
    }

    @Subscribe
    public void onLogout(DisconnectEvent e) {
        botManager.sendMessageToChatChannel(
                Message.userActivity.toString(),
                null,
                Message.left.toString().replace("{name}", e.getPlayer().getUsername()),
                Color.RED,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                AVATAR_IMG_URL.replace(
                        "{uuid}",
                        e.getPlayer().getUniqueId().toString().replace("-", "")
                )
        );

        updatePlayerCount();
    }

    @Subscribe
    public void onSwitch(ServerConnectedEvent e) {
        if (hiddenServers.contains(e.getServer().getServerInfo().getName())) return;

        botManager.sendMessageToChatChannel(
                Message.userActivity.toString(),
                null,
                Message.serverSwitched.toString()
                        .replace("{name}", e.getPlayer().getUsername())
                        .replace("{server}", e.getServer().getServerInfo().getName()),
                Color.CYAN,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                AVATAR_IMG_URL.replace(
                        "{uuid}",
                        e.getPlayer().getUniqueId().toString().replace("-", "")
                )
        );
    }

    /**
     * プレイヤー数情報を更新
     */
    private void updatePlayerCount() {
        botManager.updateGameName(
                server.getPlayerCount(),
                server.getConfiguration().getShowMaxPlayers()
        );
    }
}
