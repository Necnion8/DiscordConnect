package work.novablog.mcplugin.discordconnect.util.discord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DiscordWebhookSender {
    private final WebhookClient client;

    /**
     * DiscordのWebhookにメッセージを送信するためのインスタンスを生成します
     * @param url WebhookのURL
     * @throws IllegalArgumentException URLの形式が不正な場合
     */
    public DiscordWebhookSender(@NotNull String url) throws IllegalArgumentException {
        WebhookClientBuilder builder = new WebhookClientBuilder(url);

        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName("Discord Webhook");
            thread.setDaemon(true);
            return thread;
        });
        builder.setWait(false);
        client = builder.build();
    }

    /**
     * Webhookでプレーンメッセージを送信します
     * @param userName 送信者の名前
     * @param avatarUrl 送信者のアバターURL
     * @param message 送信するプレーンメッセージ
     * @return 送信タスク
     */
    public CompletableFuture<ReadonlyMessage> sendMessage(@Nullable String userName, @Nullable String avatarUrl, @NotNull String message) {
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setUsername(userName);
        builder.setAvatarUrl(avatarUrl);
        builder.setContent(message);
        return client.send(builder.build());
    }

    /**
     * Webhookでプレーンメッセージを送信します
     * @param userName 送信者の名前
     * @param avatarUrl 送信者のアバターURL
     * @param message 送信するプレーンメッセージ
     * @param referenceMessageId 返信先メッセージID
     * @return 送信タスク
     */
    public CompletableFuture<ReadonlyMessage> sendMessage(@Nullable String userName, @Nullable String avatarUrl, @NotNull String message, long referenceMessageId) {
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setUsername(userName);
        builder.setAvatarUrl(avatarUrl);
        builder.setContent(message);
//        builder.setReferenceMessageId(referenceMessageId);  // TODO: Discord仕様的に返信先メッセージを設定できない
        return client.send(builder.build());
    }

    /**
     * Webhookで埋め込みメッセージを送信します
     *
     * @param userName     送信者の名前
     * @param avatarUrl    送信者のアバターURL
     * @param embedMessage 送信する埋め込みメッセージ
     * @return 送信タスク
     */
    public @NotNull CompletableFuture<ReadonlyMessage> sendMessage(@Nullable String userName, @Nullable String avatarUrl, @NotNull WebhookEmbed embedMessage) {
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setUsername(userName);
        builder.setAvatarUrl(avatarUrl);
        builder.addEmbeds(embedMessage);
        return client.send(builder.build());
    }

    /**
     * Webhookのスレッドを停止します
     */
    public void shutdown() {
        client.close();
    }
}
