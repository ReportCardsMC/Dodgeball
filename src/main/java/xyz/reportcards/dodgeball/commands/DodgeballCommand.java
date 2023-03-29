package xyz.reportcards.dodgeball.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import xyz.reportcards.dodgeball.Dodgeball;
import xyz.reportcards.dodgeball.utils.Configuration;

@Subcommand("dodgeball")
public class DodgeballCommand extends BaseCommand {

    @Subcommand("reload")
    @CommandPermission("dodgeball.reload")
    public void reload(Player player) {
        Dodgeball.getInstance().reloadConfig();
        Dodgeball.getInstance().config = new Configuration(Dodgeball.getInstance());
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Reloaded Config!"));
    }

}
