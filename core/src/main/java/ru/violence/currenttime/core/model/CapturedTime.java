package ru.violence.currenttime.core.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class CapturedTime {
    private final int id;
    private final int creatorId;
    private final long timeSeconds;
    private final @NotNull String code;
}
