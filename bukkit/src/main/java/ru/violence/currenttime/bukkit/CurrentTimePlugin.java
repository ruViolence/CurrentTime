package ru.violence.currenttime.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import ru.violence.currenttime.bukkit.command.CurrentTimeCommand;

public class CurrentTimePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getCommand("currenttime").setExecutor(new CurrentTimeCommand(this));
    }
}
