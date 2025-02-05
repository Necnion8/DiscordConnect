package work.novablog.mcplugin.discordconnect.util;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Discordのテキストチャンネルにキューのメッセージを送信する
 * レート制限があるためテキストをまとめて送信する
 * interruptされるとキューを捨てて終了する
 */
public class DiscordSender extends Thread {
    private final TextChannel channel;
    private final BlockingQueue<Object> queue;

    public DiscordSender(TextChannel channel) {
        this.channel = channel;
        queue = new LinkedBlockingQueue<>();
    }

    /**
     * キューに送信するテキストメッセージを追加する
     * メッセージは非同期で送信される
     *
     * @param text 送信するテキストメッセージ
     */
    public void addQueue(String text) {
        queue.add(text);
    }

    /**
     * キューに送信する埋め込みメッセージを追加する
     * メッセージは非同期で送信される
     *
     * @param embed 送信する埋め込みメッセージ
     */
    public void addQueue(MessageEmbed embed) {
        queue.add(embed);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        try {
            Object queued = queue.take();

            while (true) {
                //キューを読む（テキスト）
                StringBuilder messages = new StringBuilder();
                for (; queued instanceof String; queued = queue.poll()) {
                    //2000文字制限
                    if (messages.length() + ((String) queued).length() > 1900) break;

                    messages.append((String) queued).append("\n");
                }

                if (!messages.toString().isEmpty()) {
                    channel.sendMessage(messages).complete();
                }

                if (queued == null) {
                    queued = queue.take();
                }

                //キューを読む（埋め込み）
                for (; queued instanceof MessageEmbed; queued = queue.take()) {
                    channel.sendMessageEmbeds((MessageEmbed) queued).complete();
                }
            }
        } catch (InterruptedException ignored) {
        }
    }
}
