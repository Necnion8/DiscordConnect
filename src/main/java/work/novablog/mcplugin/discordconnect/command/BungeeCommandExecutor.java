package work.novablog.mcplugin.discordconnect.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.novablog.mcplugin.discordconnect.util.ConfigManager;

import java.util.*;

public class BungeeCommandExecutor extends Command implements TabExecutor {
    private final ArrayList<BungeeSubCommandSettings> subCommands;
    private final String permission;

    /**
     * Bungeecordコマンドの解析や処理の呼び出しを行うインスタンスを生成します
     * @param name コマンド名
     * @param permission コマンドを実行するための権限
     *                   {@code null}または空文字の場合すべての人に実行権限を与えます。
     * @param aliases コマンドに結び付けられるエイリアス
     */
    public BungeeCommandExecutor(@NotNull String name, @Nullable String permission, @NotNull String... aliases) {
        super(name, permission, aliases);
        subCommands = new ArrayList<>();
        this.permission = permission;
    }

    /**
     * サブコマンドを追加します
     * <p>
     *     サブコマンドは "/(alias) {@link BungeeSubCommandSettings#alias}" コマンドで実行されます。
     * </p>
     * @param subCommand サブコマンドの設定
     */
    public void addSubCommand(@NotNull BungeeCommandExecutor.BungeeSubCommandSettings subCommand) {
        subCommands.add(subCommand);
    }

    /**
     * コマンド実行時に呼び出されます
     * @param commandSender コマンド送信者
     * @param args 引数
     */
    @Override
    public void execute(CommandSender commandSender, String[] args) {
        //権限の確認
        if(!commandSender.hasPermission(permission)) {
            commandSender.sendMessage(new TextComponent(ConfigManager.Message.bungeeCommandDenied.toString()));
            return;
        }
        //引数の確認
        if(args.length == 0) {
            //デフォルトコマンドの実行
            subCommands.stream().filter(subCommand -> subCommand.isDefault).forEach(subCommand -> subCommand.action.execute(commandSender, new String[1]));
            return;
        }

        //サブコマンドを選択
        Optional<BungeeSubCommandSettings> targetSubCommand = subCommands.stream()
                .filter(subCommand -> subCommand.alias.equals(args[0])).findFirst();
        if(!targetSubCommand.isPresent()) {
            //エイリアスが一致するサブコマンドがない場合エラー
            commandSender.sendMessage(new TextComponent(ConfigManager.Message.bungeeCommandNotFound.toString()));
            return;
        }

        //権限の確認
        if (targetSubCommand.get().subPermission != null && !commandSender.hasPermission(targetSubCommand.get().subPermission)) {
            commandSender.sendMessage(new TextComponent(ConfigManager.Message.bungeeCommandDenied.toString()));
            return;
        }

        String[] commandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

        //引数の確認
        if(commandArgs.length < targetSubCommand.get().requireArgs) {
            commandSender.sendMessage(new TextComponent(ConfigManager.Message.bungeeCommandSyntaxError.toString()));
            return;
        }

        targetSubCommand.get().action.execute(commandSender, commandArgs);
    }

    /**
     * タブ補完時に呼び出されます
     * @param commandSender 送信者
     * @param args 引数
     * @return 補完リスト
     */
    @Override
    public Iterable<String> onTabComplete(CommandSender commandSender, String[] args) {
        //引数がなかったら
        if (args.length == 0) {
            return Collections.emptyList();
        }

        ArrayList<String> match = new ArrayList<>();
        args[0] = args[0].toLowerCase();
        if(args.length == 1) {
            subCommands.stream().filter(subCommand -> subCommand.alias.startsWith(args[0]) && commandSender.hasPermission(subCommand.subPermission))
                    .forEach(subCommand -> match.add(subCommand.alias));
        }

        return match;
    }

    public class BungeeSubCommandSettings {
        private final String alias;
        private final String subPermission;
        private final BungeeCommandAction action;
        private boolean isDefault;
        private int requireArgs;

        /**
         * サブコマンドの設定等を保持するインスタンスを生成します
         * @param alias サブコマンドのエイリアス
         * @param subPermission コマンドを実行するための権限
         *                      {@code null}または空文字の場合
         *                      {@link BungeeCommandExecutor#permission}権限を持っている
         *                      すべての人に実行権限を与えます
         *                      {@link BungeeCommandExecutor#permission}が{@code null}または空文字の場合
         *                      subPermission引数が何であれすべての人に実行権限を与えます
         * @param action 実行する処理
         */
        public BungeeSubCommandSettings(@NotNull String alias, @Nullable String subPermission, @NotNull BungeeCommandAction action) {
            this.alias = alias;
            this.subPermission = StringUtils.isEmpty(subPermission) || StringUtils.isEmpty(permission) ? null : permission + "." + subPermission;
            this.action = action;
            isDefault = false;
            requireArgs = 0;
        }

        /**
         * デフォルトのコマンドであるか設定します
         * <p>
         *     サブコマンドのエイリアスを指定せずにコマンドを実行した際に、デフォルトコマンドの処理が実行されます。
         * </p>
         * @param isDefault trueでデフォルトにする
         */
        public BungeeSubCommandSettings setDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        /**
         * 必要な引数の数を設定します
         * <p>
         *     コマンド実行時、引数の数が足りていなかったらエラーメッセージが出ます。
         * </p>
         * @param cnt 引数の数
         */
        public BungeeSubCommandSettings requireArgs(int cnt) {
            this.requireArgs = cnt;
            return this;
        }
    }
}
