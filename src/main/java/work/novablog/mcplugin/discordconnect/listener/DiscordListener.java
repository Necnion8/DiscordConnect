package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DiscordListener extends ListenerAdapter {
    private final List<Long> chatChannelIds;
    private final String toMinecraftFormat;

    public DiscordListener(@NotNull List<Long> chatChannelIds, @NotNull String toMinecraftFormat) {
        this.chatChannelIds = chatChannelIds;
        this.toMinecraftFormat = toMinecraftFormat;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!chatChannelIds.contains(event.getChannel().getIdLong()))
            return;

        String[] parts = toMinecraftFormat
                .replace("{name}", event.getAuthor().getName())
                .replace("{channel_name}", event.getChannel().getName())
                .split("\\{message}", -1);

        ComponentBuilder messageBuilder = new ComponentBuilder();
        messageBuilder.append(TextComponent.fromLegacyText(parts[0]));

        // テキストメッセージ
        MarkComponent[] markComponents = MarkdownConverter.fromDiscordMessage(
                event.getMessage().getContentDisplay()
        );
        TextComponent[] convertedMessage = MarkdownConverter.toMinecraftMessage(markComponents);

        // 添付ファイル
        TextComponent[] attachments = event.getMessage().getAttachments().stream().map((attachment) -> {
            TextComponent url = new TextComponent(" [" + attachment.getFileName() + "]");
            url.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()));
            return url;
        }).toArray(TextComponent[]::new);

        // partsの間にテキストメッセージと添付ファイルを挿入
        for (int i = 1; i < parts.length; i++) {
            if (convertedMessage.length != 0)
                messageBuilder.append(convertedMessage, ComponentBuilder.FormatRetention.NONE);
            if (attachments.length != 0)
                messageBuilder.append(attachments, ComponentBuilder.FormatRetention.NONE);
            messageBuilder.append(TextComponent.fromLegacyText(parts[i]), ComponentBuilder.FormatRetention.NONE);
        }

        ProxyServer.getInstance().broadcast(messageBuilder.create());
    }
}
