package work.novablog.mcplugin.discordconnect.command.bungee;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.PermissionSubject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class Command {

    private final String name;
    private final @Nullable String permission;
    private final String[] aliases;

    public Command(@NotNull String name, @Nullable String permission, @NotNull String... aliases) {
        this.name = name;
        this.permission = permission;
        this.aliases = aliases;
    }

    public Command(@NotNull String name) {
        this(name, null);
    }

    public String getName() {
        return name;
    }

    public @Nullable String getPermission() {
        return permission;
    }

    public String[] getAliases() {
        return aliases;
    }

    public boolean hasPermission(PermissionSubject permissible) {
        return permission == null || permissible.hasPermission(permission);
    }

    public abstract void execute(CommandSource sender, String[] args);

    public List<String> onTabComplete(CommandSource sender, String[] args) {
        return Collections.emptyList();
    }

}
