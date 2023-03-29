package xyz.reportcards.dodgeball;

import co.aikar.commands.PaperCommandManager;
import com.google.common.collect.ImmutableList;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import xyz.reportcards.dodgeball.commands.ArenaCommand;
import xyz.reportcards.dodgeball.utils.Configuration;
import xyz.reportcards.dodgeball.utils.Logging;
import xyz.reportcards.dodgeball.utils.game.GameHandler;

public final class Dodgeball extends JavaPlugin {

    public Configuration config;
    @Getter
    private MVWorldManager worldManager;
    @Getter
    private GameHandler gameHandler;

    @Override
    public void onEnable() {
        gameHandler = new GameHandler();
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");
        commandManager.registerCommand(new ArenaCommand());
        commandManager.getCommandCompletions().registerCompletion("games", c -> ImmutableList.copyOf(gameHandler.getInstances().keySet()));
        if (getServer().getPluginManager().getPlugin("Multiverse-Core") == null) {
            Logging.error("Dodgeball#onEnable", "Multiverse-Core isn't on the server, disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        worldManager = getPlugin(MultiverseCore.class).getMVWorldManager();

        getServer().getScheduler().runTaskLater(this, () -> { // This is due to an issue with worlds loading after the plugin, so this does all startup setup 1 tick after startup
            config = new Configuration(this);
            commandManager.getCommandCompletions().registerCompletion("arenas", c -> ImmutableList.copyOf(config.getArenas().keySet()));
            if(getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
                Logging.error("Dodgeball#onEnable", "PlaceholderAPI isn't on the server, placeholders will not be registered.");
            } else {
                // Add placeholderapi service
                Logging.log("Loading PlaceholderAPI utils");
            }

            for (World world : Bukkit.getWorlds()) {
                if (world.getName().startsWith("dodgeball-")) {
                    worldManager.deleteWorld(world.getName(), true, true);
                }
            }
        }, 1);
    }

    @NotNull
    public Configuration getConfiguration() {
        return config;
    }

    public static Dodgeball getInstance() {
        return getPlugin(Dodgeball.class);
    }

    @Override
    public void onDisable() {
        Logging.log("Dodgeball has been disabled.");
    }
}
