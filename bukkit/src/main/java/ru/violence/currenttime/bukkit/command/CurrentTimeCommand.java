package ru.violence.currenttime.bukkit.command;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.violence.coreapi.bukkit.api.BukkitHelper;
import ru.violence.coreapi.common.api.CoreAPI;
import ru.violence.coreapi.common.message.LegacyPrinter;
import ru.violence.coreapi.common.user.User;
import ru.violence.coreapi.common.util.CommonUtil;
import ru.violence.currenttime.bukkit.CurrentTimePlugin;
import ru.violence.currenttime.bukkit.LangKeys;
import ru.violence.currenttime.common.network.PacketType;
import ru.violence.currenttime.common.util.TimeUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CurrentTimeCommand implements CommandExecutor {
    private static final int RATE_LIMIT_DELAY = 15 * 20;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final CurrentTimePlugin plugin;
    private final Set<UUID> rateLimit = new HashSet<>();

    public CurrentTimeCommand(CurrentTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        if (!sender.hasPermission("currenttime.use")) return true;

        Player player = (Player) sender;

        User user = BukkitHelper.getUser(player);
        if (user == null) return true;

        if (args.length != 0 && sender.hasPermission("currenttime.use.get")) {
            handleGet(player, args);
            return true;
        }

        if (!checkRateLimit(player)) {
            user.sendMessage(LangKeys.WAIT_BEFORE_NEXT_USE);
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long timeSeconds = TimeUtils.currentTimeSeconds();

            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeInt(PacketType.CREATE.getId());

            output.writeInt(user.getId());
            output.writeLong(timeSeconds);

            byte[] reply = CoreAPI.getMessenger().callMessage(
                    PacketType.CHANNEL_NAME,
                    PacketType.CORE_SERVER_KEY,
                    output.toByteArray()
            );

            if (reply == null) {
                player.sendMessage("§cError: NO_RESPOND");
                return;
            }

            ByteArrayDataInput input = ByteStreams.newDataInput(reply);
            String code = input.readUTF();

            String formattedTime = FORMATTER.format(Instant.ofEpochSecond(timeSeconds).atOffset(ZoneOffset.UTC));

            { // Title
                String renderedTitle = LegacyPrinter.print(LangKeys.RESULT_TITLE
                        .setLang(user.getLanguage())
                        .setArgs(formattedTime, code));

                if (!renderedTitle.isEmpty()) {
                    String[] split = renderedTitle.split("\n", 5);

                    String title = split.length > 0 ? split[0] : "";
                    String subTitle = split.length > 1 ? split[1] : "";
                    int fadeIn = split.length > 2 ? CommonUtil.parseInt(split[2], 20) : 20;
                    int stay = split.length > 3 ? CommonUtil.parseInt(split[3], 100) : 100;
                    int fadeOut = split.length > 4 ? CommonUtil.parseInt(split[4], 20) : 20;

                    player.sendTitle(
                            title,
                            subTitle,
                            fadeIn,
                            stay,
                            fadeOut
                    );
                }
            } // Title

            user.sendMessage(LangKeys.RESULT_MESSAGE.setArgs(formattedTime, code));
        });

        return true;
    }

    private void handleGet(@NotNull Player sender, String @NotNull [] args) {
        String code = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeInt(PacketType.GET.getId());

            output.writeUTF(code);

            byte[] reply = CoreAPI.getMessenger().callMessage(
                    PacketType.CHANNEL_NAME,
                    PacketType.CORE_SERVER_KEY,
                    output.toByteArray()
            );

            if (reply == null) {
                sender.sendMessage("§cError: NO_RESPOND");
                return;
            }

            ByteArrayDataInput input = ByteStreams.newDataInput(reply);

            if (!input.readBoolean()) {
                sender.sendMessage("§cCode not found!");
                return;
            }

            int creatorId = input.readInt();
            long timeSeconds = input.readLong();

            User creatorUser = CoreAPI.getUserManager().getUser(creatorId);
            String creatorName = creatorUser != null ? creatorUser.getName() : "@UNKNOWN@";

            sender.sendMessage("Creator: " + creatorName + " (" + creatorId + ")\n" +
                    "Time: " + FORMATTER.format(Instant.ofEpochSecond(timeSeconds).atOffset(ZoneOffset.UTC)));
        });
    }

    private boolean checkRateLimit(@NotNull Player player) {
        UUID uniqueId = player.getUniqueId();
        if (!rateLimit.add(uniqueId)) return false;

        Bukkit.getScheduler().runTaskLater(plugin, () -> rateLimit.remove(uniqueId), RATE_LIMIT_DELAY);

        return true;
    }
}
