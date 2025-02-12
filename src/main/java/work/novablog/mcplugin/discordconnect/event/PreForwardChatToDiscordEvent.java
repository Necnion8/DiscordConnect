package work.novablog.mcplugin.discordconnect.event;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Discordにチャットを転送する前に実行されるイベント
 * cancelするとDiscordConnectによる転送をキャンセルできる
 */
public class PreForwardChatToDiscordEvent extends Event implements Cancellable {
    private final ChatEvent event;
    private boolean cancelled;

    public PreForwardChatToDiscordEvent(@NotNull ChatEvent event) {
        this.event = event;
        cancelled = false;
    }

    /**
     * チャットを送信したプレイヤーを取得する
     *
     * @return 送信者
     */
    public ProxiedPlayer getSender() {
        return (ProxiedPlayer) event.getSender();
    }

    /**
     * チャットのメッセージを取得する
     *
     * @return メッセージ
     */
    public String getMessage() {
        return event.getMessage();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
