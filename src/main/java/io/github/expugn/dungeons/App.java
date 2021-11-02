package io.github.expugn.dungeons;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * PartyDungeons - A Minecraft Spigot Plugin.
 * PartyDungeons utilizes the Nashorn JavaScript engine to bring builds to life while also providing
 * a framework for dungeons and manages their states to keep things in control.
 * @author S'pugn
 * @version 0.2
 */
public final class App extends JavaPlugin {
    @Override
    public void onEnable() {
        // CREATE PLUGIN DATA FOLDER
        if (!getDataFolder().exists()) {
            getLogger().info("Creating necessary directories!");
            getDataFolder().mkdirs();
        }

        // CHECK IF SCRIPT ENGINE EXISTS
        try {
            new NashornScriptEngineFactory().getScriptEngine("--language=es6");
        } catch (NoClassDefFoundError e) {
            getLogger().severe(String.format("%s NOT FOUND! SHUTTING PLUGIN DOWN.", AppConstants.SCRIPT_ENGINE_NAME));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // SET UP APP STATUS
        AppStatus.init(this);

        this.getCommand("spugn").setExecutor(new SpugnCommand());
        this.getCommand("partydungeons").setExecutor(new AppCommand());
        this.getCommand("partydungeons").setTabCompleter(new AppTabCompleter());
        this.getServer().getPluginManager().registerEvents(new AppEventListener(), this);

        // SETUP OPTIONAL DEPENDENCIES
        setupEconomy();
    }

    @Override
    public void onDisable() {
        // SHUT DOWN EXECUTOR SERVICE IN CASE THERE ARE CURRENTLY ANY IN PROGRESS SCRIPTS
        getLogger().info(String.format("%s %s", "FORCE SHUTTING DOWN ScriptExecutorService!!!",
            "There may be exceptions below if there were scripts in progress..."));
        AppStatus.getExecutorService().shutdownNow();
    }

    /**
     * Setup the economy (Vault).
     * @return true if economy was successfully setup, false otherwise.
     */
    public boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            // VAULT NOT FOUND
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            return false;
        }

        AppStatus.setEconomy(rsp.getProvider());
        return AppStatus.getEconomy() != null;
    }
}
