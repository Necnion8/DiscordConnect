package work.novablog.mcplugin.discordconnect.util;

import com.gmail.necnionch.myapp.markdownconverter.Decorate;
import com.gmail.necnionch.myapp.markdownconverter.DecorateList;
import com.gmail.necnionch.myapp.markdownconverter.MarkComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class TextUtil {

    public static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacy('§');

    public static TextComponent toComponent(MarkComponent[] components) {
        if (components.length == 0)
            return Component.empty();

        TextComponent.Builder builder = Component.text();
        for (MarkComponent component : components) {
            builder.append(applyDecoratesToComponent(Component.text(component.getText()), component.getDecorates()));
        }

        return builder.build();
    }

    public static Component applyDecoratesToComponent(Component component, DecorateList decorates) {
        for (Decorate decorate : decorates) {
            switch (decorate) {
                case RESET -> component.decorations().clear();
                case BOLD -> component = component.decorate(TextDecoration.BOLD);
                case ITALIC -> component = component.decorate(TextDecoration.ITALIC);
                case OBFUSCATED -> component = component.decorate(TextDecoration.OBFUSCATED);
                case UNDERLINED -> component = component.decorate(TextDecoration.UNDERLINED);
                case STRIKETHROUGH -> component = component.decorate(TextDecoration.STRIKETHROUGH);
            }
        }
        return component;
    }

}
