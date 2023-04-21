package ru.violence.currenttime.core.database;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.coreapi.common.api.util.SchemaReader;
import ru.violence.coreapi.common.util.Check;
import ru.violence.coreapi.common.util.MathUtil;
import ru.violence.coreapi.common.util.PasswordGenerator;
import ru.violence.currenttime.common.util.TimeUtils;
import ru.violence.currenttime.core.CurrentTimeCorePlugin;
import ru.violence.currenttime.core.model.CapturedTime;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
@UtilityClass
public class SQLite {
    private static final String SELECT_TIME_BY_ID = "SELECT `id`, `creator_id`, `time`, `code` FROM `time` WHERE `id` = ?";
    private static final String SELECT_TIME_BY_CODE = "SELECT `id`, `creator_id`, `time`, `code` FROM `time` WHERE `code` = ?";
    private static final String INSERT_TIME = "INSERT INTO `time`(`creator_id`, `time`, `code`) VALUES (?, ?, ?)";
    private static final String DELETE_OLD = "DELETE FROM `time` WHERE `time` < ?";

    private static CurrentTimeCorePlugin plugin;
    private static Connection connection;
    private static final Object lock = new Object();
    private static long pruneTime;

    public static void init(CurrentTimeCorePlugin plugin) {
        SQLite.plugin = plugin;
        SQLite.pruneTime = plugin.getConfig().getLong("prune-time");
        try {
            Class.forName("org.sqlite.JDBC");
            connect();
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, SQLite::pruneOldData, 0, 24 * 60 * 60 * 20);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void terminate(CurrentTimeCorePlugin plugin) {
        SQLite.plugin = null;
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void connect() throws IOException, SQLException {
        Check.notNull(plugin);
        plugin.getDataFolder().mkdirs();
        synchronized (lock) {
            connection = DriverManager.getConnection("jdbc:sqlite://" + plugin.getDataFolder().getAbsolutePath() + "/data.db");
            createMissingTables();
        }
    }

    private static void createMissingTables() throws IOException, SQLException {
        synchronized (lock) {
            try (Statement st = connection.createStatement()) {
                List<String> statements;

                try (InputStream is = plugin.getResource("schema/sqlite.sql")) {
                    statements = SchemaReader.getStatements(is);
                }

                for (String statement : statements) {
                    st.addBatch(statement);
                }

                st.executeBatch();
            }
        }
    }

    private static Connection getConnection() {
        try {
            synchronized (lock) {
                if (connection.isClosed()) {
                    Check.isTrue(plugin.isEnabled(), "Plugin is disabled");
                    connect();
                }
                return connection;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static @NotNull String insertTime(int creatorId, long timeSeconds) {
        try {
            synchronized (lock) {
                String code = generateCode();

                try (PreparedStatement ps = getConnection().prepareStatement(INSERT_TIME)) {
                    ps.setInt(1, creatorId);
                    ps.setLong(2, timeSeconds);
                    ps.setString(3, code);
                    ps.executeUpdate();
                }

                return code;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static @Nullable CapturedTime getTimeById(int id) {
        try {
            synchronized (lock) {
                try (PreparedStatement ps = getConnection().prepareStatement(SELECT_TIME_BY_ID)) {
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) return null;
                    return extractTime(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static @Nullable CapturedTime getTimeByCode(@NotNull String code) {
        try {
            synchronized (lock) {
                try (PreparedStatement ps = getConnection().prepareStatement(SELECT_TIME_BY_CODE)) {
                    ps.setString(1, code);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) return null;
                    return extractTime(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SneakyThrows
    private static @NotNull CapturedTime extractTime(@NotNull ResultSet rs) {
        int id = rs.getInt(1);
        int creatorId = rs.getInt(2);
        long time = rs.getLong(3);
        String code = rs.getString(4);

        return new CapturedTime(id, creatorId, time, code);
    }

    private static void pruneOldData() {
        long lifeTime = MathUtil.clamp(TimeUtils.currentTimeSeconds() - pruneTime, 0, Long.MAX_VALUE);

        try {
            synchronized (lock) {
                try (PreparedStatement ps = getConnection().prepareStatement(DELETE_OLD)) {
                    ps.setLong(1, lifeTime);
                    ps.execute();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static @NotNull String generateCode() {
        while (true) {
            String code = new PasswordGenerator(8).useNumbers().useLowerEn().generate();
            if (getTimeByCode(code) == null) return code;
        }
    }
}
