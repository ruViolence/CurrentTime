package ru.violence.currenttime.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeUtils {
    public long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }
}
