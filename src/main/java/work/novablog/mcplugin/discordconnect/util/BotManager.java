package work.novablog.mcplugin.discordconnect.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import work.novablog.mcplugin.discordconnect.DiscordConnect;
import work.novablog.mcplugin.discordconnect.listener.ChatCasterListener;
import work.novablog.mcplugin.discordconnect.listener.DiscordListener;
import work.novablog.mcplugin.discordconnect.listener.LunaChatListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DiscordBotの管理を行う
 */
public class BotManager implements EventListener {
    private final Logger logger;
    private JDA bot;
    private List<Long> chatChannelIds;
    private List<DiscordSender> chatChannelSenders;
    private String playingGameName;

    private boolean isActive;

    public BotManager(Logger logger, String token, List<Long> chatChannelIds, String playingGameName, String prefix, String toMinecraftFormat) {
        this.logger = logger;

        //ログインする
        try {
            bot = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(this, new DiscordListener(prefix, toMinecraftFormat))
                    .build();
            isActive = true;
        } catch (InvalidTokenException e) {
            this.logger.severe(Message.invalidToken.toString());
            bot = null;
            isActive = false;
            return;
        }

        this.chatChannelIds = chatChannelIds;
        this.chatChannelSenders = new ArrayList<>();
        this.playingGameName = playingGameName;
    }

    /**
     * botをシャットダウンする
     */
    public void botShutdown() {
        if (!isActive) return;

        DiscordConnect.getInstance().getProxy().getPluginManager().unregisterListener(DiscordConnect.getInstance().getBungeeListener());
        ChatCasterListener chatCasterListener = DiscordConnect.getInstance().getChatCasterListener();
        LunaChatListener lunaChatListener = DiscordConnect.getInstance().getLunaChatListener();
        if (chatCasterListener != null)
            DiscordConnect.getInstance().getProxy().getPluginManager().unregisterListener(chatCasterListener);
        if (lunaChatListener != null)
            DiscordConnect.getInstance().getProxy().getPluginManager().unregisterListener(lunaChatListener);
        logger.info(Message.normalShutdown.toString());

        //プロキシ停止メッセージ
        sendMessageToChatChannel(
                Message.serverActivity.toString(),
                null,
                Message.proxyStopped.toString(),
                new Color(102, 205, 170),
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        //送信完了まで待機
        chatChannelSenders.forEach(DiscordSender::interrupt);
        chatChannelSenders.forEach(sender -> {
            try {
                sender.join();
            } catch (InterruptedException e) {
                logger.warning(e.getMessage());
            }
        });

        //botのシャットダウン
        bot.shutdown();

        bot = null;
        chatChannelSenders = null;
        chatChannelIds = null;
        isActive = false;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof ReadyEvent) {
            //Botのログインが完了

            //チャットチャンネルを登録
            for (long id : chatChannelIds) {
                TextChannel channel = bot.getTextChannelById(id);

                if (channel == null) {
                    logger.warning(Message.channelNotFound.toString().replace("{id}", String.valueOf(id)));
                    continue;
                }

                DiscordSender sender = new DiscordSender(channel);
                sender.start();
                chatChannelSenders.add(sender);
            }

            DiscordConnect.getInstance().getProxy().getPluginManager().registerListener(DiscordConnect.getInstance(), DiscordConnect.getInstance().getBungeeListener());
            ChatCasterListener chatCasterListener = DiscordConnect.getInstance().getChatCasterListener();
            LunaChatListener lunaChatListener = DiscordConnect.getInstance().getLunaChatListener();
            if (chatCasterListener != null)
                DiscordConnect.getInstance().getProxy().getPluginManager().registerListener(DiscordConnect.getInstance(), chatCasterListener);
            if (lunaChatListener != null)
                DiscordConnect.getInstance().getProxy().getPluginManager().registerListener(DiscordConnect.getInstance(), lunaChatListener);
            updateGameName(
                    DiscordConnect.getInstance().getProxy().getPlayers().size(),
                    DiscordConnect.getInstance().getProxy().getConfig().getPlayerLimit()
            );

            logger.info(Message.botIsReady.toString());

            sendMessageToChatChannel(
                    Message.serverActivity.toString(),
                    null,
                    Message.proxyStarted.toString(),
                    new Color(102, 205, 170),
                    new ArrayList<>(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    /**
     * チャットチャンネルへメッセージを送信
     *
     * @param mes メッセージ
     */
    public void sendMessageToChatChannel(String mes) {
        chatChannelSenders.forEach(sender -> sender.addQueue(mes));
    }

    /**
     * チャットチャンネルへ埋め込みメッセージを送信
     *
     * @param title       タイトル
     * @param titleUrl    タイトルのリンクURL
     * @param desc        説明
     * @param color       色
     * @param embedFields フィールド
     * @param author      送信者の名前
     * @param authorUrl   送信者のリンクURL
     * @param authorIcon  送信者のアイコン
     * @param footer      フッター
     * @param footerIcon  フッターのアイコン
     * @param image       画像
     * @param thumbnail   サムネイル
     */
    public void sendMessageToChatChannel(String title, String titleUrl, String desc, Color color, @NotNull List<MessageEmbed.Field> embedFields, String author, String authorUrl, String authorIcon, String footer, String footerIcon, String image, String thumbnail) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(title, titleUrl);
        eb.setColor(color);
        eb.setDescription(desc);
        embedFields.forEach(eb::addField);
        eb.setAuthor(author, authorUrl, authorIcon);
        eb.setFooter(footer, footerIcon);
        eb.setImage(image);
        eb.setThumbnail(thumbnail);

        chatChannelSenders.forEach(sender -> sender.addQueue(eb.build()));
    }

    /**
     * チャットチャンネルのIDリストを取得
     *
     * @return IDリスト
     */
    public List<Long> getChatChannelIds() {
        return chatChannelIds;
    }

    /**
     * プレイ中のゲーム名を更新
     *
     * @param playerCount プレイヤー数
     * @param maxPlayers  最大プレイヤー数
     */
    public void updateGameName(int playerCount, int maxPlayers) {
        String maxPlayersString = maxPlayers != -1 ? String.valueOf(maxPlayers) : "∞";

        bot.getPresence().setActivity(
                Activity.playing(playingGameName
                        .replace("{players}", String.valueOf(playerCount))
                        .replace("{max}", maxPlayersString)));
    }
}