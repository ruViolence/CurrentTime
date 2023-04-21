package ru.violence.currenttime.common.network;

import org.jetbrains.annotations.NotNull;

public enum PacketType {
    CREATE,
    GET;

    public static final String CHANNEL_NAME = "currenttime";
    public static final String CORE_SERVER_KEY = "core";
    private static final PacketType[] VALUES = values();

    public static @NotNull PacketType fromId(int id) {
        return VALUES[id];
    }

    public int getId() {
        return ordinal();
    }
}
