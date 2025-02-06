package work.novablog.mcplugin.discordconnect.util;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Discordのテキストチャンネルにキューのメッセージを送信する
 * レート制限があるためテキストをまとめて送信する
 */
public class DiscordSender extends Thread {
    private final TextChannel channel;
    private final BlockingQueue<Object> queue;

    public DiscordSender(@NotNull TextChannel channel) {
        this.channel = channel;
        queue = new LinkedBlockingQueue<>();
    }

    /**
     * キューに送信するテキストメッセージを追加する
     * interruptedの場合は追加されない
     * メッセージは非同期で送信される
     *
     * @param text 送信するテキストメッセージ
     * @return キューに追加されたか
     */
    public boolean addQueue(@NotNull String text) {
        if (isInterrupted()) return false;
        else return queue.add(text);
    }

    /**
     * キューに送信する埋め込みメッセージを追加する
     * interruptedの場合は追加されない
     * メッセージは非同期で送信される
     *
     * @param embed 送信する埋め込みメッセージ
     * @return キューに追加されたか
     */
    public boolean addQueue(@NotNull MessageEmbed embed) {
        if (isInterrupted()) return false;
        else return queue.add(embed);
    }

    @Override
    public void run() {
        Object queued = null;

        while (true) {
            if (queued == null) {
                try {
                    queued = queue.take();
                } catch (InterruptedException e) {
                    if (queue.isEmpty()) break;
                    queued = queue.poll();
                }
            }

            //キューを読む（テキスト）
            StringBuilder messages = new StringBuilder();
            for (; queued instanceof String; queued = queue.poll()) {
                if (messages.length() + ((String) queued).length() > 1900) break;  // 2000文字制限
                messages.append((String) queued).append("\n");
            }

            if (!messages.toString().isEmpty()) {
                channel.sendMessage(messages).complete();
            }

            //キューを読む（埋め込み）
            for (; queued instanceof MessageEmbed; queued = queue.poll()) {
                channel.sendMessageEmbeds((MessageEmbed) queued).complete();
            }
        }
    }
}
