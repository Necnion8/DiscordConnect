package work.novablog.mcplugin.discordconnect.listener;

import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import com.gmail.necnionch.myapp.markdownconverter.MarkdownConverter;
import com.velocitypowered.api.proxy.ProxyServer;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.util.TextUtil;

import java.util.List;

public class DiscordListener extends ListenerAdapter {

    private final @NotNull ProxyServer server;
    private final List<Long> chatChannelIds;
    private final String toMinecraftFormat;

    public DiscordListener(@NotNull ProxyServer server,  @NotNull List<Long> chatChannelIds, @NotNull String toMinecraftFormat) {
        this.server = server;
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

        TextComponent.Builder messageBuilder = TextUtil.LEGACY_SERIALIZER.deserialize(parts[0]).toBuilder();

        // テキストメッセージ
        MarkComponent[] markComponents = MarkdownConverter.fromDiscordMessage(
                event.getMessage().getContentDisplay()
        );

        TextComponent convertedMessage = TextUtil.toComponent(markComponents);

        // 添付ファイル
        TextComponent[] attachments = event.getMessage().getAttachments().stream()
                .map((attachment) -> Component.text()
                        .append(Component.text(" "))
                        .append(Component.text("[" + attachment.getFileName() + "]", NamedTextColor.BLUE, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(attachment.getUrl())))
                        .build())
                .toArray(TextComponent[]::new);

        // partsの間にテキストメッセージと添付ファイルを挿入
        for (int i = 1; i < parts.length; i++) {
            if (markComponents.length != 0)
                messageBuilder.append(convertedMessage);
            if (attachments.length != 0)
                messageBuilder.append(attachments);
            messageBuilder.append(TextUtil.LEGACY_SERIALIZER.deserialize(parts[i]));
        }

        server.sendMessage(messageBuilder);
    }
}
