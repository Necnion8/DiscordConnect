package work.novablog.mcplugin.discordconnect;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import work.novablog.mcplugin.discordconnect.command.bungee.VelocityCommand;
import work.novablog.mcplugin.discordconnect.listener.LunaChatListener;
import work.novablog.mcplugin.discordconnect.listener.VelocityListener;
import work.novablog.mcplugin.discordconnect.util.BotManager;
import work.novablog.mcplugin.discordconnect.util.Message;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;


@Plugin(
        id = "discordconnect",
        name = "DiscordConnect",
        version = BuildConstants.VERSION,
        authors = {"nova27", "Necnion8"},
        dependencies = {
                @Dependency(id = "lunachat", optional = true),
        },
        description = "Mutual chat transmission between Discord and Minecraft."
)
public class DiscordConnect {
    private static final int CONFIG_LATEST = 5;
    private static final String PLUGIN_DOWNLOAD_LINK = "https://github.com/nova-27/DiscordConnect/releases";

    private static DiscordConnect instance;
    private final ProxyServer server;
    private final Logger log;
    private final Path dataFolder;
    private boolean enableLunaChat;
    private Properties langData;

    private BotManager botManager;
    private VelocityListener velocityListener;
    private LunaChatListener lunaChatListener;

    @Inject
    public DiscordConnect(ProxyServer server, Logger logger, @DataDirectory Path dataFolder) {
        this.server = server;
        this.log = logger;
        this.dataFolder = dataFolder;
    }

    /**
     * インスタンスを返す
     *
     * @return インスタンス
     */
    public static DiscordConnect getInstance() {
        return instance;
    }

    /**
     * 言語データを返す
     *
     * @return 言語データ
     */
    public Properties getLangData() {
        return langData;
    }

    /**
     * configを読み直してbotを再起動する
     */
    public void reload() {
        botManager.sendMessageToChatChannel(
                Message.serverActivity.toString(),
                null,
                Message.botRestarting.toString(),
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

        shutdown();
        setup();
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        instance = this;

        //LunaChatと連携
        enableLunaChat = server.getPluginManager().getPlugin("lunachat").map(PluginContainer::getInstance).isPresent();

        setup();

        //コマンドの追加
        VelocityCommand command = new VelocityCommand();
        server.getCommandManager().register(server.getCommandManager().metaBuilder(command.getName()).plugin(this).aliases(command.getAliases()).build(), command);
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        shutdown();
    }

    private void setup() {
        ConfigurationNode config;
        try {
            config = loadConfig();
        } catch (IOException e) {
            log.error("Exception", e);
            return;
        }

        String token = config.node("token").getString("");
        List<Long> chatChannelIds = parseSafeList(config.node("chatChannelIDs"), Long.class);
        String playingGameName = config.node("playingGameName").getString("");
        String toMinecraftFormat = config.node("toMinecraftFormat").getString("");
        String toDiscordFormat = config.node("toDiscordFormat").getString("");
        List<String> hiddenServers = parseSafeList(config.node("hiddenServers"), String.class);

        botManager = new BotManager(server, getLogger(), token, chatChannelIds, playingGameName, toMinecraftFormat);
        velocityListener = new VelocityListener(server, botManager, toDiscordFormat, hiddenServers);
        server.getEventManager().register(this, velocityListener);
        if (enableLunaChat) {
            lunaChatListener = new LunaChatListener(botManager, toDiscordFormat);
            server.getEventManager().register(this, lunaChatListener);
        }

//        // アップデートチェック
//        boolean updateCheck = config.getBoolean("updateCheck");
//        String currentVer = getDescription().getVersion();
//        String latestVer = GithubAPI.getLatestVersionNum();
//        if (updateCheck) {
//            if (latestVer == null) {
//                // チェックに失敗
//                getLogger().info(
//                        Message.updateCheckFailed.toString()
//                );
//            } else if (currentVer.equals(latestVer)) {
//                // すでに最新
//                getLogger().info(
//                        Message.pluginIsLatest.toString()
//                                .replace("{current}", currentVer)
//                );
//            } else {
//                // 新しいバージョンがある
//                getLogger().info(
//                        Message.updateNotice.toString()
//                                .replace("{current}", currentVer)
//                                .replace("{latest}", latestVer)
//                );
//                getLogger().info(
//                        Message.updateDownloadLink.toString()
//                                .replace("{link}", PLUGIN_DOWNLOAD_LINK)
//                );
//            }
//        }
    }

    private void shutdown() {
        if (velocityListener != null) server.getEventManager().unregisterListener(this, velocityListener);
        if (lunaChatListener != null) server.getEventManager().unregisterListener(this, lunaChatListener);
        botManager.botShutdown();
        botManager = null;
    }

    private ConfigurationNode loadConfig() throws IOException {
        //設定フォルダ
        Files.createDirectories(getDataFolder());

        //言語ファイル
        Path langFile = getDataFolder().resolve("message.yml");
        if (!Files.exists(langFile)) {
            //存在しなければコピー
            InputStream src = getResourceAsStream(Locale.getDefault().toString() + ".properties");
            if (src == null) src = getClass().getResourceAsStream("ja_JP.properties");
            Files.copy(src, langFile);
        }

        langData = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                Files.newInputStream(langFile),
                StandardCharsets.UTF_8)
        ) {
            langData.load(reader);
        }

        //configファイル
        Path configFile = getDataFolder().resolve("config.yml");
        if (!Files.exists(configFile)) {
            //存在しなければコピー
            InputStream src = getResourceAsStream("config.yml");
            Files.copy(src, configFile);
        }

        ConfigurationNode config = YamlConfigurationLoader.builder().path(configFile).build().load();

        //configが古ければ新しいconfigをコピー
        int configVersion = config.node("configVersion").getInt(0);
        if (configVersion < CONFIG_LATEST) {
            //古いlangをバックアップ
            Path oldLangFile = getDataFolder().resolve("message_old.yml");
            Files.move(langFile, oldLangFile, StandardCopyOption.REPLACE_EXISTING);

            //新しいlangファイルをコピー
            langFile = getDataFolder().resolve("message.yml");
            InputStream src = getResourceAsStream(Locale.getDefault().toString() + ".properties");
            if (src == null) src = getResourceAsStream("ja_JP.properties");
            Files.copy(src, langFile);
            try (InputStreamReader reader = new InputStreamReader(
                    Files.newInputStream(langFile),
                    StandardCharsets.UTF_8)
            ) {
                langData.load(reader);
            }

            //古いconfigをバックアップ
            Path oldConfigFile = getDataFolder().resolve("config_old.yml");
            Files.move(configFile, oldConfigFile, StandardCopyOption.REPLACE_EXISTING);

            //新しいconfigをコピー
            src = getResourceAsStream("config.yml");
            Files.copy(src, configFile);
            config = YamlConfigurationLoader.builder().path(configFile).build().load();

            getLogger().info(Message.configIsOld.toString());
        }

        return config;
    }


    private Logger getLogger() {
        return log;
    }

    private InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    private Path getDataFolder() {
        return dataFolder;
    }

    private <V> List<V> parseSafeList(ConfigurationNode node, Class<V> type) {
        try {
            return node.getList(type);
        } catch (SerializationException e) {
            return Collections.emptyList();
        }
    }
}
