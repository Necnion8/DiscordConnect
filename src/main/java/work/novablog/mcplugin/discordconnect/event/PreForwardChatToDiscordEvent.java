package work.novablog.mcplugin.discordconnect.event;

import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Discordにチャットを転送する前に実行されるイベント
 * cancelするとDiscordConnectによる転送をキャンセルできる
 */
public class PreForwardChatToDiscordEvent {
    private final PlayerChatEvent event;
    private boolean cancelled;

    public PreForwardChatToDiscordEvent(@NotNull PlayerChatEvent event) {
        this.event = event;
        cancelled = false;
    }

    /**
     * チャットを送信したプレイヤーを取得する
     *
     * @return 送信者
     */
    public Player getSender() {
        return event.getPlayer();
    }

    /**
     * チャットのメッセージを取得する
     *
     * @return メッセージ
     */
    public String getMessage() {
        return event.getMessage();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
