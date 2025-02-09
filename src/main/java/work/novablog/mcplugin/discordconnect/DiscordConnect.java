package work.novablog.mcplugin.discordconnect;

import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.LunaChatBungee;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterAPI;
import com.gmail.necnionch.myplugin.n8chatcaster.bungee.N8ChatCasterPlugin;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bstats.bungeecord.Metrics;
import work.novablog.mcplugin.discordconnect.command.bungee.BungeeCommand;
import work.novablog.mcplugin.discordconnect.listener.BungeeListener;
import work.novablog.mcplugin.discordconnect.listener.ChatCasterListener;
import work.novablog.mcplugin.discordconnect.listener.LunaChatListener;
import work.novablog.mcplugin.discordconnect.util.BotManager;
import work.novablog.mcplugin.discordconnect.util.GithubAPI;
import work.novablog.mcplugin.discordconnect.util.Message;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

public final class DiscordConnect extends Plugin {
    private static final int CONFIG_LATEST = 4;
    private static final String pluginDownloadLink = "https://github.com/nova-27/DiscordConnect/releases";

    private static DiscordConnect instance;
    private BotManager botManager;
    private Properties langData;
    private BungeeListener bungeeListener;

    private N8ChatCasterAPI chatCasterAPI;
    private ChatCasterListener chatCasterListener;

    private LunaChatAPI lunaChatAPI;
    private LunaChatListener lunaChatListener;

    /**
     * インスタンスを返す
     *
     * @return インスタンス
     */
    public static DiscordConnect getInstance() {
        return instance;
    }

    /**
     * Botマネージャーを返す
     *
     * @return botマネージャー
     */
    public BotManager getBotManager() {
        return botManager;
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
     * ChatCasterAPIを返す
     *
     * @return chatCasterAPI
     */
    public @Nullable N8ChatCasterAPI getChatCasterAPI() {
        return chatCasterAPI;
    }

    /**
     * ChatCasterListenerを返す
     *
     * @return chatCasterListener
     */
    public ChatCasterListener getChatCasterListener() {
        return chatCasterListener;
    }

    @Override
    public void onEnable() {
        instance = this;

        //bstats
        new Metrics(this, 7990);

        //N8ChatCasterと連携
        Plugin temp = getProxy().getPluginManager().getPlugin("N8ChatCaster");
        if (temp instanceof N8ChatCasterPlugin) {
            chatCasterAPI = (((N8ChatCasterPlugin) temp).getChatCasterApi());
            chatCasterListener = new ChatCasterListener();
        }

        //LunaChatと連携
        temp = getProxy().getPluginManager().getPlugin("LunaChat");
        if (temp instanceof LunaChatBungee) {
            lunaChatAPI = ((LunaChatBungee) temp).getLunaChatAPI();
        }

        //configの読み込み
        loadConfig();

        //コマンドの追加
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand());
    }

    public void loadConfig() {
        if (botManager != null) {
            if (bungeeListener != null) getProxy().getPluginManager().unregisterListener(bungeeListener);
            if (lunaChatListener != null) getProxy().getPluginManager().unregisterListener(lunaChatListener);

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
            botManager.botShutdown();
            botManager = null;
        }

        //設定フォルダ
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        //言語ファイル
        File languageFile = new File(getDataFolder(), "message.yml");
        if (!languageFile.exists()) {
            //存在しなければコピー
            InputStream src = getResourceAsStream(Locale.getDefault().toString() + ".properties");
            if (src == null) src = getResourceAsStream("ja_JP.properties");

            try {
                Files.copy(src, languageFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Messageの準備
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(languageFile), StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(Objects.requireNonNull(inputStreamReader));
            langData = new Properties();
            langData.load(bufferedReader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //configファイル
        File pluginConfig = new File(getDataFolder(), "config.yml");
        if (!pluginConfig.exists()) {
            //存在しなければコピー
            InputStream src = getResourceAsStream("config.yml");

            try {
                Files.copy(src, pluginConfig.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //config取得・bot起動
        Configuration pluginConfiguration = null;
        try {
            pluginConfiguration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(pluginConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int configVersion = pluginConfiguration.getInt("configVersion", 0);
        //configが古ければ新しいconfigをコピー
        if (configVersion < CONFIG_LATEST) {
            try {
                //古いconfigをリネーム
                File old_config = new File(getDataFolder(), "config_old.yml");
                Files.deleteIfExists(old_config.toPath());
                pluginConfig.renameTo(old_config);

                //新しいconfigをコピー
                pluginConfig = new File(getDataFolder(), "config.yml");
                InputStream src = getResourceAsStream("config.yml");
                Files.copy(src, pluginConfig.toPath());
                pluginConfiguration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(pluginConfig);

                //古いlangファイルをリネーム
                File old_lang = new File(getDataFolder(), "message_old.yml");
                Files.deleteIfExists(old_lang.toPath());
                languageFile.renameTo(old_lang);

                //新しいlangファイルをコピー
                languageFile = new File(getDataFolder(), "message.yml");
                src = getResourceAsStream(Locale.getDefault().toString() + ".properties");
                if (src == null) src = getResourceAsStream("ja_JP.properties");
                Files.copy(src, languageFile.toPath());
                InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(languageFile), StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(Objects.requireNonNull(inputStreamReader));
                langData = new Properties();
                langData.load(bufferedReader);

                DiscordConnect.getInstance().getLogger().info(Message.configIsOld.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String token = pluginConfiguration.getString("token");
        List<Long> chatChannelIds = pluginConfiguration.getLongList("chatChannelIDs");
        String playingGameName = pluginConfiguration.getString("playingGameName");
        String prefix = pluginConfiguration.getString("prefix");
        String toMinecraftFormat = pluginConfiguration.getString("toMinecraftFormat");
        String toDiscordFormat = pluginConfiguration.getString("toDiscordFormat");
        List<String> hiddenServers = pluginConfiguration.getStringList("hiddenServers");
        botManager = new BotManager(getLogger(), token, chatChannelIds, playingGameName, prefix, toMinecraftFormat);
        bungeeListener = new BungeeListener(botManager, toDiscordFormat, hiddenServers);
        getProxy().getPluginManager().registerListener(this, bungeeListener);
        if (lunaChatAPI != null) {
            lunaChatListener = new LunaChatListener(botManager, toDiscordFormat);
            getProxy().getPluginManager().registerListener(this, lunaChatListener);
        }

        // アップデートチェック
        boolean updateCheck = pluginConfiguration.getBoolean("updateCheck");
        String currentVer = getDescription().getVersion();
        String latestVer = GithubAPI.getLatestVersionNum();
        if (updateCheck) {
            if (latestVer == null) {
                // チェックに失敗
                getLogger().info(
                        Message.updateCheckFailed.toString()
                );
            } else if (currentVer.equals(latestVer)) {
                // すでに最新
                getLogger().info(
                        Message.pluginIsLatest.toString()
                                .replace("{current}", currentVer)
                );
            } else {
                // 新しいバージョンがある
                getLogger().info(
                        Message.updateNotice.toString()
                                .replace("{current}", currentVer)
                                .replace("{latest}", latestVer)
                );
                getLogger().info(
                        Message.updateDownloadLink.toString()
                                .replace("{link}", pluginDownloadLink)
                );
            }
        }
    }

    @Override
    public void onDisable() {
        botManager.botShutdown();
    }
}
