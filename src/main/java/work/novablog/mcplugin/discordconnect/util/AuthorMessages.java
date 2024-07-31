package work.novablog.mcplugin.discordconnect.util;

import club.minnced.discord.webhook.receive.ReadonlyMessage;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * DiscordConnectによって送信されたDiscordメッセージの送信者を特定するマッピング
 */
public class AuthorMessages {
    private final Map<Long, Sender> messagesSender = new ConcurrentHashMap<>();

    public AuthorMessages() {
    }

    public MinecraftSender put(UUID playerId, String playerName, long discordMessageId) {
        MinecraftSender sender = MinecraftSender.of(playerId, playerName);
        messagesSender.put(discordMessageId, sender);
        return sender;
    }

    public MinecraftSender put(ProxiedPlayer player, long discordMessageId) {
        return put(player.getUniqueId(), player.getName(), discordMessageId);
    }

    public DiscordSender put(long userId, String userName, @Nullable String userNickname, long discordMessageId) {
        DiscordSender sender = DiscordSender.of(userId, userName, userNickname);
        messagesSender.put(discordMessageId, sender);
        return sender;
    }

    public Consumer<ReadonlyMessage> putOf(MessageReceivedEvent event) {
        User author = event.getAuthor();
        String nickname = Optional.ofNullable(event.getGuild().getMember(author))
                .map(Member::getNickname)
                .orElse(null);

        return readonlyMessage -> put(author.getIdLong(), author.getName(), nickname, readonlyMessage.getId());
    }

    public Sender getMessageSender(long discordMessageId) {
        return messagesSender.get(discordMessageId);
    }

    public void clear() {
        messagesSender.clear();
    }

    public static void clearCache() {
        MinecraftSender.SENDERS.clear();
        MinecraftSender.CACHED_NAMES.clear();
        DiscordSender.SENDERS.clear();
        DiscordSender.CACHED_NAMES.clear();
        DiscordSender.CACHED_NICKNAMES.clear();
    }


    public interface Sender {
        String getDisplayName();

        default String getNickName() {
            return getDisplayName();
        }
    }

    public static class MinecraftSender implements Sender {
        public static final Map<UUID, String> CACHED_NAMES = new ConcurrentHashMap<>();
        public static final Map<UUID, MinecraftSender> SENDERS = new ConcurrentHashMap<>();

        private final UUID uuid;

        public static MinecraftSender of(UUID uuid, String name) {
            CACHED_NAMES.put(uuid, name);
            return SENDERS.computeIfAbsent(uuid, MinecraftSender::new);
        }

        public MinecraftSender(UUID uuid) {
            this.uuid = uuid;
        }

        public UUID getUniqueId() {
            return uuid;
        }

        @Override
        public String getDisplayName() {
            return CACHED_NAMES.getOrDefault(uuid, uuid.toString());
        }
    }

    public static class DiscordSender implements Sender {
        public static final Map<Long, String> CACHED_NAMES = new ConcurrentHashMap<>();
        public static final Map<Long, String> CACHED_NICKNAMES = new ConcurrentHashMap<>();
        public static final Map<Long, DiscordSender> SENDERS = new ConcurrentHashMap<>();

        private final long userId;

        public static DiscordSender of(long userId, String userName, @Nullable String nickname) {
            CACHED_NAMES.put(userId, userName);
            if (nickname != null) {
                CACHED_NICKNAMES.put(userId, nickname);
            }
            return SENDERS.computeIfAbsent(userId, DiscordSender::new);
        }

        public DiscordSender(long userId) {
            this.userId = userId;
        }

        public long getUserId() {
            return userId;
        }

        @Override
        public String getDisplayName() {
            return CACHED_NAMES.getOrDefault(userId, String.valueOf(userId));
        }

        @Override
        public String getNickName() {
            return CACHED_NICKNAMES.getOrDefault(userId, getDisplayName());
        }
    }
}
