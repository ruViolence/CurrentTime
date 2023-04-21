package ru.violence.currenttime.core;

import org.bukkit.plugin.java.JavaPlugin;
import ru.violence.coreapi.common.api.CoreAPI;
import ru.violence.currenttime.common.network.PacketType;
import ru.violence.currenttime.core.database.SQLite;
import ru.violence.currenttime.core.network.ChannelListener;

public class CurrentTimeCorePlugin extends JavaPlugin {
    private ChannelListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        SQLite.init(this);

        listener = new ChannelListener();
        CoreAPI.getMessenger().registerInChannel(PacketType.CHANNEL_NAME, listener);
    }

    @Override
    public void onDisable() {
        CoreAPI.getMessenger().unregisterListener(PacketType.CHANNEL_NAME, listener);
        SQLite.terminate(this);
    }
}
